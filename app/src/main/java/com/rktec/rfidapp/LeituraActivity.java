package com.rktec.rfidapp;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.pda.rfid.EPCModel;
import com.pda.rfid.IAsynchronousMessage;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Random;


public class LeituraActivity extends AppCompatActivity implements IAsynchronousMessage {

    // ====== LISTAS (FONTE + VISÍVEL) ======
    private final List<ItemLeituraSessao> itensSessao = new ArrayList<>();
    private final List<ItemLeituraSessao> itensVisiveis = new ArrayList<>();

    private enum FiltroStatus { TODAS, CONFORME, DIVERGENTE, NAO_CADASTRADO }
    private FiltroStatus filtroAtual = FiltroStatus.TODAS;

    // Botões do filtro
    private Button btnFiltroTodas, btnFiltroVerde, btnFiltroAmarelo, btnFiltroVermelho;

    private LeitorRFID leitorRFID;
    private String lojaSelecionada, usuario;
    private SetorLocalizacao setorSelecionado;
    private List<ItemPlanilha> listaPlanilha;
    private List<SetorLocalizacao> listaSetores;
    private ItemLeituraSessaoAdapter adapter;
    private final HashSet<String> epcsJaProcessados = new HashSet<>();
    private boolean lendo = false;
    private TextView tvMsgLeitura, tvContadorItens;
    private HashMap<String, ItemPlanilha> mapPlaquetasGlobal;
    private int potenciaAtual = 20;
    private Button btnFinalizar;
    private MediaPlayer mpSucesso;
    private long ultimoVolumeDown = 0;
    private static final long TEMPO_CONFIRMACAO = 2000; // 2 segundos

