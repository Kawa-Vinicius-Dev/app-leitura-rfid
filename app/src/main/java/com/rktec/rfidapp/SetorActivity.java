package com.rktec.rfidapp;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class SetorActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setor);

        ImageButton btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> {
            startActivity(new Intent(this, LojaActivity.class));
            finish();
        });

        DadosGlobais dados = DadosGlobais.getInstance();

        // Loja escolhida na tela anterior (ex.: "001-CARPINA")
        String lojaSelecionada = dados.getLojaSelecionada();

        // Todos os setores importados do TXT
        List<SetorLocalizacao> todosSetores = dados.getListaSetores();

        // Normaliza loja para o código numérico (ex.: "001-CARPINA" -> "1")
        String codigoLoja = extrairCodigoLoja(lojaSelecionada);

        if (codigoLoja == null || codigoLoja.isEmpty()) {
            Toast.makeText(this, "Loja inválida. Selecione novamente.", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, LojaActivity.class));
            finish();
            return;
        }

        // Lista base: nomes únicos de setores APENAS da loja
        final List<String> nomesOriginais =
                ImportadorSetor.setoresUnicosPorLoja(todosSetores, codigoLoja);

        if (nomesOriginais == null || nomesOriginais.isEmpty()) {
            Toast.makeText(this, "Nenhum setor encontrado para esta loja.", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, LojaActivity.class));
            finish();
            return;
        }

        // Lista auxiliar em minúsculas (mesma ordem)
        final List<String> nomesOriginaisLower = new ArrayList<>(nomesOriginais.size());
        for (String nome : nomesOriginais) {
            nomesOriginaisLower.add(nome.toLowerCase(Locale.ROOT));
        }

        // Lista que aparece na tela (filtrada)
        final List<String> nomesFiltrados = new ArrayList<>(nomesOriginais);

        ListView lv = findViewById(R.id.listViewSetores);
        final ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_list_item_1,
                nomesFiltrados
        );
        lv.setAdapter(adapter);

        // Busca
        EditText edtBusca = findViewById(R.id.editSearchSetor);
        edtBusca.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String filtro = s.toString().toLowerCase(Locale.ROOT).trim();
                nomesFiltrados.clear();

                if (filtro.isEmpty()) {
                    nomesFiltrados.addAll(nomesOriginais);
                } else {
                    final int size = nomesOriginais.size();
                    for (int i = 0; i < size; i++) {
                        if (nomesOriginaisLower.get(i).contains(filtro)) {
                            nomesFiltrados.add(nomesOriginais.get(i));
                        }
                    }
                }

                adapter.notifyDataSetChanged();
            }

            @Override public void afterTextChanged(Editable s) {}
        });

        // Clique no setor
        lv.setOnItemClickListener((p, v, pos, id) -> {
            String nomeEscolhido = nomesFiltrados.get(pos);

            // Acha um SetorLocalizacao correspondente APENAS dessa loja (primeira ocorrência)
            SetorLocalizacao escolhido = null;
            if (todosSetores != null) {
                for (SetorLocalizacao s : todosSetores) {
                    if (s == null) continue;
                    if (codigoLoja.equals(s.loja) && nomeEscolhido.equals(s.setor)) {
                        escolhido = s;
                        break;
                    }
                }
            }

            if (escolhido != null) {
                dados.setSetorSelecionado(escolhido);
                startActivity(new Intent(this, LeituraActivity.class));
                finish();
            } else {
                Toast.makeText(this, "Setor inválido. Selecione novamente.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Converte o texto da loja selecionada no código numérico da loja.
     *
     * Exemplos:
     *  "001-CARPINA"   -> "1"
     *  "002-VIT.SA"    -> "2"
     *  "500-MATRIZ"    -> "500"
     *  "1"             -> "1"
     */
    private String extrairCodigoLoja(String lojaSelecionada) {
        if (lojaSelecionada == null) return null;
        String s = lojaSelecionada.trim();
        if (s.isEmpty()) return null;

        // pega tudo antes do primeiro hífen
        int idx = s.indexOf('-');
        String numero = (idx > 0) ? s.substring(0, idx).trim() : s;

        if (numero.isEmpty()) return null;

        // mantém só dígitos
        StringBuilder digits = new StringBuilder(numero.length());
        for (int i = 0; i < numero.length(); i++) {
            char ch = numero.charAt(i);
            if (ch >= '0' && ch <= '9') digits.append(ch);
        }

        if (digits.length() == 0) return null;

        // remove zeros à esquerda ("001" -> "1")
        int start = 0;
        int len = digits.length();
        while (start < len - 1 && digits.charAt(start) == '0') start++;

        return digits.substring(start);
    }
}
