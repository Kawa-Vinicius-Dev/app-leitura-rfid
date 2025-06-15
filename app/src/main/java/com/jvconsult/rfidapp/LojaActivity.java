package com.jvconsult.rfidapp;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import androidx.appcompat.app.AppCompatActivity;
import java.util.*;

public class LojaActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_loja);

        List<ItemPlanilha> listaPlanilha = DadosGlobais.getInstance().getListaPlanilha();
        Set<String> lojasSet = new HashSet<>();
        for (ItemPlanilha it : listaPlanilha) lojasSet.add(it.loja);

        List<String> lojas = new ArrayList<>(lojasSet);
        Collections.sort(lojas);

        ListView lv = findViewById(R.id.listViewLojas);
        lv.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, lojas));

        lv.setOnItemClickListener((p,v,pos,id)->{
            DadosGlobais.getInstance().setLojaSelecionada(lojas.get(pos));
            startActivity(new Intent(this, SetorActivity.class));
            finish();
        });
    }
}
