package com.rktec.rfidapp;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.EditText;
import android.text.Editable;
import android.text.TextWatcher;

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
            // Volta pra tela de lojas
            startActivity(new Intent(this, LojaActivity.class));
            finish();
        });

        DadosGlobais dados = DadosGlobais.getInstance();

        // Loja que o usuário escolheu na tela anterior (ex.: "001-CARPINA")
        String lojaSelecionada = dados.getLojaSelecionada();

        // Todos os setores importados do TXT
        List<SetorLocalizacao> todosSetores = dados.getListaSetores();

        // Agora filtramos só os setores da loja escolhida
        List<SetorLocalizacao> setoresFiltrados = filtrarPorLoja(todosSetores, lojaSelecionada);

        // Lista base com todos os nomes (originais)
        List<String> nomesOriginais = new ArrayList<>();
        for (SetorLocalizacao s : setoresFiltrados) {
            nomesOriginais.add(s.setor);
        }

        // Lista que é de fato mostrada na tela (filtrada)
        List<String> nomesFiltrados = new ArrayList<>(nomesOriginais);

        ListView lv = findViewById(R.id.listViewSetores);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_list_item_1,
                nomesFiltrados
        );
        lv.setAdapter(adapter);

        // Barra de busca
        EditText edtBusca = findViewById(R.id.editSearchSetor);
        edtBusca.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String filtro = s.toString().toLowerCase(Locale.ROOT).trim();

                nomesFiltrados.clear();

                if (filtro.isEmpty()) {
                    // Sem texto: mostra todos os setores da loja
                    nomesFiltrados.addAll(nomesOriginais);
                } else {
                    // Contém em qualquer parte do nome do setor
                    for (String nome : nomesOriginais) {
                        if (nome.toLowerCase(Locale.ROOT).contains(filtro)) {
                            nomesFiltrados.add(nome);
                        }
                    }
                }

                adapter.notifyDataSetChanged();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Ao clicar, pega o nome filtrado e localiza o objeto SetorLocalizacao correspondente
        lv.setOnItemClickListener((p, v, pos, id) -> {
            String nomeEscolhido = nomesFiltrados.get(pos);

            SetorLocalizacao escolhido = null;
            for (SetorLocalizacao s : setoresFiltrados) {
                if (s.setor.equals(nomeEscolhido)) {
                    escolhido = s;
                    break;
                }
            }

            if (escolhido != null) {
                dados.setSetorSelecionado(escolhido);
                startActivity(new Intent(this, LeituraActivity.class));
                finish();
            }
        });
    }

    /**
     * Filtra a lista de setores pela loja.
     * Se der algum problema em descobrir a loja, volta a lista inteira (pra não quebrar).
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

        // Se por algum motivo não achou nada, devolve tudo pra não ficar tela vazia
        if (resultado.isEmpty()) {
            resultado.addAll(todos);
        }

        return resultado;
    }

    /**
     * Converte o texto da loja selecionada no código numérico da loja.
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
}
