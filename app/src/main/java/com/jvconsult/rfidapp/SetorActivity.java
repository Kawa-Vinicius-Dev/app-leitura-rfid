package com.jvconsult.rfidapp;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import androidx.appcompat.app.AppCompatActivity;
import java.util.ArrayList;
import java.util.List;

public class SetorActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setor);

        List<SetorLocalizacao> setores = DadosGlobais.getInstance().getListaSetores();
        List<String> nomes = new ArrayList<>();
        for (SetorLocalizacao s : setores) nomes.add(s.setor);

        ListView lv = findViewById(R.id.listViewSetores);
        lv.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, nomes));

        lv.setOnItemClickListener((p,v,pos,id)->{
            // Salva o setor e vai pra Leitura
            DadosGlobais.getInstance().setSetorSelecionado(setores.get(pos));
            startActivity(new Intent(this, LeituraActivity.class));
            finish();
        });
    }
}