    // loading e flag de base pronta
    private AlertDialog loadingDialog;
    private volatile boolean basePronta = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_leitura_rfid);

        tvContadorItens = findViewById(R.id.tvContadorItens);
        tvMsgLeitura = findViewById(R.id.tvMsgLeitura);
        btnFinalizar = findViewById(R.id.btnFinalizar);

        ImageButton back = findViewById(R.id.btnBack);
        back.setOnClickListener(v -> solicitarConfirmacaoSaida());

        // ====== FILTRO: pegar botões ======
        btnFiltroTodas   = findViewById(R.id.btnFiltroTodas);
        btnFiltroVerde   = findViewById(R.id.btnFiltroVerde);
        btnFiltroAmarelo = findViewById(R.id.btnFiltroAmarelo);
        btnFiltroVermelho= findViewById(R.id.btnFiltroVermelho);

        // ====== UI: botões com TEXTO (SEM ÍCONES) ======
        configurarBotoesFiltroComIcones();

        btnFiltroTodas.setOnClickListener(v -> aplicarFiltro(FiltroStatus.TODAS));
        btnFiltroVerde.setOnClickListener(v -> aplicarFiltro(FiltroStatus.CONFORME));
        btnFiltroAmarelo.setOnClickListener(v -> aplicarFiltro(FiltroStatus.DIVERGENTE));
        btnFiltroVermelho.setOnClickListener(v -> aplicarFiltro(FiltroStatus.NAO_CADASTRADO));

        setorSelecionado = DadosGlobais.getInstance().getSetorSelecionado();
        if (setorSelecionado == null) {
            Toast.makeText(this, "Nenhum setor selecionado! Voltando...", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        listaPlanilha   = DadosGlobais.getInstance().getListaPlanilha();
        listaSetores    = DadosGlobais.getInstance().getListaSetores();
        lojaSelecionada = DadosGlobais.getInstance().getLojaSelecionada();
        usuario         = DadosGlobais.getInstance().getUsuario();

        if (usuario == null) {
            usuario = getSharedPreferences("prefs", MODE_PRIVATE)
                    .getString("usuario_nome", "Usuário");
        }

        if (listaPlanilha == null) listaPlanilha = new ArrayList<>();
        if (listaSetores == null)  listaSetores  = new ArrayList<>();

        ((TextView) findViewById(R.id.tvLojaSelecionada)).setText("Loja: " + lojaSelecionada);
        ((TextView) findViewById(R.id.tvSetorSelecionado)).setText("Setor: " + setorSelecionado.setor);

        RecyclerView recyclerView = findViewById(R.id.listaItensLidos);

        // ====== Adapter usa a lista VISÍVEL (filtrada) ======
        adapter = new ItemLeituraSessaoAdapter(itensVisiveis);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        adapter.setOnItemClickListener(position -> {
            ItemLeituraSessao itemSessao = itensVisiveis.get(position);
            abrirDialogEdicao(itemSessao, position);
        });

        // Swipe esquerda remove
        ItemTouchHelper.SimpleCallback simpleCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            @Override
            public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
                int pos = viewHolder.getAdapterPosition();

                if (pos < 0 || pos >= itensVisiveis.size()) {
                    adapter.notifyDataSetChanged();
                    return;
                }

                ItemLeituraSessao removido = itensVisiveis.get(pos);

                itensSessao.remove(removido);
                itensVisiveis.remove(pos);

                adapter.notifyItemRemoved(pos);
                atualizarContadorItens();
                Toast.makeText(LeituraActivity.this, "Item removido!", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onChildDraw(Canvas c, RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder,
                                    float dX, float dY, int actionState, boolean isCurrentlyActive) {
                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);

                if (dX < 0) {
                    View itemView = viewHolder.itemView;

                    Paint p = new Paint();
                    p.setColor(Color.parseColor("#D32F2F"));
                    c.drawRect(
                            itemView.getRight() + dX, itemView.getTop(),
                            itemView.getRight(), itemView.getBottom(), p);

                    Drawable icon = ContextCompat.getDrawable(itemView.getContext(), R.drawable.ic_delete);
                    if (icon != null) {
                        int iconMargin = (itemView.getHeight() - icon.getIntrinsicHeight()) / 2;
                        int iconTop = itemView.getTop() + iconMargin;
                        int iconBottom = iconTop + icon.getIntrinsicHeight();
                        int iconLeft = itemView.getRight() - iconMargin - icon.getIntrinsicWidth();
                        int iconRight = itemView.getRight() - iconMargin;
                        icon.setBounds(iconLeft, iconTop, iconRight, iconBottom);
                        icon.setAlpha((int) (255 * Math.min(1f, Math.abs(dX) / itemView.getWidth())));
                        icon.draw(c);
                    }
                }
            }
        };
        new ItemTouchHelper(simpleCallback).attachToRecyclerView(recyclerView);

        mpSucesso = MediaPlayer.create(this, R.raw.sucesso);

        btnFinalizar.setOnClickListener(v -> {

            // PRIORIDADE 1: LOJA ERRADA (mais grave)
            if (existeItemLojaErrada()) {
                mostrarDialogConfirmacao(
                        "ATENÇÃO",
                        Color.parseColor("#D32F2F"),
                        "Foram identificados itens de OUTRA LOJA.\n\n" +
                                "Para corrigir, será necessário trocar a loja antes de finalizar.\n\n" +
                                "Deseja verificar agora?",
                        "VERIFICAR",
                        () -> {
                            // Usuário escolheu verificar, não faz nada
                        }
                );
                return;
            }

            // PRIORIDADE 2: SETOR ERRADO
            if (existeItemSetorErrado()) {
                mostrarDialogConfirmacao(
                        "Itens divergentes",
                        Color.parseColor("#F9A825"),
                        "Há itens em setor diferente do selecionado.\n\n" +
                                "Deseja verificar/corrigir antes de finalizar?",
                        "VERIFICAR",
                        () -> {
                            // Usuário escolheu verificar
                        }
                );
                return;
            }

            // SEM DIVERGÊNCIAS → FINALIZA DIRETO
            iniciarFluxoFinalizacao();
        });


        iniciarPreparacaoBase();

        SeekBar sbPotencia = findViewById(R.id.sbPotencia);
        TextView tvPotencia = findViewById(R.id.tvPotencia);

        sbPotencia.setMax(32);
        sbPotencia.setProgress(potenciaAtual - 1);
        tvPotencia.setText(String.valueOf(potenciaAtual));

        sbPotencia.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                potenciaAtual = progress;
                tvPotencia.setText(String.valueOf(potenciaAtual));
                if (leitorRFID != null) leitorRFID.setPotencia(potenciaAtual);
            }

            @Override public void onStartTrackingTouch(SeekBar seekBar) { }
            @Override public void onStopTrackingTouch(SeekBar seekBar) { }
        });

        simularItensParaTeste();
        aplicarFiltro(FiltroStatus.TODAS);

        getOnBackPressedDispatcher().addCallback(this,
                new OnBackPressedCallback(true) {
                    @Override
                    public void handleOnBackPressed() {
                        solicitarConfirmacaoSaida();
                    }
                });

    }

    // ====== UI: BOTÕES COM TEXTO (SEM ÍCONES) ======
    private void configurarBotoesFiltroComIcones() {
        if (btnFiltroTodas == null || btnFiltroVerde == null || btnFiltroAmarelo == null || btnFiltroVermelho == null) return;

        // Textos
        btnFiltroTodas.setText("Todos");
        btnFiltroVerde.setText("OK");
        btnFiltroAmarelo.setText("Divergente");
        btnFiltroVermelho.setText("Não encontrado");

        // Remove qualquer ícone colocado via código/tema
        btnFiltroTodas.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
        btnFiltroVerde.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
        btnFiltroAmarelo.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
        btnFiltroVermelho.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);

        // Centraliza
        btnFiltroTodas.setGravity(Gravity.CENTER);
        btnFiltroVerde.setGravity(Gravity.CENTER);
        btnFiltroAmarelo.setGravity(Gravity.CENTER);
        btnFiltroVermelho.setGravity(Gravity.CENTER);

        // Padding leve (não quebra tela)
        int padV = (int) (6 * getResources().getDisplayMetrics().density);
        int padH = (int) (4 * getResources().getDisplayMetrics().density);

        btnFiltroTodas.setPadding(padH, padV, padH, padV);
        btnFiltroVerde.setPadding(padH, padV, padH, padV);
        btnFiltroAmarelo.setPadding(padH, padV, padH, padV);
        btnFiltroVermelho.setPadding(padH, padV, padH, padV);
    }

    // ====== FILTRO ======
    private void aplicarFiltro(FiltroStatus novoFiltro) {
        filtroAtual = novoFiltro;

        itensVisiveis.clear();

        for (ItemLeituraSessao item : itensSessao) {
            if (item == null) continue;

            if (filtroAtual == FiltroStatus.TODAS) {
                itensVisiveis.add(item);
            } else if (filtroAtual == FiltroStatus.CONFORME && item.status == ItemLeituraSessao.STATUS_OK) {
                itensVisiveis.add(item);

            } else if (filtroAtual == FiltroStatus.DIVERGENTE
                    && (item.status == ItemLeituraSessao.STATUS_SETOR_ERRADO
                    || item.status == ItemLeituraSessao.STATUS_LOJA_ERRADA)) {
                itensVisiveis.add(item);

            } else if (filtroAtual == FiltroStatus.NAO_CADASTRADO && item.status == ItemLeituraSessao.STATUS_NAO_ENCONTRADO) {
                itensVisiveis.add(item);
            }
        }

        adapter.notifyDataSetChanged();
        atualizarEstiloBotoesFiltro();
    }

    private void atualizarEstiloBotoesFiltro() {
        if (btnFiltroTodas != null)    btnFiltroTodas.setEnabled(filtroAtual != FiltroStatus.TODAS);
        if (btnFiltroVerde != null)    btnFiltroVerde.setEnabled(filtroAtual != FiltroStatus.CONFORME);
        if (btnFiltroAmarelo != null)  btnFiltroAmarelo.setEnabled(filtroAtual != FiltroStatus.DIVERGENTE);
        if (btnFiltroVermelho != null) btnFiltroVermelho.setEnabled(filtroAtual != FiltroStatus.NAO_CADASTRADO);
    }

    // ====== BASE ======
    private void iniciarPreparacaoBase() {
        mostrarLoading("Preparando base de leitura, aguarde...");

        new Thread(() -> {
            long inicio = System.currentTimeMillis();

            construirMapaPlaquetasGlobal(listaPlanilha);

            int total = calcularTotalItensNoSetor();
            final int totalFinal = total;

            long fim = System.currentTimeMillis();
            Log.d("LeituraActivity", "Base de leitura preparada em " + (fim - inicio) + " ms");

            runOnUiThread(() -> {
                int lidos = itensSessao.size();
                tvContadorItens.setText("Itens lidos: " + lidos + " / " + totalFinal);
                basePronta = true;
                esconderLoading();
            });
        }).start();
    }

    private void mostrarLoading(String mensagem) {
        if (loadingDialog == null) {
            View view = getLayoutInflater().inflate(R.layout.dialog_loading, null);
            TextView tvMsg = view.findViewById(R.id.tvLoadingMsg);
            tvMsg.setText(mensagem);

            loadingDialog = new AlertDialog.Builder(this, R.style.AppDialogTheme)
                    .setView(view)
                    .setCancelable(false)
                    .create();
            loadingDialog.show();
        } else {
            if (!loadingDialog.isShowing()) loadingDialog.show();
            TextView tvMsg = loadingDialog.findViewById(R.id.tvLoadingMsg);
            if (tvMsg != null) tvMsg.setText(mensagem);
        }
    }

    private void esconderLoading() {
        if (loadingDialog != null && loadingDialog.isShowing()) {
            loadingDialog.dismiss();
        }
    }

    private boolean getPreferencia(String chave, boolean padrao) {
        return getSharedPreferences("prefs", MODE_PRIVATE).getBoolean(chave, padrao);
    }

    private void construirMapaPlaquetasGlobal(List<ItemPlanilha> lista) {
        mapPlaquetasGlobal = new HashMap<>();
        if (lista == null) return;

        for (ItemPlanilha item : lista) {
            if (item != null && item.nroplaqueta != null) {
                String chave = item.nroplaqueta.trim().replaceFirst("^0+(?!$)", "");
                mapPlaquetasGlobal.put(chave, item);
            }
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == 139 && !lendo && setorSelecionado != null) {

            if (!basePronta) {
                Toast.makeText(this, "Aguarde, preparando base de leitura...", Toast.LENGTH_SHORT).show();
                return true;
            }

            if (leitorRFID == null) leitorRFID = new LeitorRFID(this, this);
            leitorRFID.setPotencia(potenciaAtual);
            lendo = leitorRFID.iniciarLeitura();
            tvMsgLeitura.setText("Leitura iniciada. Aproxime as etiquetas.");
            epcsJaProcessados.clear();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (keyCode == 139 && lendo && leitorRFID != null) {
            Log.d("LeituraActivity", "Chamando pararLeitura()");
            leitorRFID.pararLeitura();
            lendo = false;
            tvMsgLeitura.setText("Leitura pausada! Aperte o gatilho para ler novamente.");
            return true;
        }
        return super.onKeyUp(keyCode, event);
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN && event.getKeyCode() == KeyEvent.KEYCODE_VOLUME_UP) {
            long agora = System.currentTimeMillis();
            if (agora - ultimoVolumeDown < TEMPO_CONFIRMACAO) {
                btnFinalizar.performClick();
                ultimoVolumeDown = 0;
            } else {
                Toast.makeText(this, "Aperte o volume - novamente para finalizar!", Toast.LENGTH_SHORT).show();
                ultimoVolumeDown = agora;
            }
            return true;
        }
        return super.dispatchKeyEvent(event);
    }

    public static String formatarEPC(String epc) {
        if (epc == null) return "";
        String ultimos = epc.length() > 7 ? epc.substring(epc.length() - 7) : epc;
        return ultimos.replaceFirst("^0+(?!$)", "");
    }

    @Override
    public void OutPutEPC(EPCModel model) {
        final String epcFinal = model._EPC;
        String epcLimpo = formatarEPC(epcFinal);
        if (epcsJaProcessados.contains(epcLimpo)) return;
        epcsJaProcessados.add(epcLimpo);
        runOnUiThread(() -> processarEPC(epcFinal));
    }

    private void processarEPC(String epc) {
        String epcLimpo = formatarEPC(epc);

        for (ItemLeituraSessao i : itensSessao) {
            if (i.epc.equals(epcLimpo)) return;
        }

        ItemPlanilha item = encontrarItemPorEPC(epc);

        ItemLeituraSessao novo = new ItemLeituraSessao(epcLimpo, item);

        if (item == null) {
            novo.status = ItemLeituraSessao.STATUS_NAO_ENCONTRADO;
        } else {
            boolean mesmaLoja = (lojaSelecionada != null && lojaSelecionada.equals(item.loja));
            boolean mesmoSetor = (setorSelecionado != null
                    && item.codlocalizacao != null
                    && item.codlocalizacao.equals(setorSelecionado.codlocalizacao));

            if (mesmaLoja && mesmoSetor) {
                novo.status = ItemLeituraSessao.STATUS_OK;
            } else if (mesmaLoja) {
                novo.status = ItemLeituraSessao.STATUS_SETOR_ERRADO;
            } else {
                novo.status = ItemLeituraSessao.STATUS_LOJA_ERRADA;
            }
        }

        itensSessao.add(novo);

        aplicarFiltro(filtroAtual);

        atualizarContadorItens();
        atualizarFeedbackDaLeitura(novo);
    }

    private String gerarEpcAleatorio() {
        String chars = "0123456789ABCDEF";
        StringBuilder sb = new StringBuilder();
        Random random = new Random();
        for (int i = 0; i < 24; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }

    private void abrirDialogSimularLeitura() {
        final EditText input = new EditText(this);
        input.setHint("Digite o EPC ou deixe em branco para aleatório");
        input.setSingleLine(true);

        int padding = (int) (16 * getResources().getDisplayMetrics().density);
        input.setPadding(padding, padding, padding, padding);

        new AlertDialog.Builder(this)
                .setTitle("Simular leitura de EPC")
                .setView(input)
                .setPositiveButton("Simular", (dialog, which) -> {
                    String epc = input.getText().toString().trim();
                    if (epc.isEmpty()) {
                        epc = gerarEpcAleatorio();
                        Toast.makeText(this, "EPC aleatório: " + epc, Toast.LENGTH_SHORT).show();
                    }
                    processarEPC(epc);
                })
                .setNeutralButton("Aleatório", (dialog, which) -> {
                    String epcAleatorio = gerarEpcAleatorio();
                    Toast.makeText(this, "EPC aleatório: " + epcAleatorio, Toast.LENGTH_SHORT).show();
                    processarEPC(epcAleatorio);
                })
                .setNegativeButton("Cancelar", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void atualizarFeedbackDaLeitura(ItemLeituraSessao sessao) {
        if (sessao == null) return;

        String msg;
        int corFundo;

        switch (sessao.status) {
            case ItemLeituraSessao.STATUS_OK:
                if (sessao.item != null && sessao.item.descresumida != null) {
                    msg = "OK: " + sessao.item.descresumida + " está na loja e setor corretos.";
                } else {
                    msg = "OK: item na loja e setor corretos.";
                }
                corFundo = Color.parseColor("#E8F5E9");
                break;

            case ItemLeituraSessao.STATUS_SETOR_ERRADO:
                String setorBase = (sessao.item != null && sessao.item.codlocalizacao != null)
                        ? sessao.item.codlocalizacao
                        : "-";
                msg = "Atenção: item da loja correta, mas em outro setor (setor na base: " + setorBase + ").";
                corFundo = Color.parseColor("#FFF8E1");
                break;

            case ItemLeituraSessao.STATUS_LOJA_ERRADA:
                String lojaBase = (sessao.item != null && sessao.item.loja != null)
                        ? sessao.item.loja
                        : "-";
                msg = "Alerta: item pertence à loja " + lojaBase + ", não à loja " + lojaSelecionada + ".";
                corFundo = Color.parseColor("#FFF3E0");
                break;

            case ItemLeituraSessao.STATUS_NAO_ENCONTRADO:
            default:
                msg = "Item não encontrado na base: EPC " + sessao.epc;
                corFundo = Color.parseColor("#FFEBEE");
                break;
        }

        tvMsgLeitura.setText(msg);

        Drawable bg = tvMsgLeitura.getBackground();
        if (bg instanceof android.graphics.drawable.GradientDrawable) {
            android.graphics.drawable.GradientDrawable gd =
                    (android.graphics.drawable.GradientDrawable) bg.mutate();
            gd.setColor(corFundo);
        } else {
            tvMsgLeitura.setBackgroundColor(corFundo);
        }
    }

    private ItemPlanilha encontrarItemPorEPC(String epc) {
        if (mapPlaquetasGlobal == null || epc == null) return null;
        String epcLido = epc.trim().replaceFirst("^0+(?!$)", "");
        return mapPlaquetasGlobal.get(epcLido);
    }

    private int calcularTotalItensNoSetor() {
        int total = 0;

        if (listaPlanilha != null) {
            for (ItemPlanilha item : listaPlanilha) {
                if (item == null) continue;
                if (item.loja != null
                        && item.loja.equals(lojaSelecionada)
                        && item.codlocalizacao != null
                        && setorSelecionado != null
                        && item.codlocalizacao.equals(setorSelecionado.codlocalizacao)
                ) {
                    total++;
                }
            }
        }
        return total;
    }

    private void atualizarContadorItens() {
        int total = calcularTotalItensNoSetor();
        int lidos = itensSessao.size();
        tvContadorItens.setText("Itens lidos: " + lidos + " / " + total);
    }

    private void finalizarEExportar() {
        if (listaPlanilha == null) listaPlanilha = new ArrayList<>();

        List<ItemPlanilha> itensMovidos = new ArrayList<>();
        List<ItemPlanilha> itensOutrasLojas = new ArrayList<>();
        List<String> epcsNaoCadastrados = new ArrayList<>();
        List<ItemLeituraSessao> itensNaoCadastradosEditados = new ArrayList<>();

        for (ItemLeituraSessao lido : itensSessao) {
            if (lido == null) continue;

            boolean naoEstaNaPlanilha = (lido.item == null) || !listaPlanilha.contains(lido.item);
            boolean editou = lido.item != null
                    && lido.item.descresumida != null
                    && !lido.item.descresumida.isEmpty();

            if (naoEstaNaPlanilha && editou) {
                itensNaoCadastradosEditados.add(lido);
                continue;
            }

            if (lido.item == null) {
                epcsNaoCadastrados.add(lido.epc);
                continue;
            }

            String plaqLimpo = lido.item.nroplaqueta != null
                    ? lido.item.nroplaqueta.trim().replaceFirst("^0+(?!$)", "")
                    : "";

            if (plaqLimpo.equals(lido.epc)) {
                if (lido.item.loja != null && lido.item.loja.equals(lojaSelecionada)) {
                    if (setorSelecionado != null
                            && lido.item.codlocalizacao != null
                            && !lido.item.codlocalizacao.equals(setorSelecionado.codlocalizacao)) {
                        lido.item.codlocalizacao = setorSelecionado.codlocalizacao;
                    }
                    itensMovidos.add(lido.item);
                } else {
                    itensOutrasLojas.add(lido.item);
                }
            }
        }

        try {
            for (ItemLeituraSessao editado : itensNaoCadastradosEditados) {
                if (editado == null || editado.item == null) continue;

                ItemPlanilha fake = editado.item;
                StringBuilder alteracoes = new StringBuilder();
                alteracoes.append("Cadastro/edição de item não encontrado antes da finalização; ");
                alteracoes.append("Descrição final: ").append(fake.descresumida).append("; ");
                alteracoes.append("Setor final: ").append(fake.codlocalizacao).append("; ");

                LogHelper.logEdicaoItem(
                        this,
                        usuario,
                        lojaSelecionada,
                        fake.codlocalizacao,
                        null,
                        fake,
                        alteracoes.toString()
                );
            }

            LogHelper.logRelatorioPorLoja(
                    this,
                    usuario,
                    lojaSelecionada,
                    setorSelecionado != null ? setorSelecionado.setor : "",
                    itensMovidos,
                    itensOutrasLojas,
                    epcsNaoCadastrados
            );

            File arquivo = ExportadorPlanilha.exportarCSV(this, listaPlanilha);
            String caminho = (arquivo != null) ? arquivo.getAbsolutePath() : null;
            final String caminhoFinal = caminho;

            runOnUiThread(() -> {
                if (caminhoFinal == null) {
                    Toast.makeText(this, "Falha ao exportar a planilha. Tente novamente.", Toast.LENGTH_LONG).show();
                    btnFinalizar.setText("Finalizar");
                    btnFinalizar.setEnabled(true);
                    btnFinalizar.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#FFA000")));
                    btnFinalizar.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
                    return;
                }

                if (mpSucesso != null) mpSucesso.start();
                Toast.makeText(this, "Exportado para: " + caminhoFinal, Toast.LENGTH_LONG).show();

                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    btnFinalizar.setText("Concluído!");
                    btnFinalizar.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#43A047")));
                    btnFinalizar.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_check, 0, 0, 0);

                    Intent intent = new Intent(this, SetorActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    startActivity(intent);
                    finish();
                }, 1200);
            });
        } catch (Exception e) {
            e.printStackTrace();
            runOnUiThread(() -> {
                Toast.makeText(this, "Erro ao finalizar e exportar. Verifique o armazenamento.", Toast.LENGTH_LONG).show();
                btnFinalizar.setText("Finalizar");
                btnFinalizar.setEnabled(true);
                btnFinalizar.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#FFA000")));
                btnFinalizar.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
            });
        }
    }

    @Override
    protected void onDestroy() {
        if (leitorRFID != null) leitorRFID.fechar();
        if (mpSucesso != null) mpSucesso.release();
        super.onDestroy();
    }

    private void mostrarDialogConfirmacao(String titulo, int corTitulo, String mensagem,
                                          String textoBtnPositivo, Runnable acaoConfirmar) {
        View viewDialog = LayoutInflater.from(this).inflate(R.layout.dialog_confirmacao, null);

        TextView tvTitulo = viewDialog.findViewById(R.id.tvConfirmTitle);
        TextView tvMsg = viewDialog.findViewById(R.id.tvConfirmMsg);
        Button btnPositivo = viewDialog.findViewById(R.id.btnConfirm);
        Button btnNegativo = viewDialog.findViewById(R.id.btnCancel);

        tvTitulo.setText(titulo);
        tvTitulo.setTextColor(corTitulo);
        tvMsg.setText(mensagem);
        btnPositivo.setText(textoBtnPositivo);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(viewDialog)
                .setCancelable(false)
                .create();

        final AlertDialog dialogFinal = dialog;
        btnPositivo.setOnClickListener(v -> {
            acaoConfirmar.run();
            dialogFinal.dismiss();
        });

        btnNegativo.setOnClickListener(v -> dialogFinal.dismiss());

        dialog.show();
    }

    // ====== SEU TRECHO FINAL (IGUAL) ======
    private void abrirDialogEdicao(ItemLeituraSessao sessao, int posVisivel) {
        // (mantive exatamente como você mandou)
        final ItemLeituraSessao sessaoFinal = sessao;
        final int posVisivelFinal = posVisivel;

        LayoutInflater inflater = LayoutInflater.from(this);
        View view = inflater.inflate(R.layout.dialog_editar_item_lido, null);

        EditText edtDesc = view.findViewById(R.id.edtDescResumidaDialog);
        TextView tvLoja = view.findViewById(R.id.tvLojaDialog);
        Spinner spinnerSetor = view.findViewById(R.id.spinnerSetorDialog);
        TextView tvPlaqueta = view.findViewById(R.id.tvPlaquetaDialog);
        Button btnRemover = view.findViewById(R.id.btnRemoverDialog);
        Button btnSalvar = view.findViewById(R.id.btnSalvarDialog);
        Button btnCancelar = view.findViewById(R.id.btnCancelarDialog);

        final EditText edtDescFinal = edtDesc;
        final Spinner spinnerSetorFinal = spinnerSetor;

        String lojaAtual = (sessaoFinal.item != null && sessaoFinal.item.loja != null && !sessaoFinal.item.loja.trim().isEmpty())
                ? sessaoFinal.item.loja.trim()
                : (lojaSelecionada != null ? lojaSelecionada : "");
        final String lojaAtualFinal = lojaAtual;

        tvLoja.setText("Loja: " + lojaAtualFinal);

        if (sessaoFinal.item != null) {
            String textoDesc;
            if (sessaoFinal.item.descdetalhada != null && !sessaoFinal.item.descdetalhada.trim().isEmpty()) {
                textoDesc = sessaoFinal.item.descdetalhada.trim();
            } else if (sessaoFinal.item.descresumida != null && !sessaoFinal.item.descresumida.trim().isEmpty()) {
                textoDesc = sessaoFinal.item.descresumida.trim();
            } else {
                textoDesc = "";
            }
            edtDescFinal.setText(textoDesc);
        } else {
            edtDescFinal.setText("");
        }

        tvPlaqueta.setText("Plaqueta: " + sessaoFinal.epc);

        List<SetorLocalizacao> setoresFiltrados = filtrarPorLoja(listaSetores, lojaSelecionada);

        List<String> nomesSetores = new ArrayList<>();
        for (SetorLocalizacao s : setoresFiltrados) {
            if (s != null && s.setor != null) {
                nomesSetores.add(s.setor);
            }
        }

        ArrayAdapter<String> setorAdapter = new ArrayAdapter<>(
                LeituraActivity.this,
                android.R.layout.simple_spinner_item,
                nomesSetores
        );
        setorAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerSetorFinal.setAdapter(setorAdapter);

        String setorAtualNome;
        if (sessaoFinal.item != null && sessaoFinal.item.codlocalizacao != null) {
            setorAtualNome = buscarNomeSetorPorCodigo(sessaoFinal.item.codlocalizacao);
        } else if (setorSelecionado != null) {
            setorAtualNome = setorSelecionado.setor;
        } else {
            setorAtualNome = null;
        }

        if (setorAtualNome != null) {
            int idx = nomesSetores.indexOf(setorAtualNome);
            if (idx >= 0) spinnerSetorFinal.setSelection(idx);
        }

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(view)
                .setCancelable(false)
                .create();
        final AlertDialog dialogFinal = dialog;

        btnSalvar.setOnClickListener(v -> {
            mostrarDialogConfirmacao(
                    "Confirmar alteração",
                    Color.parseColor("#1976D2"),
                    "Tem certeza que deseja salvar as alterações deste item?",
                    "Salvar",
                    () -> {
                        String novaDescDet = edtDescFinal.getText().toString();
                        String novoSetorNome = (String) spinnerSetorFinal.getSelectedItem();
                        String novoSetorCodigo = buscarCodigoSetorPorNome(novoSetorNome);

                        ItemPlanilha itemAntigo = null;
                        if (sessaoFinal.item != null) {
                            itemAntigo = new ItemPlanilha(
                                    sessaoFinal.item.loja, sessaoFinal.item.sqbem, sessaoFinal.item.codgrupo, sessaoFinal.item.codlocalizacao, sessaoFinal.item.nrobem,
                                    sessaoFinal.item.nroincorp, sessaoFinal.item.descresumida, sessaoFinal.item.descdetalhada, sessaoFinal.item.qtdbem,
                                    sessaoFinal.item.nroplaqueta, sessaoFinal.item.nroseriebem, sessaoFinal.item.modelobem
                            );
                        }

                        if (sessaoFinal.item == null) {
                            ItemPlanilha itemNovoFake = new ItemPlanilha(
                                    lojaAtualFinal, "", "", novoSetorCodigo, "", "",
                                    novaDescDet,
                                    novaDescDet,
                                    "",
                                    sessaoFinal.epc, "", ""
                            );
                            sessaoFinal.item = itemNovoFake;
                            sessaoFinal.encontrado = true;

                            aplicarFiltro(filtroAtual);
                            atualizarContadorItens();
                            dialogFinal.dismiss();
                            return;
                        } else {
                            sessaoFinal.item.descdetalhada = novaDescDet;
                            if (sessaoFinal.item.descresumida == null || sessaoFinal.item.descresumida.trim().isEmpty()) {
                                sessaoFinal.item.descresumida = novaDescDet;
                            }
                            sessaoFinal.item.loja = lojaAtualFinal;
                            sessaoFinal.item.codlocalizacao = novoSetorCodigo;
                        }

                        if (sessaoFinal.item != null && sessaoFinal.item.nroplaqueta != null) {
                            BancoHelper bancoHelper = new BancoHelper(getApplicationContext());
                            bancoHelper.atualizarDescricaoESetor(sessaoFinal.item.nroplaqueta, novaDescDet, novoSetorCodigo);
                        }

                        StringBuilder alteracoes = new StringBuilder();
                        if (itemAntigo != null) {
                            String antigaDet = itemAntigo.descdetalhada != null ? itemAntigo.descdetalhada : "";
                            String novaDet = novaDescDet != null ? novaDescDet : "";

                            if (!antigaDet.equals(novaDet)) {
                                alteracoes.append("Descrição detalhada: ")
                                        .append(antigaDet)
                                        .append(" -> ")
                                        .append(novaDet)
                                        .append("; ");
                            }

                            if (!itemAntigo.codlocalizacao.equals(novoSetorCodigo)) {
                                alteracoes.append("Setor: ")
                                        .append(itemAntigo.codlocalizacao)
                                        .append(" -> ")
                                        .append(novoSetorCodigo)
                                        .append("; ");
                            }
                        }

                        if (alteracoes.length() > 0) {
                            LogHelper.logEdicaoItem(
                                    getApplicationContext(),
                                    usuario,
                                    lojaAtualFinal,
                                    novoSetorCodigo,
                                    itemAntigo,
                                    sessaoFinal.item,
                                    alteracoes.toString()
                            );
                        }

                        aplicarFiltro(filtroAtual);
                        atualizarContadorItens();
                        dialogFinal.dismiss();
                    }
            );
        });

        btnRemover.setOnClickListener(v -> {
            mostrarDialogConfirmacao(
                    "Remover item",
                    Color.parseColor("#D32F2F"),
                    "Tem certeza que deseja salvar as alterações e remover esse item da lista?\n\nEsta ação não pode ser desfeita.",
                    "Remover",
                    () -> {
                        String novaDesc = edtDescFinal.getText().toString();
                        String novoSetorNome = (String) spinnerSetorFinal.getSelectedItem();
                        String novoSetorCodigo = buscarCodigoSetorPorNome(novoSetorNome);

                        ItemPlanilha itemAntigo = null;
                        if (sessaoFinal.item != null) {
                            itemAntigo = new ItemPlanilha(
                                    sessaoFinal.item.loja, sessaoFinal.item.sqbem, sessaoFinal.item.codgrupo, sessaoFinal.item.codlocalizacao, sessaoFinal.item.nrobem,
                                    sessaoFinal.item.nroincorp, sessaoFinal.item.descresumida, sessaoFinal.item.descdetalhada, sessaoFinal.item.qtdbem,
                                    sessaoFinal.item.nroplaqueta, sessaoFinal.item.nroseriebem, sessaoFinal.item.modelobem
                            );

                            sessaoFinal.item.descresumida = novaDesc;
                            sessaoFinal.item.codlocalizacao = novoSetorCodigo;
                            sessaoFinal.item.loja = lojaAtualFinal;

                            if (sessaoFinal.item.nroplaqueta != null) {
                                BancoHelper bancoHelper = new BancoHelper(getApplicationContext());
                                bancoHelper.atualizarDescricaoESetor(sessaoFinal.item.nroplaqueta, novaDesc, novoSetorCodigo);
                            }
                        }

                        StringBuilder alteracoes = new StringBuilder();
                        if (itemAntigo != null) {
                            if (!itemAntigo.descresumida.equals(novaDesc)) {
                                alteracoes.append("Descrição: ")
                                        .append(itemAntigo.descresumida)
                                        .append(" -> ")
                                        .append(novaDesc)
                                        .append("; ");
                            }
                            if (!itemAntigo.codlocalizacao.equals(novoSetorCodigo)) {
                                alteracoes.append("Setor: ")
                                        .append(itemAntigo.codlocalizacao)
                                        .append(" -> ")
                                        .append(novoSetorCodigo)
                                        .append("; ");
                            }
                        }

                        if (alteracoes.length() > 0) {
                            LogHelper.logEdicaoItem(
                                    getApplicationContext(),
                                    usuario,
                                    lojaAtualFinal,
                                    novoSetorCodigo,
                                    itemAntigo,
                                    sessaoFinal.item,
                                    alteracoes.toString()
                            );
                        }

                        itensSessao.remove(sessaoFinal);
                        if (posVisivelFinal >= 0 && posVisivelFinal < itensVisiveis.size()) {
                            itensVisiveis.remove(posVisivelFinal);
                        }

                        aplicarFiltro(filtroAtual);
                        atualizarContadorItens();
                        dialogFinal.dismiss();
                    }
            );
        });

        btnCancelar.setOnClickListener(v -> dialogFinal.dismiss());

        dialog.show();
    }

    private String buscarNomeSetorPorCodigo(String codlocalizacao) {
        if (codlocalizacao == null) return codlocalizacao;

        List<SetorLocalizacao> setoresFiltrados = filtrarPorLoja(listaSetores, lojaSelecionada);
        String codNorm = codlocalizacao.trim();

        for (SetorLocalizacao s : setoresFiltrados) {
            if (s.codlocalizacao != null && s.codlocalizacao.trim().equals(codNorm)) {
                return s.setor;
            }
        }
        return codNorm;
    }

    private String buscarCodigoSetorPorNome(String nomeSetor) {
        if (nomeSetor == null) return nomeSetor;

        List<SetorLocalizacao> setoresFiltrados = filtrarPorLoja(listaSetores, lojaSelecionada);
        String nomeNorm = nomeSetor.trim();

        for (SetorLocalizacao s : setoresFiltrados) {
            if (s.setor != null && s.setor.trim().equals(nomeNorm)) {
                return s.codlocalizacao;
            }
        }
        return nomeNorm;
    }

    private List<SetorLocalizacao> filtrarPorLoja(List<SetorLocalizacao> todos, String lojaSelecionada) {
        List<SetorLocalizacao> resultado = new ArrayList<>();
        if (todos == null || todos.isEmpty()) return resultado;

        String codigoLoja = extrairCodigoLoja(lojaSelecionada);
        if (codigoLoja == null || codigoLoja.isEmpty()) {
            resultado.addAll(todos);
            return resultado;
        }

        for (SetorLocalizacao s : todos) {
            if (s == null) continue;
            if (codigoLoja.equals(s.loja)) {
                resultado.add(s);
            }
        }

        if (resultado.isEmpty()) resultado.addAll(todos);

        return resultado;
    }

    private String extrairCodigoLoja(String lojaSelecionada) {
        if (lojaSelecionada == null) return null;
        String s = lojaSelecionada.trim();
        if (s.isEmpty()) return null;

        int idx = s.indexOf('-');
        String numero = (idx > 0) ? s.substring(0, idx).trim() : s;

        numero = numero.replaceAll("\\D+", "");
        if (numero.isEmpty()) return null;

        if (numero.matches("^\\d+$")) {
            numero = numero.replaceFirst("^0+(?!$)", "");
        }

        return numero;
    }

    private List<SetorLocalizacao> getSetoresDaLoja(String loja) {
        List<SetorLocalizacao> result = new ArrayList<>();
        if (listaSetores == null || loja == null) return result;

        String lojaNorm = loja.trim();
        for (SetorLocalizacao s : listaSetores) {
            if (s == null || s.loja == null) continue;
            if (lojaNorm.equals(s.loja.trim())) {
                result.add(s);
            }
        }
        return result;
    }
    // ================= ETAPA 1 - CONFIRMAÇÃO AO SAIR =================

    private void solicitarConfirmacaoSaida() {

        View viewDialog = LayoutInflater.from(this)
                .inflate(R.layout.dialog_confirmacao, null);

        TextView tvTitulo = viewDialog.findViewById(R.id.tvConfirmTitle);
        TextView tvMensagem = viewDialog.findViewById(R.id.tvConfirmMsg);
        Button btnConfirmar = viewDialog.findViewById(R.id.btnConfirm);
        Button btnCancelar = viewDialog.findViewById(R.id.btnCancel);

        tvTitulo.setText("Atenção");
        tvMensagem.setText("Todas as alterações serão perdidas.\nDeseja sair?");
        btnConfirmar.setText("Confirmar");

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(viewDialog)
                .setCancelable(false)
                .create();
 
        btnConfirmar.setOnClickListener(v -> {
            dialog.dismiss();

            // Para a leitura caso esteja ativa
            try {
                if (leitorRFID != null && lendo) {
                    leitorRFID.pararLeitura();
                }
            } catch (Exception ignored) {}

            lendo = false;
            finish();
        });

        btnCancelar.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private boolean existeItemLojaErrada() {
        for (ItemLeituraSessao item : itensSessao) {
            if (item != null && item.status == ItemLeituraSessao.STATUS_LOJA_ERRADA) {
                return true;
            }
        }
        return false;
    }

    private boolean existeItemSetorErrado() {
        for (ItemLeituraSessao item : itensSessao) {
            if (item != null && item.status == ItemLeituraSessao.STATUS_SETOR_ERRADO) {
                return true;
            }
        }
        return false;
    }
    private void iniciarFluxoFinalizacao() {
        btnFinalizar.setText("Finalizando...");
        btnFinalizar.setEnabled(false);
        btnFinalizar.setTextColor(Color.WHITE);
        btnFinalizar.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#FFA000")));
        btnFinalizar.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_hourglass, 0, 0, 0);

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            new Thread(this::finalizarEExportar).start();
        }, 100);
    }
    // ===== TESTE SEM LEITOR (REMOVER DEPOIS) =====
    // ===== TESTE SEM LEITOR (REMOVER DEPOIS) =====
    private void simularItensParaTeste() {
        itensSessao.clear();

        ItemPlanilha fakePlanilha = new ItemPlanilha(
                "999",        // loja
                "", "",
                "SETOR_X",    // codlocalizacao
                "", "",
                "Item fake",
                "Item fake detalhado",
                "",
                "EPC_FAKE",
                "",
                ""
        );

        ItemLeituraSessao itemSetorErrado =
                new ItemLeituraSessao("EPC_TESTE_1", fakePlanilha);
        itemSetorErrado.status = ItemLeituraSessao.STATUS_SETOR_ERRADO;

        ItemLeituraSessao itemLojaErrada =
                new ItemLeituraSessao("EPC_TESTE_2", fakePlanilha);
        itemLojaErrada.status = ItemLeituraSessao.STATUS_LOJA_ERRADA;

        itensSessao.add(itemSetorErrado);
        itensSessao.add(itemLojaErrada);
    }


}
