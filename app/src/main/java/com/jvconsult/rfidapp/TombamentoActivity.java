package com.jvconsult.rfidapp;

import android.os.Bundle;
import android.widget.*;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class TombamentoActivity extends AppCompatActivity {

    private EditText edtSqBem, edtCodGrupo, edtNrObem, edtNrOincorp, edtDescResumida,
            edtDescDetalhada, edtQtdBem, edtNroPlaqueta, edtNroSerieBem, edtModeloBem;
    private Spinner spinnerLoja, spinnerSetor;
    private Button btnTombar;
    private ImageButton btnFechar;

    private List<String> listaLojas = new ArrayList<>();
    private Map<String, List<String>> mapSetoresPorLoja = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tombamento);

        // Verifica se as DUAS planilhas foram importadas
        List<ItemPlanilha> listaPlanilha = DadosGlobais.getInstance().getListaPlanilha();
        List<SetorLocalizacao> listaSetores = DadosGlobais.getInstance().getListaSetores();
        if (listaPlanilha == null || listaPlanilha.isEmpty()
                || listaSetores == null || listaSetores.isEmpty()) {
            Toast.makeText(this,
                    "Importe as planilhas de INVENTÁRIO e SETORES antes de tombar um item!",
                    Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        // Inicialização dos campos
        spinnerLoja        = findViewById(R.id.spinnerLoja);
        spinnerSetor       = findViewById(R.id.spinnerSetor);
        edtSqBem           = findViewById(R.id.edtSqBem);
        edtCodGrupo        = findViewById(R.id.edtCodGrupo);
        edtNrObem          = findViewById(R.id.edtNrObem);
        edtNrOincorp       = findViewById(R.id.edtNrOincorp);
        edtDescResumida    = findViewById(R.id.edtDescResumida);
        edtDescDetalhada   = findViewById(R.id.edtDescDetalhada);
        edtQtdBem          = findViewById(R.id.edtQtdBem);
        edtNroPlaqueta     = findViewById(R.id.edtNroPlaqueta);
        edtNroSerieBem     = findViewById(R.id.edtNroSerieBem);
        edtModeloBem       = findViewById(R.id.edtModeloBem);
        btnTombar          = findViewById(R.id.btnTombar);
        btnFechar          = findViewById(R.id.btnFechar);

        // Preenche Spinners
        carregarLojasESetores(listaPlanilha);

        ArrayAdapter<String> lojaAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, listaLojas);
        lojaAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerLoja.setAdapter(lojaAdapter);

        spinnerLoja.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String lojaSelecionada = listaLojas.get(position);
                List<String> setores = mapSetoresPorLoja.get(lojaSelecionada);
                if (setores == null) setores = new ArrayList<>();
                ArrayAdapter<String> setorAdapter = new ArrayAdapter<>(TombamentoActivity.this, android.R.layout.simple_spinner_item, setores);
                setorAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                spinnerSetor.setAdapter(setorAdapter);
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });

        // Botão Tombar Item
        btnTombar.setOnClickListener(v -> {
            // Validação
            if (listaPlanilha == null || listaPlanilha.isEmpty()) {
                Toast.makeText(this, "Importe a planilha de inventário antes de tombar um item!", Toast.LENGTH_LONG).show();
                return;
            }
            String loja = (String) spinnerLoja.getSelectedItem();
            String setor = (String) spinnerSetor.getSelectedItem();

            String sqBem         = edtSqBem.getText().toString().trim();
            String codGrupo      = edtCodGrupo.getText().toString().trim();
            String codLocal      = setor != null ? setor.trim() : "";
            String nroBem        = edtNrObem.getText().toString().trim();
            String nroIncorp     = edtNrOincorp.getText().toString().trim();
            String descResumida  = edtDescResumida.getText().toString().trim();
            String descDetalhada = edtDescDetalhada.getText().toString().trim();
            String qtdBem        = edtQtdBem.getText().toString().trim();
            String nroPlaqueta   = edtNroPlaqueta.getText().toString().trim();
            String nroSerieBem   = edtNroSerieBem.getText().toString().trim();
            String modeloBem     = edtModeloBem.getText().toString().trim();

            if (loja == null || loja.isEmpty() || codLocal.isEmpty() || nroPlaqueta.isEmpty()) {
                Toast.makeText(this, "Preencha Loja, Setor e Plaqueta!", Toast.LENGTH_SHORT).show();
                return;
            }

            // Adiciona item
            ItemPlanilha item = new ItemPlanilha(
                    loja,              // pega do spinner
                    sqBem,             // edtSqBem
                    codGrupo,          // edtCodGrupo
                    codLocal,          // do setor selecionado
                    nroBem,            // edtNrObem
                    nroIncorp,         // edtNrOincorp
                    descResumida,      // edtDescResumida
                    descDetalhada,     // edtDescDetalhada
                    qtdBem,            // edtQtdBem
                    nroPlaqueta,       // edtNroPlaqueta
                    nroSerieBem,       // edtNroSerieBem
                    modeloBem          // edtModeloBem
            );


            adicionarItemNoInventario(item);

            adicionarNoTombLog(item);

            Toast.makeText(this, "Item tombado com sucesso!", Toast.LENGTH_SHORT).show();
            limparCampos();
        });

        // Botão Fechar (X)
        btnFechar.setOnClickListener(v -> finish());
    }

    // Preenche listaLojas e mapSetoresPorLoja
    private void carregarLojasESetores(List<ItemPlanilha> listaPlanilha) {
        Set<String> lojasSet = new HashSet<>();
        for (ItemPlanilha item : listaPlanilha) {
            lojasSet.add(item.loja);
        }
        listaLojas.clear();
        listaLojas.addAll(lojasSet);
        Collections.sort(listaLojas);

        // Carrega os nomes dos setores, não o código!
        mapSetoresPorLoja.clear();
        List<SetorLocalizacao> listaSetores = DadosGlobais.getInstance().getListaSetores();
        if (listaSetores != null) {
            for (String loja : listaLojas) {
                List<String> setoresDaLoja = new ArrayList<>();
                for (SetorLocalizacao setorLoc : listaSetores) {
                    if (setorLoc.setor != null && !setorLoc.setor.isEmpty()) {
                        // Só o nome!
                        setoresDaLoja.add(setorLoc.setor);
                    }
                }
                Collections.sort(setoresDaLoja);
                mapSetoresPorLoja.put(loja, setoresDaLoja);
            }
        }
    }

    // Limpa todos os campos menos Loja/Setor (pode resetar se preferir)
    private void limparCampos() {
        edtSqBem.setText("");
        edtCodGrupo.setText("");
        edtNrObem.setText("");
        edtNrOincorp.setText("");
        edtDescResumida.setText("");
        edtDescDetalhada.setText("");
        edtQtdBem.setText("");
        edtNroPlaqueta.setText("");
        edtNroSerieBem.setText("");
        edtModeloBem.setText("");
        // Loja e setor mantêm os últimos selecionados pra facilitar
    }

    private void adicionarItemNoInventario(ItemPlanilha item) {
        File pasta = getExternalFilesDir(null);
        File arquivo = new File(pasta, "inventario_editado.csv");
        List<String> linhas = new ArrayList<>();

        // Lê todas as linhas
        try (BufferedReader br = new BufferedReader(new FileReader(arquivo))) {
            String linha;
            while ((linha = br.readLine()) != null) {
                linhas.add(linha);
            }
        } catch (Exception e) { /* ignora se não existe */ }

        // Cabeçalho se necessário
        if (linhas.isEmpty()) {
            linhas.add("loja,sqbem,codgrupo,codlocalizacao,nrobem,nroincorp,descresumida,descdetalhada,qtdbem,nroplaqueta,nroseriebem,modelobem");
        }

        // Monta a linha nova
        String novaLinha = item.loja + "," + item.sqbem + "," + item.codgrupo + "," + item.codlocalizacao + "," +
                item.nrobem + "," + item.nroincorp + "," + item.descresumida + "," + item.descdetalhada + "," +
                item.qtdbem + "," + item.nroplaqueta + "," + item.nroseriebem + "," + item.modelobem;

        // Procura a primeira linha da loja (depois do cabeçalho)
        int posInserir = linhas.size(); // padrão: final
        for (int i = 1; i < linhas.size(); i++) { // começa depois do cabeçalho
            String[] colunas = linhas.get(i).split(",");
            if (colunas.length > 0 && colunas[0].equals(item.loja)) {
                posInserir = i;
                break;
            }
        }
        linhas.add(posInserir, novaLinha); // insere no local certo

        // Escreve de volta
        try (FileWriter writer = new FileWriter(arquivo, false)) {
            for (String l : linhas) writer.write(l + "\n");
        } catch (Exception e) {
            Toast.makeText(this, "Erro ao salvar inventário!", Toast.LENGTH_SHORT).show();
        }
    }



    private void adicionarNoTombLog(ItemPlanilha item) {
        String usuario = DadosGlobais.getInstance().getUsuario();
        String data = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
        File pasta = getExternalFilesDir(null);
        File arquivo = new File(pasta, "TOMBLOG.csv");
        try (FileWriter writer = new FileWriter(arquivo, true)) {
            writer.write(usuario + "," + data + "," + item.nroplaqueta + "," + item.loja + "," + item.descresumida + "\n");
        } catch (Exception e) {
            Toast.makeText(this, "Erro ao registrar TOMBLOG!", Toast.LENGTH_SHORT).show();
        }
    }
}
