package com.jvconsult.rfidapp;

import android.os.Bundle;
import android.view.KeyEvent;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.app.AlertDialog;

import androidx.appcompat.app.AppCompatActivity;

import com.pda.rfid.EPCModel;
import com.pda.rfid.IAsynchronousMessage;

import java.util.ArrayList;
import java.util.List;

public class LeituraActivity extends AppCompatActivity implements IAsynchronousMessage {

    private LeitorRFID leitorRFID;
    private String lojaSelecionada;
    private SetorLocalizacao setorSelecionado;
    private List<ItemPlanilha> listaPlanilha;
    private List<SetorLocalizacao> listaSetores;
    private List<ItemPlanilha> itensFiltrados = new ArrayList<>();
    private ArrayAdapter<String> adapter;
    private ArrayList<String> itensLidos = new ArrayList<>();
    private boolean lendo = false;

    private TextView tvMsgLeitura;
    private String usuario;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_leitura_rfid);

        setorSelecionado = DadosGlobais.getInstance().getSetorSelecionado();
        if (setorSelecionado == null) {
            Toast.makeText(this, "Nenhum setor selecionado! Voltando...", Toast.LENGTH_LONG).show();
            finish(); // ou volta pra tela de setor
            return;
        }



        usuario = DadosGlobais.getInstance().getUsuario();
        if (usuario == null)  // fallback (não deveria acontecer)
            usuario = getSharedPreferences("prefs", MODE_PRIVATE).getString("usuario_nome", "Usuário");


        listaPlanilha = DadosGlobais.getInstance().getListaPlanilha();
        listaSetores = DadosGlobais.getInstance().getListaSetores();

        TextView tvLoja = findViewById(R.id.tvLojaSelecionada);
        TextView tvSetor = findViewById(R.id.tvSetorSelecionado);
        tvMsgLeitura = findViewById(R.id.tvMsgLeitura);
        ListView listView = findViewById(R.id.listaItensLidos);

        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, itensLidos);
        listView.setAdapter(adapter);

        lojaSelecionada = DadosGlobais.getInstance().getLojaSelecionada();
        setorSelecionado = DadosGlobais.getInstance().getSetorSelecionado();

        tvLoja.setText("Loja: " + lojaSelecionada);
        tvSetor.setText("Setor: " + setorSelecionado.setor);

        itensFiltrados.clear();
        for (ItemPlanilha item : listaPlanilha) {
            if (item.loja.equals(lojaSelecionada)) {
                itensFiltrados.add(item);
            }
        }

        Button btnFinalizar = findViewById(R.id.btnFinalizar);
        btnFinalizar.setOnClickListener(v -> finalizarEExportar());
    }

    // CHAVE DO GATILHO
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == 139 && !lendo && setorSelecionado != null) {
            if (leitorRFID == null)
                leitorRFID = new LeitorRFID(this, this);
            lendo = leitorRFID.iniciarLeitura();
            tvMsgLeitura.setText("Leitura iniciada. Aproxime as etiquetas.");
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (keyCode == 139 && lendo && leitorRFID != null) {
            leitorRFID.pararLeitura();
            lendo = false;
            tvMsgLeitura.setText("Leitura pausada! Aperte o gatilho para ler novamente.");
            return true;
        }
        return super.onKeyUp(keyCode, event);
    }

    @Override
    public void OutPutEPC(EPCModel model) {
        runOnUiThread(() -> {
            String epc = model._EPC;
            String epcFormatado = formatarEPC(epc);

            ItemPlanilha encontrado = null;
            for (ItemPlanilha item : itensFiltrados) {
                if (item.nroplaqueta.equals(epc)) {
                    encontrado = item;
                    break;
                }
            }

            if (encontrado != null) {
                if (encontrado.codlocalizacao == null || encontrado.codlocalizacao.isEmpty()) {
                    encontrado.codlocalizacao = setorSelecionado.codlocalizacao;
                    tvMsgLeitura.setText("Item " + epcFormatado + " sem setor definido. Atribuído para " + setorSelecionado.setor);
                    LogHelper.registrarAcao(this, usuario, "Item " + epcFormatado + " estava sem setor. Setado para " + setorSelecionado.setor);
                } else if (setorSelecionado.codlocalizacao.equals(encontrado.codlocalizacao)) {
                    tvMsgLeitura.setText("Item " + epcFormatado + " já estava no setor " + setorSelecionado.setor);
                    LogHelper.registrarAcao(this, usuario, "Item " + epcFormatado + " já estava no setor " + setorSelecionado.setor);
                } else {
                    String setorAntigo = nomeSetorPorCodigo(encontrado.codlocalizacao);
                    encontrado.codlocalizacao = setorSelecionado.codlocalizacao;
                    tvMsgLeitura.setText("Item " + epcFormatado + " movido do setor " + setorAntigo + " para " + setorSelecionado.setor);
                    LogHelper.registrarAcao(this, usuario, "Item " + epcFormatado + " movido de " + setorAntigo + " para " + setorSelecionado.setor);
                }
            } else {
                tvMsgLeitura.setText("Item " + epcFormatado + " não pertence a esta loja!");
                LogHelper.registrarAcao(this, usuario, "Item " + epcFormatado + " lido, mas não encontrado na loja " + lojaSelecionada);
                mostrarBotaoProcurar(epc, epcFormatado);
            }


            if (!itensLidos.contains(epcFormatado)) {
                itensLidos.add(epcFormatado);
                adapter.notifyDataSetChanged();
            }
        });
    }

    private void mostrarBotaoProcurar(String epc, String epcFormatado) {
        new AlertDialog.Builder(this)
                .setTitle("Item não encontrado na loja")
                .setMessage("O item " + epcFormatado + " não está nesta loja. Deseja procurar em todas as lojas?")
                .setPositiveButton("Procurar", (d, w) -> procurarEmTodasLojas(epc, epcFormatado))
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void procurarEmTodasLojas(String epc, String epcFormatado) {
        ItemPlanilha achado = null;
        for (ItemPlanilha item : listaPlanilha) {
            if (item.nroplaqueta.equals(epc)) {
                achado = item;
                break;
            }
        }
        if (achado != null) {
            String setorNome = nomeSetorPorCodigo(achado.codlocalizacao);
            tvMsgLeitura.setText("Item " + epcFormatado + " pertence à loja " + achado.loja + ", setor " + setorNome);
            LogHelper.registrarAcao(this, usuario, "Item " + epcFormatado + " pertence à loja " + achado.loja + ", setor " + setorNome + ", lido na loja " + lojaSelecionada);
        } else {
            tvMsgLeitura.setText("Item " + epcFormatado + " não cadastrado em nenhuma loja.");
            LogHelper.registrarAcao(this, usuario, "Item " + epcFormatado + " lido, mas não cadastrado em nenhuma loja");
        }
    }

    private String nomeSetorPorCodigo(String cod) {
        if (cod == null) return "";
        for (SetorLocalizacao s : listaSetores) {
            if (s.codlocalizacao.equals(cod))
                return s.setor;
        }
        return cod;
    }

    public static String formatarEPC(String epc) {
        if (epc == null) return "";
        String ultimos = epc.length() > 7 ? epc.substring(epc.length() - 7) : epc;
        return ultimos.replaceFirst("^0+(?!$)", "");
    }

    private void finalizarEExportar() {
        List<String> esperados = new ArrayList<>();
        for (ItemPlanilha item : itensFiltrados) {
            if (item.codlocalizacao.equals(setorSelecionado.codlocalizacao))
                esperados.add(formatarEPC(item.nroplaqueta));
        }
        List<String> lidos = new ArrayList<>(itensLidos);

        List<String> naoEncontrados = new ArrayList<>(esperados);
        naoEncontrados.removeAll(lidos);

        LogHelper.registrarAcao(this, usuario, "Itens esperados no setor " + setorSelecionado.setor + ": " + esperados);
        LogHelper.registrarAcao(this, usuario, "Itens lidos no setor " + setorSelecionado.setor + ": " + lidos);
        for (String n : naoEncontrados) {
            LogHelper.registrarAcao(this, usuario, "Item " + n + " não foi encontrado/lido no setor " + setorSelecionado.setor);
        }

        String caminho = ExportadorPlanilha.exportarCSV(this, listaPlanilha).getAbsolutePath();
        Toast.makeText(this, "Exportado para: " + caminho, Toast.LENGTH_LONG).show();

        // Volta para tela de seleção de setor
        DadosGlobais.getInstance().setSetorSelecionado(null);
        recreate();
    }

    @Override
    protected void onDestroy() {
        if (leitorRFID != null) leitorRFID.fechar();
        super.onDestroy();
    }
}
