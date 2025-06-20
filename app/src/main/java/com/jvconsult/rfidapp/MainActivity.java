package com.jvconsult.rfidapp;

import android.app.AlertDialog;
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

        Button btnTombar = findViewById(R.id.btnTombar); // botão de tombar item

        String permissao = getSharedPreferences("prefs", MODE_PRIVATE)
                .getString("usuario_permissao", "membro"); // valor padrão: membro

        if (!"adm".equals(permissao)) {
            btnTombar.setVisibility(View.GONE); // Esconde o botão se não for adm
        }

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
                    btnImportarPlanilha.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#FFB600")));
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
                    btnImportarSetor.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#FFB600")));
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

    public void onTombar(View v) {
        String permissao = getSharedPreferences("prefs", MODE_PRIVATE)
                .getString("usuario_permissao", "membro");

        if (!"adm".equals(permissao)) {
            Toast.makeText(this, "Você não tem permissão para tombar itens!", Toast.LENGTH_SHORT).show();
            return;
        }

        List<ItemPlanilha> planilha = DadosGlobais.getInstance().getListaPlanilha();
        if (planilha == null || planilha.isEmpty()) {
            Toast.makeText(this, "Importe a planilha primeiro!", Toast.LENGTH_SHORT).show();
            return;
        }
        startActivity(new Intent(this, TombamentoActivity.class));
    }



    public void onLogout(View v) {
        new AlertDialog.Builder(this)
                .setTitle("Sair do aplicativo")
                .setMessage("Tem certeza que deseja sair?\n\n"
                        + "Ao sair, é necessário importar novamente a planilha editada na próxima vez que fizer login.\n"
                        + "Se importar uma planilha antiga, você pode perder as alterações já feitas no inventário.\n\n"
                        + "Dica: Sempre use a planilha mais recente (inventario_editado.csv) após o logout.")
                .setPositiveButton("Sair", (dialog, which) -> {
                    getSharedPreferences("prefs", MODE_PRIVATE).edit().clear().apply(); // remove nome
                    DadosGlobais.getInstance().resetar(); // limpa dados do singleton
                    startActivity(new Intent(this, LoginActivity.class));
                    finish();
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }


}
