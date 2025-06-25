package com.rktec.rfidapp;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.*;
import android.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.pda.rfid.EPCModel;
import com.pda.rfid.IAsynchronousMessage;
import java.util.*;
import android.graphics.Canvas;


public class LeituraActivity extends AppCompatActivity implements IAsynchronousMessage {

    private List<ItemLeituraSessao> itensSessao = new ArrayList<>();
    private LeitorRFID leitorRFID;
    private String lojaSelecionada, usuario;
    private SetorLocalizacao setorSelecionado;
    private List<ItemPlanilha> listaPlanilha;
    private List<SetorLocalizacao> listaSetores;
    private ItemLeituraSessaoAdapter adapter;
    private HashSet<String> epcsJaProcessados = new HashSet<>();
    private boolean lendo = false;
    private TextView tvMsgLeitura, tvContadorItens;
    private HashMap<String, ItemPlanilha> mapPlaquetasGlobal;
    private int potenciaAtual = 20;
    private Button btnFinalizar;
    private MediaPlayer mpSucesso;
    private long ultimoVolumeDown = 0;
    private static final long TEMPO_CONFIRMACAO = 2000; // 2 segundos

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_leitura_rfid);

        tvContadorItens = findViewById(R.id.tvContadorItens);
        tvMsgLeitura = findViewById(R.id.tvMsgLeitura);
        btnFinalizar = findViewById(R.id.btnFinalizar);

        setorSelecionado = DadosGlobais.getInstance().getSetorSelecionado();
        if (setorSelecionado == null) {
            Toast.makeText(this, "Nenhum setor selecionado! Voltando...", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        BancoHelper bancoHelper = new BancoHelper(this);
        listaPlanilha = DadosGlobais.getInstance().getListaPlanilha();
        listaSetores = DadosGlobais.getInstance().getListaSetores();
        lojaSelecionada = DadosGlobais.getInstance().getLojaSelecionada();
        usuario = DadosGlobais.getInstance().getUsuario();
        if (usuario == null)
            usuario = getSharedPreferences("prefs", MODE_PRIVATE).getString("usuario_nome", "Usuário");

        construirMapaPlaquetasGlobal(listaPlanilha);

        ((TextView) findViewById(R.id.tvLojaSelecionada)).setText("Loja: " + lojaSelecionada);
        ((TextView) findViewById(R.id.tvSetorSelecionado)).setText("Setor: " + setorSelecionado.setor);

        RecyclerView recyclerView = findViewById(R.id.listaItensLidos);
        adapter = new ItemLeituraSessaoAdapter(itensSessao);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        // Clique abre dialog de edição
        adapter.setOnItemClickListener(position -> {
            ItemLeituraSessao itemSessao = itensSessao.get(position);
            abrirDialogEdicao(itemSessao, position);
        });

        // Swipe esquerda remove direto (sem editar/salvar)
        ItemTouchHelper.SimpleCallback simpleCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            @Override
            public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
                int pos = viewHolder.getAdapterPosition();
                itensSessao.remove(pos);
                adapter.notifyItemRemoved(pos);
                atualizarContadorItens();
                Toast.makeText(LeituraActivity.this, "Item removido!", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onChildDraw(Canvas c, RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder,
                                    float dX, float dY, int actionState, boolean isCurrentlyActive) {
                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);

                // Se deslizando pra esquerda (dX < 0)
                if (dX < 0) {
                    View itemView = viewHolder.itemView;

                    // Fundo vermelho proporcional ao swipe
                    Paint p = new Paint();
                    p.setColor(Color.parseColor("#D32F2F"));
                    c.drawRect(
                            itemView.getRight() + dX, itemView.getTop(),
                            itemView.getRight(), itemView.getBottom(), p);

                    // Ícone da lixeira centralizado
                    Drawable icon = ContextCompat.getDrawable(itemView.getContext(), R.drawable.ic_delete);
                    int iconMargin = (itemView.getHeight() - icon.getIntrinsicHeight()) / 2;
                    int iconTop = itemView.getTop() + iconMargin;
                    int iconBottom = iconTop + icon.getIntrinsicHeight();
                    int iconLeft = itemView.getRight() - iconMargin - icon.getIntrinsicWidth();
                    int iconRight = itemView.getRight() - iconMargin;
                    icon.setBounds(iconLeft, iconTop, iconRight, iconBottom);
                    icon.setAlpha((int) (255 * Math.min(1f, Math.abs(dX) / itemView.getWidth()))); // efeito fade se quiser

                    icon.draw(c);
                }
            }
        };
        new ItemTouchHelper(simpleCallback).attachToRecyclerView(recyclerView);
        mpSucesso = MediaPlayer.create(this, R.raw.sucesso);

        btnFinalizar.setOnClickListener(v -> {
            btnFinalizar.setText("Finalizando...");
            btnFinalizar.setEnabled(false);
            btnFinalizar.setTextColor(Color.WHITE);
            btnFinalizar.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#FFA000")));
            btnFinalizar.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_hourglass, 0, 0, 0);
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                new Thread(this::finalizarEExportar).start();
            }, 100);
        });

        atualizarContadorItens();

        SeekBar sbPotencia = findViewById(R.id.sbPotencia);
        TextView tvPotencia = findViewById(R.id.tvPotencia);
        tvPotencia.setText("Potência: " + potenciaAtual);
        sbPotencia.setProgress(potenciaAtual);
        sbPotencia.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                potenciaAtual = progress;
                tvPotencia.setText("Potência: " + potenciaAtual);
                if (leitorRFID != null) leitorRFID.setPotencia(potenciaAtual);
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) { }
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) { }
        });
    }

    private boolean getPreferencia(String chave, boolean padrao) {
        return getSharedPreferences("prefs", MODE_PRIVATE).getBoolean(chave, padrao);
    }

    private void construirMapaPlaquetasGlobal(List<ItemPlanilha> lista) {
        mapPlaquetasGlobal = new HashMap<>();
        for (ItemPlanilha item : lista) {
            if (item.nroplaqueta != null) {
                String chave = item.nroplaqueta.trim().replaceFirst("^0+(?!$)", "");
                mapPlaquetasGlobal.put(chave, item);
            }
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == 139 && !lendo && setorSelecionado != null) {
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
                // Segunda vez: finaliza de verdade
                btnFinalizar.performClick(); // Chama o mesmo fluxo do botão!
                ultimoVolumeDown = 0; // Reseta para novas confirmações depois
            } else {
                Toast.makeText(this, "Aperte o volume - novamente para finalizar!", Toast.LENGTH_SHORT).show();
                ultimoVolumeDown = agora;
            }
            return true; // Consome o evento (não baixa volume)
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
        String epc = model._EPC;
        String epcLimpo = formatarEPC(epc);
        if (epcsJaProcessados.contains(epcLimpo)) return;
        epcsJaProcessados.add(epcLimpo);
        runOnUiThread(() -> processarEPC(epc));
    }

    private void processarEPC(String epc) {
        String epcLimpo = formatarEPC(epc);

        for (ItemLeituraSessao i : itensSessao)
            if (i.epc.equals(epcLimpo)) return;

        ItemPlanilha item = encontrarItemPorEPC(epc);

        ItemLeituraSessao novo = new ItemLeituraSessao(epcLimpo, item);
        itensSessao.add(novo);

        adapter.notifyDataSetChanged();
        atualizarContadorItens();

        if (item != null && item.loja.equals(lojaSelecionada)) {
            tvMsgLeitura.setText("Encontrado: " + item.descresumida);
        } else {
            tvMsgLeitura.setText("Novo EPC: " + epcLimpo);
        }
    }

    private ItemPlanilha encontrarItemPorEPC(String epc) {
        if (mapPlaquetasGlobal == null || epc == null) return null;
        String epcLido = epc.trim().replaceFirst("^0+(?!$)", "");
        return mapPlaquetasGlobal.get(epcLido);
    }

    private void atualizarContadorItens() {
        int total = 0, lidos = 0;
        Set<String> nomesItensSetor = new HashSet<>();
        for (ItemPlanilha item : listaPlanilha) {
            if (item.loja.equals(lojaSelecionada)
                    && item.codlocalizacao != null
                    && item.codlocalizacao.equals(setorSelecionado.codlocalizacao)
            ) {
                total++;
                nomesItensSetor.add(item.descresumida);
            }
        }
        for (ItemLeituraSessao lido : itensSessao) {
            if (lido.item != null && nomesItensSetor.contains(lido.item.descresumida)) {
                lidos++;
            }
        }
        tvContadorItens.setText("Itens lidos: " + lidos + " / " + total);
    }

    // Processo pesado isolado da UI
    private void finalizarEExportar() {
        List<ItemPlanilha> itensMovidos = new ArrayList<>();
        List<ItemPlanilha> itensOutrasLojas = new ArrayList<>();
        List<String> epcsNaoCadastrados = new ArrayList<>();
        List<ItemLeituraSessao> itensNaoCadastradosEditados = new ArrayList<>();

        for (ItemLeituraSessao lido : itensSessao) {
            // Se foi editado (ou seja, tem um item "fake") e NÃO está na planilha, logar só agora!
            boolean naoEstaNaPlanilha = (lido.item == null) || !listaPlanilha.contains(lido.item);
            boolean editou = lido.item != null && lido.item.descresumida != null && !lido.item.descresumida.isEmpty();
            if (naoEstaNaPlanilha && editou) {
                itensNaoCadastradosEditados.add(lido);
                continue; // Não coloca como epcsNaoCadastrados, pois foi editado!
            }
            if (lido.item == null) {
                epcsNaoCadastrados.add(lido.epc);
                continue;
            }
            String plaqLimpo = lido.item.nroplaqueta != null ? lido.item.nroplaqueta.trim().replaceFirst("^0+(?!$)", "") : "";
            if (plaqLimpo.equals(lido.epc)) {
                if (lido.item.loja.equals(lojaSelecionada)) {
                    if (!lido.item.codlocalizacao.equals(setorSelecionado.codlocalizacao)) {
                        lido.item.codlocalizacao = setorSelecionado.codlocalizacao;
                    }
                    itensMovidos.add(lido.item);
                } else {
                    itensOutrasLojas.add(lido.item);
                }
            }
        }

        // LOGA EDIÇÕES DE ITENS NÃO CADASTRADOS (só aqui! Só 1x!)
        for (ItemLeituraSessao editado : itensNaoCadastradosEditados) {
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
                setorSelecionado.setor,
                itensMovidos,
                itensOutrasLojas,
                epcsNaoCadastrados
        );

        String caminho = ExportadorPlanilha.exportarCSV(this, listaPlanilha).getAbsolutePath();

        runOnUiThread(() -> {
            if (mpSucesso != null) mpSucesso.start();
            Toast.makeText(this, "Exportado para: " + caminho, Toast.LENGTH_LONG).show();
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                btnFinalizar.setText("Concluído!");
                btnFinalizar.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#43A047"))); // verde
                btnFinalizar.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_check, 0, 0, 0);
                Intent intent = new Intent(this, SetorActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                finish();
            }, 1200);
        });
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

        btnPositivo.setOnClickListener(v -> {
            acaoConfirmar.run();
            dialog.dismiss();
        });

        btnNegativo.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    // --- Dialog de edição para qualquer item lido ---
    private void abrirDialogEdicao(ItemLeituraSessao sessao, int pos) {
        LayoutInflater inflater = LayoutInflater.from(this);
        View view = inflater.inflate(R.layout.dialog_editar_item_lido, null);

        EditText edtDesc = view.findViewById(R.id.edtDescResumidaDialog);
        TextView tvLoja = view.findViewById(R.id.tvLojaDialog);
        Spinner spinnerSetor = view.findViewById(R.id.spinnerSetorDialog);
        TextView tvPlaqueta = view.findViewById(R.id.tvPlaquetaDialog);
        Button btnRemover = view.findViewById(R.id.btnRemoverDialog);
        Button btnSalvar = view.findViewById(R.id.btnSalvarDialog);
        Button btnCancelar = view.findViewById(R.id.btnCancelarDialog);

        // Preenche campos
        edtDesc.setText(sessao.item != null ? sessao.item.descresumida : "");
        tvPlaqueta.setText("Plaqueta: " + sessao.epc);

        // Nome da loja (apenas visual, não editável)
        String lojaAtual = sessao.item != null ? sessao.item.loja : lojaSelecionada;
        tvLoja.setText("Loja: " + lojaAtual);

        // Carrega setores (exibe nome, mas salva o código)
        List<String> nomesSetores = new ArrayList<>();
        for (SetorLocalizacao s : listaSetores) {
            nomesSetores.add(s.setor);
        }
        ArrayAdapter<String> setorAdapter = new ArrayAdapter<>(LeituraActivity.this, android.R.layout.simple_spinner_item, nomesSetores);
        setorAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerSetor.setAdapter(setorAdapter);

        // Seleciona setor atual (busca nome pelo código)
        String setorAtual = sessao.item != null
                ? buscarNomeSetorPorCodigo(sessao.item.codlocalizacao)
                : setorSelecionado.setor;
        int setorIndex = nomesSetores.indexOf(setorAtual);
        if (setorIndex >= 0) spinnerSetor.setSelection(setorIndex);

        // Cria dialog principal
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(view)
                .setCancelable(false)
                .create();

        // Salvar com confirmação customizada
        btnSalvar.setOnClickListener(v -> {
            mostrarDialogConfirmacao(
                    "Confirmar alteração",
                    Color.parseColor("#1976D2"),
                    "Tem certeza que deseja salvar as alterações deste item?",
                    "Salvar",
                    () -> {
                        String novaDesc = edtDesc.getText().toString();
                        String novoSetorNome = (String) spinnerSetor.getSelectedItem();
                        String novoSetorCodigo = buscarCodigoSetorPorNome(novoSetorNome);

                        // Salva dados antigos antes de alterar
                        ItemPlanilha itemAntigo = null;
                        if (sessao.item != null) {
                            itemAntigo = new ItemPlanilha(
                                    sessao.item.loja, sessao.item.sqbem, sessao.item.codgrupo, sessao.item.codlocalizacao, sessao.item.nrobem,
                                    sessao.item.nroincorp, sessao.item.descresumida, sessao.item.descdetalhada, sessao.item.qtdbem,
                                    sessao.item.nroplaqueta, sessao.item.nroseriebem, sessao.item.modelobem
                            );
                        }

                        if (sessao.item == null) {
                            // Só atualiza o objeto visual na lista, não salva na planilha real, nem loga agora!
                            ItemPlanilha itemNovoFake = new ItemPlanilha(
                                    lojaAtual, "", "", novoSetorCodigo, "", "",
                                    novaDesc, "", "", sessao.epc, "", ""
                            );
                            sessao.item = itemNovoFake;
                            sessao.encontrado = true;
                            adapter.notifyDataSetChanged();
                            atualizarContadorItens();
                            dialog.dismiss();
                            return;
                        } else {
                            sessao.item.descresumida = novaDesc;
                            sessao.item.loja = lojaAtual;
                            sessao.item.codlocalizacao = novoSetorCodigo;
                        }

                        if (sessao.item != null && sessao.item.nroplaqueta != null) {
                            BancoHelper bancoHelper = new BancoHelper(getApplicationContext());
                            bancoHelper.atualizarDescricaoESetor(sessao.item.nroplaqueta, novaDesc, novoSetorCodigo);
                        }
                        // Verifica alterações (só registra se editou algo)
                        StringBuilder alteracoes = new StringBuilder();
                        if (itemAntigo != null) {
                            if (!itemAntigo.descresumida.equals(novaDesc))
                                alteracoes.append("Descrição: ").append(itemAntigo.descresumida).append(" -> ").append(novaDesc).append("; ");
                            if (!itemAntigo.codlocalizacao.equals(novoSetorCodigo))
                                alteracoes.append("Setor: ").append(itemAntigo.codlocalizacao).append(" -> ").append(novoSetorCodigo).append("; ");
                        }

                        if (alteracoes.length() > 0) {
                            LogHelper.logEdicaoItem(
                                    getApplicationContext(),
                                    usuario,
                                    lojaAtual,
                                    novoSetorCodigo,
                                    itemAntigo,
                                    sessao.item,
                                    alteracoes.toString()
                            );
                        }

                        adapter.notifyDataSetChanged();
                        atualizarContadorItens();
                        dialog.dismiss();
                    }
            );
        });

        // Remover com confirmação customizada
        btnRemover.setOnClickListener(v -> {
            mostrarDialogConfirmacao(
                    "Remover item",
                    Color.parseColor("#D32F2F"),
                    "Tem certeza que deseja salvar as alterações e remover esse item da lista?\n\nEsta ação não pode ser desfeita.",
                    "Remover",
                    () -> {
                        // --- SALVA ALTERAÇÕES ANTES DE REMOVER ---
                        String novaDesc = edtDesc.getText().toString();
                        String novoSetorNome = (String) spinnerSetor.getSelectedItem();
                        String novoSetorCodigo = buscarCodigoSetorPorNome(novoSetorNome);

                        // Salva dados antigos antes de alterar
                        ItemPlanilha itemAntigo = null;
                        if (sessao.item != null) {
                            itemAntigo = new ItemPlanilha(
                                    sessao.item.loja, sessao.item.sqbem, sessao.item.codgrupo, sessao.item.codlocalizacao, sessao.item.nrobem,
                                    sessao.item.nroincorp, sessao.item.descresumida, sessao.item.descdetalhada, sessao.item.qtdbem,
                                    sessao.item.nroplaqueta, sessao.item.nroseriebem, sessao.item.modelobem
                            );
                        }

                        if (sessao.item != null) {
                            sessao.item.descresumida = novaDesc;
                            sessao.item.codlocalizacao = novoSetorCodigo;
                            sessao.item.loja = lojaAtual; // só pra garantir

                            // Atualiza no banco se for item real (se não for fake/temporário)
                            if (sessao.item.nroplaqueta != null) {
                                BancoHelper bancoHelper = new BancoHelper(getApplicationContext());
                                bancoHelper.atualizarDescricaoESetor(sessao.item.nroplaqueta, novaDesc, novoSetorCodigo);
                            }
                        }

                        // Log de edição (se mudou algo)
                        StringBuilder alteracoes = new StringBuilder();
                        if (itemAntigo != null) {
                            if (!itemAntigo.descresumida.equals(novaDesc))
                                alteracoes.append("Descrição: ").append(itemAntigo.descresumida).append(" -> ").append(novaDesc).append("; ");
                            if (!itemAntigo.codlocalizacao.equals(novoSetorCodigo))
                                alteracoes.append("Setor: ").append(itemAntigo.codlocalizacao).append(" -> ").append(novoSetorCodigo).append("; ");
                        }
                        if (alteracoes.length() > 0) {
                            LogHelper.logEdicaoItem(
                                    getApplicationContext(),
                                    usuario,
                                    lojaAtual,
                                    novoSetorCodigo,
                                    itemAntigo,
                                    sessao.item,
                                    alteracoes.toString()
                            );
                        }

                        // --- AGORA REMOVE DA LISTA TEMPORÁRIA ---
                        itensSessao.remove(pos);
                        adapter.notifyDataSetChanged();
                        dialog.dismiss();
                        atualizarContadorItens();
                    }
            );
        });


        btnCancelar.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    // Métodos auxiliares pra lidar com código e nome do setor
    private String buscarNomeSetorPorCodigo(String codlocalizacao) {
        for (SetorLocalizacao s : listaSetores) {
            if (s.codlocalizacao.equals(codlocalizacao)) {
                return s.setor;
            }
        }
        return codlocalizacao; // fallback se não achar
    }

    private String buscarCodigoSetorPorNome(String nomeSetor) {
        for (SetorLocalizacao s : listaSetores) {
            if (s.setor.equals(nomeSetor)) {
                return s.codlocalizacao;
            }
        }
        return nomeSetor; // fallback se não achar
    }
}
