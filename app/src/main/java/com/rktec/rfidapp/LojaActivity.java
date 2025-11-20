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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Collections;
import java.util.Locale;

public class LojaActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_loja);

        List<ItemPlanilha> listaPlanilha = DadosGlobais.getInstance().getListaPlanilha();
        Set<String> lojasSet = new HashSet<>();
        for (ItemPlanilha it : listaPlanilha) {
            lojasSet.add(it.loja);
        }

        ImageButton back = findViewById(R.id.btnBack);
        back.setOnClickListener(v -> finish());

        // Lista "base" (todas as lojas)
        List<String> lojasOriginais = new ArrayList<>(lojasSet);
        Collections.sort(lojasOriginais);

        // Lista que realmente aparece na tela (filtrada)
        List<String> lojasFiltradas = new ArrayList<>(lojasOriginais);

        ListView lv = findViewById(R.id.listViewLojas);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_list_item_1,
                lojasFiltradas
        );
        lv.setAdapter(adapter);

        // Barra de busca
        EditText edtBusca = findViewById(R.id.editSearchLoja);
        edtBusca.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String filtro = s.toString().toLowerCase(Locale.ROOT).trim();

                lojasFiltradas.clear();

                if (filtro.isEmpty()) {
                    // Sem texto: mostra tudo
                    lojasFiltradas.addAll(lojasOriginais);
                } else {
                    // Contém em qualquer parte do nome da loja
                    for (String loja : lojasOriginais) {
                        if (loja.toLowerCase(Locale.ROOT).contains(filtro)) {
                            lojasFiltradas.add(loja);
                        }
                    }
                }

                adapter.notifyDataSetChanged();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Usa a lista filtrada no clique
        lv.setOnItemClickListener((p, v, pos, id) -> {
            String lojaSelecionada = lojasFiltradas.get(pos);
            DadosGlobais.getInstance().setLojaSelecionada(lojaSelecionada);
            startActivity(new Intent(this, SetorActivity.class));
            finish();
        });
    }
}
