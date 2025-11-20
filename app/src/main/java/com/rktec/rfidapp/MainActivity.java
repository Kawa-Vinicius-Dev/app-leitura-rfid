package com.rktec.rfidapp;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.cardview.widget.CardView;
import android.widget.ImageView;
import androidx.cardview.widget.CardView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private ActivityResultLauncher<String> importarPlanilhaLauncher;
    private ActivityResultLauncher<String> importarSetorLauncher;

    private List<ItemPlanilha> listaPlanilha = new ArrayList<>();
    private List<SetorLocalizacao> listaSetores = new ArrayList<>();

    // ANTES
// private Button btnImportarPlanilha, btnImportarSetor, btnGerenciarUsuarios, btnLogout;

    // DEPOIS
    private CardView btnImportarPlanilha, btnImportarSetor, btnGerenciarUsuarios;
    private Button btnLogout;
    private ImageButton btnLimparPlanilha, btnLimparSetor;

    // NOVOS TextViews para mudar o texto dos cards
    private TextView tvStatusPlanilha, tvStatusSetor;

    private ImageView imgIconPlanilha, imgIconSetor;

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

        btnImportarPlanilha   = findViewById(R.id.btnImportarPlanilha);
        btnImportarSetor      = findViewById(R.id.btnImportarSetor);
        btnLogout             = findViewById(R.id.btnLogout);
        btnGerenciarUsuarios  = findViewById(R.id.btnGerenciarUsuarios);
        btnLimparPlanilha     = findViewById(R.id.btnLimparPlanilha);
        btnLimparSetor        = findViewById(R.id.btnLimparSetor);

