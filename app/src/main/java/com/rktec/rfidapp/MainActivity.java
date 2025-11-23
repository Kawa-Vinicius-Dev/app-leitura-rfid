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
import android.graphics.Color;
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

    private AlertDialog loadingDialog;

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
        if (permissao == null) permissao = "";

// CEO: pode cadastrar e gerenciar
        if ("CEO".equalsIgnoreCase(permissao)) {
            btnGerenciarUsuarios.setVisibility(View.VISIBLE);
            btnGerenciarUsuarios.setOnClickListener(v -> mostrarDialogGerenciarAcessos());

// ADM: só gerencia (não cadastra)
        } else if ("ADM".equalsIgnoreCase(permissao)) {
            btnGerenciarUsuarios.setVisibility(View.VISIBLE);
            btnGerenciarUsuarios.setOnClickListener(v ->
                    startActivity(new Intent(MainActivity.this, GerenciarUsuariosActivity.class)));

// Membro comum: não vê nada
        } else {
            btnGerenciarUsuarios.setVisibility(View.GONE);
        }

        // Lixeiras
        btnLimparPlanilha.setOnClickListener(v -> mostrarDialogoDeLimpeza("planilha"));
        btnLimparSetor.setOnClickListener(v -> mostrarDialogoDeLimpeza("setores"));

        // Importar PLANILHA principal
        // Importar PLANILHA principal
        // Importar PLANILHA principal
        importarPlanilhaLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri == null) return;

                    // 🔹 Feedback imediato: volta pro app e mostra loading
                    mostrarLoading("Importando planilha, aguarde...");

                    new Thread(() -> {
                        // roda FORA da UI thread
                        List<ItemPlanilha> importada = ImportadorPlanilha.importar(MainActivity.this, uri);

                        runOnUiThread(() -> {
                            // volta pra UI
                            esconderLoading();

                            listaPlanilha = (importada != null) ? importada : new ArrayList<>();
                            DadosGlobais.getInstance().setListaPlanilha(listaPlanilha);

                            if (listaPlanilha == null || listaPlanilha.isEmpty()) {
                                Toast.makeText(MainActivity.this, "Erro: Nenhum item importado! Verifique a planilha.", Toast.LENGTH_LONG).show();
                                resetarEstadoBotaoPlanilha();
                            } else {
                                // Se já temos setores, aplica o mapeamento agora
                                if (listaSetores != null && !listaSetores.isEmpty()) {
                                    Map<String, String> mapa = ImportadorSetor.toMap(listaSetores);
                                    MapeadorSetor.aplicar(listaPlanilha, mapa);
                                }

                                // 🔹 Feedback visual amigável
                                tvStatusPlanilha.setText("Planilha carregada (" + listaPlanilha.size() + " itens)");

                                // Card “travado” em cinza suave (não mais verdão)
                                btnImportarPlanilha.setEnabled(false);
                                btnImportarPlanilha.setCardBackgroundColor(Color.parseColor("#F5F5F5"));

                                // Ícone de check verde
                                imgIconPlanilha.setImageResource(R.drawable.ic_check);
                                imgIconPlanilha.setColorFilter(
                                        ContextCompat.getColor(MainActivity.this, R.color.success_green)
                                );

                                btnLimparPlanilha.setVisibility(View.VISIBLE);
                                Toast.makeText(MainActivity.this, "Importados " + listaPlanilha.size() + " itens!", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }).start();
                }
        );

        importarSetorLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri == null) return;

                    mostrarLoading("Importando setores, aguarde...");

                    new Thread(() -> {
                        List<SetorLocalizacao> importados = ImportadorSetor.importar(MainActivity.this, uri);

                        runOnUiThread(() -> {
                            esconderLoading();

                            listaSetores = (importados != null) ? importados : new ArrayList<>();
                            DadosGlobais.getInstance().setListaSetores(listaSetores);

                            if (listaSetores == null || listaSetores.isEmpty()) {
                                Toast.makeText(MainActivity.this, "Erro: Nenhum setor importado!", Toast.LENGTH_LONG).show();
                                resetarEstadoBotaoSetor();
                            } else {
                                if (listaPlanilha != null && !listaPlanilha.isEmpty()) {
                                    Map<String, String> mapa = ImportadorSetor.toMap(listaSetores);
                                    MapeadorSetor.aplicar(listaPlanilha, mapa);
                                }

                                tvStatusSetor.setText("Setores carregados (" + listaSetores.size() + ")");

                                btnImportarSetor.setEnabled(false);
                                btnImportarSetor.setCardBackgroundColor(Color.parseColor("#F5F5F5"));

                                imgIconSetor.setImageResource(R.drawable.ic_check);
                                imgIconSetor.setColorFilter(
                                        ContextCompat.getColor(MainActivity.this, R.color.success_green)
                                );

                                btnLimparSetor.setVisibility(View.VISIBLE);
                                Toast.makeText(MainActivity.this, "Importados " + listaSetores.size() + " setores!", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }).start();
                }
        );
    }

    private void mostrarDialogGerenciarAcessos() {
        View view = getLayoutInflater().inflate(R.layout.dialog_gerenciar_acessos, null);

        Button btnVerUsuarios  = view.findViewById(R.id.btnVerUsuarios);
        Button btnNovoUsuario  = view.findViewById(R.id.btnNovoUsuario);

        AlertDialog dialog = new AlertDialog.Builder(this, R.style.AppDialogTheme)
                .setView(view)
                .setCancelable(true)
                .create();

        btnVerUsuarios.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, GerenciarUsuariosActivity.class));
            dialog.dismiss();
        });

        btnNovoUsuario.setOnClickListener(v -> {
            Intent it = new Intent(MainActivity.this, CadastroActivity.class);
            it.putExtra("primeiroAcesso", false); // cadastro feito de dentro do sistema
            startActivity(it);
            dialog.dismiss();
        });

        dialog.show();
    }

    private void mostrarLoading(String mensagem) {
        if (loadingDialog == null) {
            View view = getLayoutInflater().inflate(R.layout.dialog_loading, null);
            TextView tvMsg = view.findViewById(R.id.tvLoadingMsg);
            tvMsg.setText(mensagem);

            loadingDialog = new AlertDialog.Builder(this, R.style.AppDialogTheme)
                    .setView(view)
                    .setCancelable(false)
                    .create();
            loadingDialog.show();
        } else {
            if (!loadingDialog.isShowing()) {
                loadingDialog.show();
            }
            TextView tvMsg = loadingDialog.findViewById(R.id.tvLoadingMsg);
            if (tvMsg != null) tvMsg.setText(mensagem);
        }
    }

    private void esconderLoading() {
        if (loadingDialog != null && loadingDialog.isShowing()) {
            loadingDialog.dismiss();
        }
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
        btnImportarPlanilha.setCardBackgroundColor(Color.WHITE);

        imgIconPlanilha.setImageResource(R.drawable.ic_upload_file);
        imgIconPlanilha.setColorFilter(
                ContextCompat.getColor(this, R.color.novo_atacarejo_blue)
        );

        btnLimparPlanilha.setVisibility(View.GONE);
        Toast.makeText(this, "Planilha descartada.", Toast.LENGTH_SHORT).show();
    }

    private void resetarEstadoBotaoSetor() {
        listaSetores.clear();
        DadosGlobais.getInstance().setListaSetores(null);

        tvStatusSetor.setText("Importar Setores");

        btnImportarSetor.setEnabled(true);
        btnImportarSetor.setCardBackgroundColor(Color.WHITE);

        imgIconSetor.setImageResource(R.drawable.ic_upload_file);
        imgIconSetor.setColorFilter(
                ContextCompat.getColor(this, R.color.novo_atacarejo_blue)
        );

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
