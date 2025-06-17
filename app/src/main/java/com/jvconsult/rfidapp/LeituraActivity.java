package com.jvconsult.rfidapp;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.widget.*;
import android.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.pda.rfid.EPCModel;
import com.pda.rfid.IAsynchronousMessage;
import java.util.*;

public class LeituraActivity extends AppCompatActivity implements IAsynchronousMessage {

    private ArrayList<String> itensExibidos = new ArrayList<>();
    private LeitorRFID leitorRFID;
    private String lojaSelecionada, usuario;
    private SetorLocalizacao setorSelecionado;
    private List<ItemPlanilha> listaPlanilha, itensFiltrados;
    private List<SetorLocalizacao> listaSetores;
    private ArrayAdapter<String> adapter;
    private ArrayList<String> epcsNaoEncontrados = new ArrayList<>();
    private HashSet<String> epcsJaProcessados = new HashSet<>();
    private boolean lendo = false;
    private TextView tvMsgLeitura;
    private HashMap<String, ItemPlanilha> mapPlaquetasGlobal;

    private ArrayList<String> epcsLidosNaSessao = new ArrayList<>();
    private TextView tvContadorItens;
    private int potenciaAtual = 20;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_leitura_rfid);

        tvContadorItens = findViewById(R.id.tvContadorItens);

        setorSelecionado = DadosGlobais.getInstance().getSetorSelecionado();
        if (setorSelecionado == null) {
            Toast.makeText(this, "Nenhum setor selecionado! Voltando...", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        listaPlanilha = DadosGlobais.getInstance().getListaPlanilha();
        listaSetores = DadosGlobais.getInstance().getListaSetores();
        lojaSelecionada = DadosGlobais.getInstance().getLojaSelecionada();
        usuario = DadosGlobais.getInstance().getUsuario();
        if (usuario == null)
            usuario = getSharedPreferences("prefs", MODE_PRIVATE).getString("usuario_nome", "Usuário");

        construirMapaPlaquetasGlobal(listaPlanilha);

        itensFiltrados = new ArrayList<>();
        for (ItemPlanilha item : listaPlanilha)
            if (item.loja.equals(lojaSelecionada))
                itensFiltrados.add(item);

        tvMsgLeitura = findViewById(R.id.tvMsgLeitura);
        ((TextView) findViewById(R.id.tvLojaSelecionada)).setText("Loja: " + lojaSelecionada);
        ((TextView) findViewById(R.id.tvSetorSelecionado)).setText("Setor: " + setorSelecionado.setor);

        ListView listView = findViewById(R.id.listaItensLidos);
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, itensExibidos);
        listView.setAdapter(adapter);

        findViewById(R.id.btnFinalizar).setOnClickListener(v -> finalizarEExportar());

        // Agora sim pode chamar, pois tudo foi carregado!
        atualizarContadorItens();

        SeekBar sbPotencia = findViewById(R.id.sbPotencia);
        TextView tvPotencia = findViewById(R.id.tvPotencia);

        // Mostra o valor inicial
        tvPotencia.setText("Potência: " + potenciaAtual);

        sbPotencia.setProgress(potenciaAtual);

        sbPotencia.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                potenciaAtual = progress;
                tvPotencia.setText("Potência: " + potenciaAtual);
                // Se já tem leitor inicializado, já muda a potência em tempo real
                if (leitorRFID != null) {
                    leitorRFID.setPotencia(potenciaAtual);
                }
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
        for (String plaq : mapPlaquetasGlobal.keySet()) {
            Log.d("PlaquetaImportada", plaq);
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == 139 && !lendo && setorSelecionado != null) {
            if (leitorRFID == null)
                leitorRFID = new LeitorRFID(this, this);
            leitorRFID.setPotencia(potenciaAtual);
            lendo = leitorRFID.iniciarLeitura();
            tvMsgLeitura.setText("Leitura iniciada. Aproxime as etiquetas.");
            epcsJaProcessados.clear();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    private void atualizarContadorItens() {
        int lidos = epcsLidosNaSessao.size(); // ou epcsJaProcessados.size(), se preferir
        int total = 0;
        for (ItemPlanilha item : listaPlanilha) {
            if (item.loja.equals(lojaSelecionada)) {
                total++;
            }
        }
        tvContadorItens.setText("Itens lidos: " + lidos + " / " + total);
    }


    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (keyCode == 139 && lendo && leitorRFID != null) {
            leitorRFID.pararLeitura();
            lendo = false;
            tvMsgLeitura.setText("Leitura pausada! Aperte o gatilho para ler novamente.");
            if (!epcsNaoEncontrados.isEmpty())
                mostrarDialogNaoEncontrados();
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
        if (!epcsLidosNaSessao.contains(epcLimpo)) {
            epcsLidosNaSessao.add(epcLimpo);
        }


        ItemPlanilha item = encontrarItemPorEPC(epc);
        if (item != null && item.loja.equals(lojaSelecionada)) {
            if (!itensExibidos.contains(item.descresumida)) {
                itensExibidos.add(item.descresumida);
                adapter.notifyDataSetChanged();
            }
            atualizarLocalizacaoSeNecessario(item);
            tvMsgLeitura.setText(msgStatusItem(item, item.descresumida));
        } else {
            String infoNaoEncontrado = "Não encontrado: " + formatarEPC(epc);
            if (!itensExibidos.contains(infoNaoEncontrado)) {
                itensExibidos.add(infoNaoEncontrado);
                adapter.notifyDataSetChanged();
            }
            if (!epcsNaoEncontrados.contains(epc)) epcsNaoEncontrados.add(epc);
            tvMsgLeitura.setText("Item " + formatarEPC(epc) + " não pertence a esta loja!");
        }
        Log.d("PlaquetaLida", "EPC lido: [" + epc + "] Limpo: [" + epc.trim().replaceFirst("^0+(?!$)", "") + "]");
    }

    private ItemPlanilha encontrarItemPorEPC(String epc) {
        if (mapPlaquetasGlobal == null || epc == null) return null;
        String epcLido = epc.trim().replaceFirst("^0+(?!$)", "");
        return mapPlaquetasGlobal.get(epcLido);
    }

    private void atualizarLocalizacaoSeNecessario(ItemPlanilha item) {
        if (!setorSelecionado.codlocalizacao.equals(item.codlocalizacao)) {
            String setorAntigo = item.codlocalizacao;
            item.codlocalizacao = setorSelecionado.codlocalizacao;
            Log.d("LeituraActivity", "Item " + item.descresumida +
                    " movido do setor " + setorAntigo + " para " + setorSelecionado.codlocalizacao);
        }
    }

    private String msgStatusItem(ItemPlanilha item, String info) {
        if (item.codlocalizacao == null || item.codlocalizacao.isEmpty())
            return "Item " + info + " sem setor definido. Atribuído para " + setorSelecionado.setor;
        if (setorSelecionado.codlocalizacao.equals(item.codlocalizacao))
            return "Item " + info + " já estava no setor " + setorSelecionado.setor;
        return "Item " + info + " movido para " + setorSelecionado.setor;
    }

    private void mostrarDialogNaoEncontrados() {
        StringBuilder sb = new StringBuilder();
        for (String epc : epcsNaoEncontrados)
            sb.append(formatarEPC(epc)).append("\n");
        new AlertDialog.Builder(this)
                .setTitle("EPCs não encontrados")
                .setMessage("Os seguintes EPCs não foram encontrados:\n\n" + sb)
                .setPositiveButton("Fechar", (d, w) -> epcsNaoEncontrados.clear())
                .show();
    }

    public static String formatarEPC(String epc) {
        if (epc == null) return "";
        String ultimos = epc.length() > 7 ? epc.substring(epc.length() - 7) : epc;
        return ultimos.replaceFirst("^0+(?!$)", "");
    }

    private void finalizarEExportar() {
        List<ItemPlanilha> itensMovidos = new ArrayList<>();
        List<ItemPlanilha> itensOutrasLojas = new ArrayList<>();
        List<String> epcsNaoCadastrados = new ArrayList<>();

        for (String epcLido : epcsLidosNaSessao) {
            boolean encontrou = false;
            for (ItemPlanilha item : listaPlanilha) {
                String plaqLimpo = item.nroplaqueta != null ? item.nroplaqueta.trim().replaceFirst("^0+(?!$)", "") : "";
                if (plaqLimpo.equals(epcLido)) {
                    encontrou = true;
                    if (item.loja.equals(lojaSelecionada)) {
                        // Se é da loja selecionada, entra como "movido"
                        if (!item.codlocalizacao.equals(setorSelecionado.codlocalizacao)) {
                            // Só muda o setor se for diferente
                            item.codlocalizacao = setorSelecionado.codlocalizacao;
                        }
                        // Adiciona ao log de movidos
                        itensMovidos.add(new ItemPlanilha(
                                item.loja, item.sqbem, item.codgrupo, item.codlocalizacao,
                                item.nrobem, item.nroincorp, item.descresumida, item.descdetalhada,
                                item.qtdbem, item.nroplaqueta, item.nroseriebem, item.modelobem
                        ));
                    } else {
                        // Pertence a outra loja
                        itensOutrasLojas.add(new ItemPlanilha(
                                item.loja, item.sqbem, item.codgrupo, item.codlocalizacao,
                                item.nrobem, item.nroincorp, item.descresumida, item.descdetalhada,
                                item.qtdbem, item.nroplaqueta, item.nroseriebem, item.modelobem
                        ));
                    }
                    break;
                }
            }
            if (!encontrou) {
                epcsNaoCadastrados.add(epcLido);
            }
        }

        LogHelper.logRelatorio(this, usuario, itensMovidos, itensOutrasLojas, epcsNaoCadastrados);
        String caminho = ExportadorPlanilha.exportarCSV(this, listaPlanilha).getAbsolutePath();
        Toast.makeText(this, "Exportado para: " + caminho, Toast.LENGTH_LONG).show();

        // Volta pra tela de setores
        Intent intent = new Intent(this, SetorActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
    }



    @Override
    protected void onDestroy() {
        if (leitorRFID != null) leitorRFID.fechar();
        super.onDestroy();
    }
}