// novos:
        tvStatusPlanilha = findViewById(R.id.tvStatusPlanilha);
        tvStatusSetor    = findViewById(R.id.tvStatusSetor);

        imgIconPlanilha       = findViewById(R.id.imgIconPlanilha);
        imgIconSetor          = findViewById(R.id.imgIconSetor);


        btnLogout.setOnClickListener(this::onLogout);

        UsuarioDAO dao = new UsuarioDAO(this);
        String permissao = dao.getPermissaoUsuario(nome);
        if ("adm".equals(permissao)) {
            btnGerenciarUsuarios.setVisibility(View.VISIBLE);
            btnGerenciarUsuarios.setOnClickListener(v ->
                    startActivity(new Intent(this, GerenciarUsuariosActivity.class)));
        } else {
            btnGerenciarUsuarios.setVisibility(View.GONE);
        }

        // Lixeiras
        btnLimparPlanilha.setOnClickListener(v -> mostrarDialogoDeLimpeza("planilha"));
        btnLimparSetor.setOnClickListener(v -> mostrarDialogoDeLimpeza("setores"));

        // Importar PLANILHA principal
        importarPlanilhaLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri == null) return;

                    listaPlanilha = ImportadorPlanilha.importar(this, uri);
                    DadosGlobais.getInstance().setListaPlanilha(listaPlanilha);

                    if (listaPlanilha == null || listaPlanilha.isEmpty()) {
                        Toast.makeText(this, "Erro: Nenhum item importado! Verifique a planilha.", Toast.LENGTH_LONG).show();
                        resetarEstadoBotaoPlanilha();
                    } else {
                        // Se já temos setores, aplica o mapeamento agora
                        if (listaSetores != null && !listaSetores.isEmpty()) {
                            Map<String, String> mapa = ImportadorSetor.toMap(listaSetores);
                            MapeadorSetor.aplicar(listaPlanilha, mapa);
                        }

                        tvStatusPlanilha.setText("Planilha OK (" + listaPlanilha.size() + " itens)");
                        btnImportarPlanilha.setEnabled(false);
                        btnImportarPlanilha.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.success_green)));
                        imgIconPlanilha.setImageResource(R.drawable.ic_check);

                        btnLimparPlanilha.setVisibility(View.VISIBLE);
                        Toast.makeText(this, "Importados " + listaPlanilha.size() + " itens!", Toast.LENGTH_SHORT).show();
                    }
                }
        );

        // Importar SETORES (2 colunas: código → nome)
        importarSetorLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri == null) return;

                    listaSetores = ImportadorSetor.importar(this, uri);
                    DadosGlobais.getInstance().setListaSetores(listaSetores);

                    if (listaSetores == null || listaSetores.isEmpty()) {
                        Toast.makeText(this, "Erro: Nenhum setor importado!", Toast.LENGTH_LONG).show();
                        resetarEstadoBotaoSetor();
                    } else {
                        // Se já temos planilha, aplica o mapeamento agora
                        if (listaPlanilha != null && !listaPlanilha.isEmpty()) {
                            Map<String, String> mapa = ImportadorSetor.toMap(listaSetores);
                            MapeadorSetor.aplicar(listaPlanilha, mapa);
                            // (Opcional) atualize algo visual se quiser mostrar que os nomes foram aplicados
                        }

                        tvStatusSetor.setText("Setores OK (" + listaSetores.size() + ")");
                        btnImportarSetor.setEnabled(false);
                        btnImportarSetor.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.success_green)));
                        imgIconSetor.setImageResource(R.drawable.ic_check);

                        btnLimparSetor.setVisibility(View.VISIBLE);
                        Toast.makeText(this, "Importados " + listaSetores.size() + " setores!", Toast.LENGTH_SHORT).show();
                    }
                }
        );
    }

    private void mostrarDialogoDeLimpeza(String tipoArquivo) {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_confirmacao, null);
        TextView tvTitle = dialogView.findViewById(R.id.tvConfirmTitle);
        TextView tvMsg   = dialogView.findViewById(R.id.tvConfirmMsg);
        MaterialButton btnCancel  = dialogView.findViewById(R.id.btnCancel);
        MaterialButton btnConfirm = dialogView.findViewById(R.id.btnConfirm);

        boolean limparPlanilha = "planilha".equalsIgnoreCase(tipoArquivo);
        String titulo = limparPlanilha ? "Descartar planilha" : "Descartar setores";
        String mensagem = limparPlanilha
                ? "Tem certeza que deseja descartar a planilha importada? Você precisará selecionar o arquivo novamente."
                : "Tem certeza que deseja descartar o arquivo de setores? Você precisará selecionar o arquivo novamente.";

        tvTitle.setText(titulo);
        tvMsg.setText(mensagem);

        AlertDialog dialog = new AlertDialog.Builder(this, R.style.AppDialogTheme)
                .setView(dialogView)
                .setCancelable(true)
                .create();

        btnCancel.setOnClickListener(v -> dialog.dismiss());
        btnConfirm.setOnClickListener(v -> {
            if (limparPlanilha) {
                resetarEstadoBotaoPlanilha();
            } else {
                resetarEstadoBotaoSetor();
                // (Opcional) Se quiser também "desmapear" nomes de setor já aplicados na listaPlanilha, faça aqui.
                // Ex.: for (ItemPlanilha item : listaPlanilha) { item.setNomeSetor(null); }
            }
            dialog.dismiss();
        });

        dialog.show();
    }


    private void resetarEstadoBotaoPlanilha() {
        listaPlanilha.clear();
        DadosGlobais.getInstance().setListaPlanilha(null);
        tvStatusPlanilha.setText("Importar Dados");
        btnImportarPlanilha.setEnabled(true);
        btnImportarPlanilha.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.novo_atacarejo_blue)));
        imgIconPlanilha.setImageResource(R.drawable.ic_upload_file);
        btnLimparPlanilha.setVisibility(View.GONE);
        Toast.makeText(this, "Planilha descartada.", Toast.LENGTH_SHORT).show();
    }

    private void resetarEstadoBotaoSetor() {
        listaSetores.clear();
        DadosGlobais.getInstance().setListaSetores(null);
        tvStatusSetor.setText("Importar Setores");
        btnImportarSetor.setEnabled(true);
        btnImportarSetor.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.novo_atacarejo_blue)));
        imgIconSetor.setImageResource(R.drawable.ic_upload_file);
        btnLimparSetor.setVisibility(View.GONE);
        Toast.makeText(this, "Setores descartados.", Toast.LENGTH_SHORT).show();
    }

    public void onImportarPlanilha(View v) { importarPlanilhaLauncher.launch("text/*"); }
    public void onImportarSetor(View v) { importarSetorLauncher.launch("text/*"); }

    public void onEscolherLoja(View v) {
        if (listaPlanilha == null || listaPlanilha.isEmpty() ||
                listaSetores == null || listaSetores.isEmpty()) {
            Toast.makeText(this, "Importe a planilha e os setores primeiro!", Toast.LENGTH_SHORT).show();
            return;
        }
        startActivity(new Intent(this, LojaActivity.class));
    }

    public void onLogout(View v) {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_logout, null);

        AlertDialog dialog = new AlertDialog.Builder(this, R.style.AppDialogTheme)
                .setView(dialogView)
                .setCancelable(false)
                .create();

        Button btnSair = dialogView.findViewById(R.id.btnSair);
        Button btnCancelar = dialogView.findViewById(R.id.btnCancelar);

        btnSair.setOnClickListener(view -> {
            getSharedPreferences("prefs", MODE_PRIVATE).edit().clear().apply();
            DadosGlobais.getInstance().resetar();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            dialog.dismiss();
        });

        btnCancelar.setOnClickListener(view -> dialog.dismiss());
        dialog.show();
    }
}
