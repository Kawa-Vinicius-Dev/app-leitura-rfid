package com.jvconsult.rfidapp;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private ActivityResultLauncher<String> importarPlanilhaLauncher;
    private ActivityResultLauncher<String> importarSetorLauncher;

    private List<ItemPlanilha> listaPlanilha = new ArrayList<>();
    private List<SetorLocalizacao> listaSetores = new ArrayList<>();

    private Button btnImportarPlanilha, btnImportarSetor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String nome = getSharedPreferences("prefs", MODE_PRIVATE)
                .getString("usuario_nome", null);
        if (nome == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        DadosGlobais.getInstance().setUsuario(nome);

        setContentView(R.layout.activity_main);



        btnImportarPlanilha = findViewById(R.id.btnImportarPlanilha);
        btnImportarSetor    = findViewById(R.id.btnImportarSetor);
        Button btnLogout = findViewById(R.id.btnLogout);
        btnLogout.setOnClickListener(this::onLogout);


        importarPlanilhaLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri == null) return;
                    listaPlanilha = ImportadorPlanilha.importar(this, uri);
                    DadosGlobais.getInstance().setListaPlanilha(listaPlanilha);

                    btnImportarPlanilha.setText("Planilha OK");
                    btnImportarPlanilha.setEnabled(false);
                    btnImportarPlanilha.setTextColor(Color.WHITE);
                    btnImportarPlanilha.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#43A047")));
                    btnImportarPlanilha.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_check,0,0,0);

                    Toast.makeText(this,"Importados "+listaPlanilha.size()+" itens!",Toast.LENGTH_SHORT).show();
                });

        importarSetorLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri == null) return;
                    listaSetores = ImportadorSetor.importar(this, uri);
                    DadosGlobais.getInstance().setListaSetores(listaSetores);

                    btnImportarSetor.setText("Setores OK");
                    btnImportarSetor.setEnabled(false);
                    btnImportarSetor.setTextColor(Color.WHITE);
                    btnImportarSetor.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#43A047")));
                    btnImportarSetor.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_check,0,0,0);

                    Toast.makeText(this,"Importados "+listaSetores.size()+" setores!",Toast.LENGTH_SHORT).show();
                });
    }

    public void onImportarPlanilha(View v){ importarPlanilhaLauncher.launch("text/*"); }
    public void onImportarSetor   (View v){ importarSetorLauncher.launch("text/*"); }

    public void onEscolherLoja(View v){
        if (listaPlanilha.isEmpty() || listaSetores.isEmpty()){
            Toast.makeText(this,"Importe planilha e setores primeiro!",Toast.LENGTH_SHORT).show();
            return;
        }
        startActivity(new Intent(this, LojaActivity.class));
    }

    public void onLogout(View v) {
        getSharedPreferences("prefs", MODE_PRIVATE).edit().clear().apply(); // remove nome
        DadosGlobais.getInstance().resetar(); // limpa dados do singleton
        startActivity(new Intent(this, LoginActivity.class));
        finish();
    }

}
