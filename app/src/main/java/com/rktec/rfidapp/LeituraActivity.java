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

        // ==== SIMULAÇÃO DE LEITURA - BOTÃO NA TELA ====
        // Botão que abre o diálogo para digitar um EPC ou gerar um EPC aleatório
        // Button btnSimularEpc = findViewById(R.id.btnSimularEpc);
       // btnSimularEpc.setOnClickListener(v -> abrirDialogSimularLeitura());

        ImageButton back = findViewById(R.id.btnBack);
        back.setOnClickListener(v -> finish());

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

// SeekBar vai de 0 a 32 → potência real 1..33
        sbPotencia.setMax(32);

// progress interno = potenciaAtual - 1
        sbPotencia.setProgress(potenciaAtual - 1);

        tvPotencia.setText("Potência: " + potenciaAtual);

        sbPotencia.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                // Garante faixa 1..33
                potenciaAtual = progress + 1;
                tvPotencia.setText("Potência: " + potenciaAtual);
                if (leitorRFID != null) {
                    leitorRFID.setPotencia(potenciaAtual);
                }
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

        // Evita processar o mesmo EPC mais de uma vez na sessão
        for (ItemLeituraSessao i : itensSessao) {
            if (i.epc.equals(epcLimpo)) return;
        }

        // Procura o item na planilha base (por plaqueta)
        ItemPlanilha item = encontrarItemPorEPC(epc);

        ItemLeituraSessao novo = new ItemLeituraSessao(epcLimpo, item);

        // Classificação do status da leitura:
        //  - OK                  → item existe, loja e setor corretos
        //  - SETOR_ERRADO        → item existe, loja correta, mas setor diferente
        //  - LOJA_ERRADA         → item existe, mas em outra loja
        //  - NAO_ENCONTRADO      → EPC não existe na base
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
        adapter.notifyDataSetChanged();
        atualizarContadorItens();
        atualizarFeedbackDaLeitura(novo);
    }

    // ==== SIMULAÇÃO DE LEITURA - GERA EPC ALEATÓRIO ====
    // Usado pela simulação para criar um EPC válido e testar todo o fluxo sem o coletor físico
    private String gerarEpcAleatorio() {
        String chars = "0123456789ABCDEF";
        StringBuilder sb = new StringBuilder();
        // tamanho típico de EPC (ajusta se quiser)
        java.util.Random random = new java.util.Random();
        for (int i = 0; i < 24; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }

    // ==== SIMULAÇÃO DE LEITURA - DIALOG PARA DIGITAR/GERAR EPC ====
    // Abre um diálogo onde o usuário pode:
    //  - digitar um EPC manualmente, ou
    //  - deixar em branco / clicar em "Aleatório" para gerar um EPC aleatório
    // Em ambos os casos, chama processarEPC(epc), reutilizando a mesma lógica da leitura real.
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


    /**
     * Atualiza a mensagem e a cor de fundo da área de feedback (tvMsgLeitura)
     * de acordo com o status do último EPC lido.
     */
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
                corFundo = Color.parseColor("#E8F5E9"); // verde bem claro
                break;

            case ItemLeituraSessao.STATUS_SETOR_ERRADO:
                String setorBase = (sessao.item != null && sessao.item.codlocalizacao != null)
                        ? sessao.item.codlocalizacao
                        : "-";
                msg = "Atenção: item da loja correta, mas em outro setor (setor na base: " + setorBase + ").";
                corFundo = Color.parseColor("#FFF8E1"); // amarelo claro
                break;

            case ItemLeituraSessao.STATUS_LOJA_ERRADA:
                String lojaBase = (sessao.item != null && sessao.item.loja != null)
                        ? sessao.item.loja
                        : "-";
                msg = "Alerta: item pertence à loja " + lojaBase + ", não à loja " + lojaSelecionada + ".";
                corFundo = Color.parseColor("#FFF3E0"); // laranja claro
                break;

            case ItemLeituraSessao.STATUS_NAO_ENCONTRADO:
            default:
                msg = "Item não encontrado na base: EPC " + sessao.epc;
                corFundo = Color.parseColor("#FFEBEE"); // vermelho bem claro
                break;
        }

        tvMsgLeitura.setText(msg);

        // Mantém o desenho arredondado do bg_msg_leitura, apenas trocando a cor
        android.graphics.drawable.Drawable bg = tvMsgLeitura.getBackground();
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

        // Loja atual: se o item tiver loja, usa ela; senão, usa a loja selecionada na sessão
        String lojaAtual = (sessao.item != null && sessao.item.loja != null && !sessao.item.loja.trim().isEmpty())
                ? sessao.item.loja.trim()
                : (lojaSelecionada != null ? lojaSelecionada : "");

        tvLoja.setText("Loja: " + lojaAtual);

        // Preenche descrição (dá preferência à detalhada)
        if (sessao.item != null) {
            String textoDesc;
            if (sessao.item.descdetalhada != null && !sessao.item.descdetalhada.trim().isEmpty()) {
                textoDesc = sessao.item.descdetalhada.trim();
            } else if (sessao.item.descresumida != null && !sessao.item.descresumida.trim().isEmpty()) {
                textoDesc = sessao.item.descresumida.trim();
            } else {
                textoDesc = "";
            }
            edtDesc.setText(textoDesc);
        } else {
            edtDesc.setText("");
        }

        tvPlaqueta.setText("Plaqueta: " + sessao.epc);

        // ====== CARREGA APENAS SETORES DA LOJA ATUAL ======
        // Carrega APENAS os setores da loja selecionada (mesma lógica da SetorActivity)
        List<SetorLocalizacao> setoresFiltrados = filtrarPorLoja(listaSetores, lojaSelecionada);

        List<String> nomesSetores = new ArrayList<>();
        for (SetorLocalizacao s : setoresFiltrados) {
            nomesSetores.add(s.setor); // "LJ - PADARIA", "LJ - FRIOS", etc.
        }

        ArrayAdapter<String> setorAdapter = new ArrayAdapter<>(
                LeituraActivity.this,
                android.R.layout.simple_spinner_item,
                nomesSetores
        );
        setorAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerSetor.setAdapter(setorAdapter);
        // Seleciona o setor atual do item (busca o nome a partir do código, DENTRO da loja)
        String setorAtualNome;
        if (sessao.item != null && sessao.item.codlocalizacao != null) {
            setorAtualNome = buscarNomeSetorPorCodigo(sessao.item.codlocalizacao);
        } else if (setorSelecionado != null) {
            setorAtualNome = setorSelecionado.setor;
        } else {
            setorAtualNome = null;
        }

        if (setorAtualNome != null) {
            int idx = nomesSetores.indexOf(setorAtualNome);
            if (idx >= 0) {
                spinnerSetor.setSelection(idx);
            }
        }

        // Cria dialog
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(view)
                .setCancelable(false)
                .create();

        // SALVAR
        btnSalvar.setOnClickListener(v -> {
            mostrarDialogConfirmacao(
                    "Confirmar alteração",
                    Color.parseColor("#1976D2"),
                    "Tem certeza que deseja salvar as alterações deste item?",
                    "Salvar",
                    () -> {
                        String novaDescDet = edtDesc.getText().toString();
                        String novoSetorNome = (String) spinnerSetor.getSelectedItem();
                        String novoSetorCodigo = buscarCodigoSetorPorNome(novoSetorNome);

                        // Guarda cópia antiga pra log
                        ItemPlanilha itemAntigo = null;
                        if (sessao.item != null) {
                            itemAntigo = new ItemPlanilha(
                                    sessao.item.loja, sessao.item.sqbem, sessao.item.codgrupo, sessao.item.codlocalizacao, sessao.item.nrobem,
                                    sessao.item.nroincorp, sessao.item.descresumida, sessao.item.descdetalhada, sessao.item.qtdbem,
                                    sessao.item.nroplaqueta, sessao.item.nroseriebem, sessao.item.modelobem
                            );
                        }

                        if (sessao.item == null) {
                            // Item "fake" criado na sessão
                            ItemPlanilha itemNovoFake = new ItemPlanilha(
                                    lojaAtual, "", "", novoSetorCodigo, "", "",
                                    novaDescDet,          // descresumida
                                    novaDescDet,          // descdetalhada
                                    "",                   // qtdbem
                                    sessao.epc, "", ""    // plaqueta / serie / modelo
                            );
                            sessao.item = itemNovoFake;
                            sessao.encontrado = true;
                            adapter.notifyDataSetChanged();
                            atualizarContadorItens();
                            dialog.dismiss();
                            return;
                        } else {
                            // Atualiza item real
                            sessao.item.descdetalhada = novaDescDet;
                            if (sessao.item.descresumida == null || sessao.item.descresumida.trim().isEmpty()) {
                                sessao.item.descresumida = novaDescDet;
                            }
                            sessao.item.loja = lojaAtual;
                            sessao.item.codlocalizacao = novoSetorCodigo;
                        }

                        if (sessao.item != null && sessao.item.nroplaqueta != null) {
                            BancoHelper bancoHelper = new BancoHelper(getApplicationContext());
                            bancoHelper.atualizarDescricaoESetor(sessao.item.nroplaqueta, novaDescDet, novoSetorCodigo);
                        }

                        // Loga alterações
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

        // REMOVER
        btnRemover.setOnClickListener(v -> {
            mostrarDialogConfirmacao(
                    "Remover item",
                    Color.parseColor("#D32F2F"),
                    "Tem certeza que deseja salvar as alterações e remover esse item da lista?\n\nEsta ação não pode ser desfeita.",
                    "Remover",
                    () -> {
                        String novaDesc = edtDesc.getText().toString();
                        String novoSetorNome = (String) spinnerSetor.getSelectedItem();
                        String novoSetorCodigo = buscarCodigoSetorPorNome(novoSetorNome);

                        ItemPlanilha itemAntigo = null;
                        if (sessao.item != null) {
                            itemAntigo = new ItemPlanilha(
                                    sessao.item.loja, sessao.item.sqbem, sessao.item.codgrupo, sessao.item.codlocalizacao, sessao.item.nrobem,
                                    sessao.item.nroincorp, sessao.item.descresumida, sessao.item.descdetalhada, sessao.item.qtdbem,
                                    sessao.item.nroplaqueta, sessao.item.nroseriebem, sessao.item.modelobem
                            );

                            sessao.item.descresumida = novaDesc;
                            sessao.item.codlocalizacao = novoSetorCodigo;
                            sessao.item.loja = lojaAtual;

                            if (sessao.item.nroplaqueta != null) {
                                BancoHelper bancoHelper = new BancoHelper(getApplicationContext());
                                bancoHelper.atualizarDescricaoESetor(sessao.item.nroplaqueta, novaDesc, novoSetorCodigo);
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
                                    lojaAtual,
                                    novoSetorCodigo,
                                    itemAntigo,
                                    sessao.item,
                                    alteracoes.toString()
                            );
                        }

                        itensSessao.remove(pos);
                        adapter.notifyDataSetChanged();
                        atualizarContadorItens();
                        dialog.dismiss();
                    }
            );
        });

        btnCancelar.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }
    // Métodos auxiliares pra lidar com código e nome do setor
    // Busca o NOME do setor a partir do CÓDIGO, dentro da loja informada
    // Busca o NOME do setor a partir do CÓDIGO, só dentro da loja selecionada
    private String buscarNomeSetorPorCodigo(String codlocalizacao) {
        if (codlocalizacao == null) return codlocalizacao;

        List<SetorLocalizacao> setoresFiltrados = filtrarPorLoja(listaSetores, lojaSelecionada);
        String codNorm = codlocalizacao.trim();

        for (SetorLocalizacao s : setoresFiltrados) {
            if (s.codlocalizacao != null && s.codlocalizacao.trim().equals(codNorm)) {
                return s.setor;
            }
        }
        return codNorm; // fallback se não encontrar
    }

    // Busca o CÓDIGO do setor a partir do NOME, só dentro da loja selecionada
    private String buscarCodigoSetorPorNome(String nomeSetor) {
        if (nomeSetor == null) return nomeSetor;

        List<SetorLocalizacao> setoresFiltrados = filtrarPorLoja(listaSetores, lojaSelecionada);
        String nomeNorm = nomeSetor.trim();

        for (SetorLocalizacao s : setoresFiltrados) {
            if (s.setor != null && s.setor.trim().equals(nomeNorm)) {
                return s.codlocalizacao;
            }
        }
        return nomeNorm; // fallback se não encontrar
    }

    /**
     * Filtra a lista de setores pela loja selecionada.
     * Mesmo comportamento da SetorActivity.
     */
    private List<SetorLocalizacao> filtrarPorLoja(List<SetorLocalizacao> todos, String lojaSelecionada) {
        List<SetorLocalizacao> resultado = new ArrayList<>();
        if (todos == null || todos.isEmpty()) return resultado;

        String codigoLoja = extrairCodigoLoja(lojaSelecionada); // ex.: "001-CARPINA" -> "1"
        if (codigoLoja == null || codigoLoja.isEmpty()) {
            // Não achou código de loja, devolve tudo mesmo
            resultado.addAll(todos);
            return resultado;
        }

        for (SetorLocalizacao s : todos) {
            if (s == null) continue;
            if (codigoLoja.equals(s.loja)) {
                resultado.add(s);
            }
        }

        // Se por algum motivo não achou nada, devolve tudo pra não ficar vazio
        if (resultado.isEmpty()) {
            resultado.addAll(todos);
        }

        return resultado;
    }

    /**
     * Mesmo extrairCodigoLoja da SetorActivity.
     *
     * Exemplos:
     *  "001-CARPINA"   -> "1"
     *  "002-VIT.SA"    -> "2"
     *  "500-MATRIZ"    -> "500"
     */
    private String extrairCodigoLoja(String lojaSelecionada) {
        if (lojaSelecionada == null) return null;
        String s = lojaSelecionada.trim();
        if (s.isEmpty()) return null;

        // pega tudo antes do primeiro hífen
        int idx = s.indexOf('-');
        String numero = (idx > 0) ? s.substring(0, idx).trim() : s;

        // mantém só dígitos
        numero = numero.replaceAll("\\D+", "");
        if (numero.isEmpty()) return null;

        // remove zeros à esquerda ("001" -> "1")
        if (numero.matches("^\\d+$")) {
            numero = numero.replaceFirst("^0+(?!$)", "");
        }

        return numero;
    }

    // Retorna apenas os setores da loja informada
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

}
