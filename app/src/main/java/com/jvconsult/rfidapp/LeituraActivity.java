package com.jvconsult.rfidapp;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
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
import com.pda.rfid.EPCModel;
import com.pda.rfid.IAsynchronousMessage;
import java.util.*;

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
    private ArrayList<String> epcsLidosNaSessao = new ArrayList<>();
    private int potenciaAtual = 20;
    private Button btnFinalizar;
    private MediaPlayer mpSucesso;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_leitura_rfid);

        tvContadorItens = findViewById(R.id.tvContadorItens);
        tvMsgLeitura    = findViewById(R.id.tvMsgLeitura);
        btnFinalizar    = findViewById(R.id.btnFinalizar);

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
        if (usuario == null)
            usuario = getSharedPreferences("prefs", MODE_PRIVATE).getString("usuario_nome", "Usuário");

        construirMapaPlaquetasGlobal(listaPlanilha);

        ((TextView) findViewById(R.id.tvLojaSelecionada)).setText("Loja: " + lojaSelecionada);
        ((TextView) findViewById(R.id.tvSetorSelecionado)).setText("Setor: " + setorSelecionado.setor);

        ListView listView = findViewById(R.id.listaItensLidos);
        adapter = new ItemLeituraSessaoAdapter(this, itensSessao);
        listView.setAdapter(adapter);

        // Clicável: abre dialog de edição SEMPRE (encontrado ou não)
        listView.setOnItemClickListener((parent, view, position, id) -> {
            ItemLeituraSessao itemSessao = itensSessao.get(position);
            abrirDialogEdicao(itemSessao, position);
        });

        mpSucesso = MediaPlayer.create(this, R.raw.sucesso);

        btnFinalizar.setOnClickListener(v -> {
            // Feedback visual imediato
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
            @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                potenciaAtual = progress;
                tvPotencia.setText("Potência: " + potenciaAtual);
                if (leitorRFID != null) leitorRFID.setPotencia(potenciaAtual);
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });
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
    public void OutPutEPC(EPCModel model) {
        String epc = model._EPC;
        String epcLimpo = formatarEPC(epc);
        if (epcsJaProcessados.contains(epcLimpo)) return;
        epcsJaProcessados.add(epcLimpo);
        runOnUiThread(() -> processarEPC(epc));
    }

    private void processarEPC(String epc) {
        String epcLimpo = formatarEPC(epc);

        // Só adiciona 1x cada epc na sessão
        for (ItemLeituraSessao i : itensSessao)
            if (i.epc.equals(epcLimpo)) return;

        ItemPlanilha item = encontrarItemPorEPC(epc);

        ItemLeituraSessao novo = new ItemLeituraSessao(epcLimpo, item);
        itensSessao.add(novo);

        // Se não existe na planilha, deixa com campos vazios, pronto pra editar
        adapter.notifyDataSetChanged();
        atualizarContadorItens();

        // Feedback visual/sonoro
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

        for (ItemLeituraSessao lido : itensSessao) {
            if (lido.item == null) {
                // Aqui sim: só adiciona quem continua sem cadastro ao salvar!
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

    // --- Dialog de edição para qualquer item lido ---
    private void abrirDialogEdicao(ItemLeituraSessao sessao, int pos) {
        LayoutInflater inflater = LayoutInflater.from(this);
        View view = inflater.inflate(R.layout.dialog_editar_item_lido, null);

        EditText edtDesc = view.findViewById(R.id.edtDescResumidaDialog);
        Spinner spinnerLoja = view.findViewById(R.id.spinnerLojaDialog);
        Spinner spinnerSetor = view.findViewById(R.id.spinnerSetorDialog);
        TextView tvPlaqueta = view.findViewById(R.id.tvPlaquetaDialog);
        Button btnRemover = view.findViewById(R.id.btnRemoverDialog);
        Button btnSalvar = view.findViewById(R.id.btnSalvarDialog);
        Button btnCancelar = view.findViewById(R.id.btnCancelarDialog);

        // Preenche campos
        edtDesc.setText(sessao.item != null ? sessao.item.descresumida : "");
        tvPlaqueta.setText("Plaqueta: " + sessao.epc);

        // Carrega lojas
        List<String> lojas = new ArrayList<>();
        for (ItemPlanilha item : listaPlanilha) {
            if (item.loja != null && !lojas.contains(item.loja)) lojas.add(item.loja);
        }
        ArrayAdapter<String> lojaAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, lojas);
        lojaAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerLoja.setAdapter(lojaAdapter);

        // Seleciona loja correta
        String lojaAtual = sessao.item != null ? sessao.item.loja : lojaSelecionada;
        int lojaIndex = lojas.indexOf(lojaAtual);
        if (lojaIndex >= 0) spinnerLoja.setSelection(lojaIndex);

        // Carrega setores conforme loja selecionada
        spinnerLoja.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                // Não filtra por loja, só mostra todos (adapte se quiser filtrar depois)
                List<String> setores = new ArrayList<>();
                for (SetorLocalizacao s : listaSetores) {
                    setores.add(s.setor);
                }
                ArrayAdapter<String> setorAdapter = new ArrayAdapter<>(LeituraActivity.this, android.R.layout.simple_spinner_item, setores);
                setorAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                spinnerSetor.setAdapter(setorAdapter);

                // Seleciona setor correto
                String setorAtual = sessao.item != null ? sessao.item.codlocalizacao : setorSelecionado.setor;
                int setorIndex = setores.indexOf(setorAtual);
                if (setorIndex >= 0) spinnerSetor.setSelection(setorIndex);
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });
        // Força disparar seleção do setor
        spinnerLoja.post(() -> spinnerLoja.setSelection(lojaIndex >= 0 ? lojaIndex : 0));

        // Cria dialog principal
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(view)
                .setCancelable(false)
                .create();

        // Salvar com confirmação
        btnSalvar.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("Confirmar alteração")
                    .setMessage("Tem certeza que deseja salvar as alterações deste item?")
                    .setPositiveButton("Salvar", (d, w) -> {
                        String novaDesc = edtDesc.getText().toString();
                        String novaLoja = (String) spinnerLoja.getSelectedItem();
                        String novoSetor = (String) spinnerSetor.getSelectedItem();

                        if (sessao.item == null) {
                            ItemPlanilha novoItem = new ItemPlanilha(
                                    novaLoja, "", "", novoSetor, "", "",
                                    novaDesc, "", "", sessao.epc, "", ""
                            );
                            listaPlanilha.add(novoItem);
                            sessao.item = novoItem;
                            mapPlaquetasGlobal.put(sessao.epc, novoItem);
                            sessao.encontrado = true;
                        } else {
                            sessao.item.descresumida = novaDesc;
                            sessao.item.loja = novaLoja;
                            sessao.item.codlocalizacao = novoSetor;
                        }
                        adapter.notifyDataSetChanged();
                        atualizarContadorItens();
                        dialog.dismiss();
                    })
                    .setNegativeButton("Cancelar", null)
                    .show();
        });

        // Remover com confirmação
        btnRemover.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("Remover item")
                    .setMessage("Tem certeza que deseja remover esse item da lista?\n\nEsta ação não pode ser desfeita.")
                    .setPositiveButton("Remover", (d, w) -> {
                        itensSessao.remove(pos);
                        adapter.notifyDataSetChanged();
                        dialog.dismiss();
                        atualizarContadorItens();
                    })
                    .setNegativeButton("Cancelar", null)
                    .show();
        });

        btnCancelar.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }




    // Mesma função de antes pra tratar epc
    public static String formatarEPC(String epc) {
        if (epc == null) return "";
        String ultimos = epc.length() > 7 ? epc.substring(epc.length() - 7) : epc;
        return ultimos.replaceFirst("^0+(?!$)", "");
    }
}
