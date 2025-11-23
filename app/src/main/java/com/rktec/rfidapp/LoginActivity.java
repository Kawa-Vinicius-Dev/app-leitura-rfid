package com.rktec.rfidapp;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.view.View;

public class LoginActivity extends AppCompatActivity {

    private UsuarioDAO dao;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        EditText edtNome = findViewById(R.id.edtNomeUsuario);
        EditText edtSenha = findViewById(R.id.edtSenha);
        Button btnEntrar = findViewById(R.id.btnEntrar);
        TextView tvCadastrar = findViewById(R.id.tvCadastro);

        dao = new UsuarioDAO(this);

        btnEntrar.setOnClickListener(v -> {
            String nome = edtNome.getText().toString().trim();
            String senha = edtSenha.getText().toString().trim();

            if (nome.isEmpty() || senha.isEmpty()) {
                Toast.makeText(this, "Preencha usuário e senha!", Toast.LENGTH_SHORT).show();
                return;
            }

            Usuario usuario = dao.autenticar(nome, senha);
            if (usuario != null) {
                getSharedPreferences("prefs", MODE_PRIVATE)
                        .edit()
                        .putString("usuario_nome", nome)
                        .putString("usuario_permissao", usuario.permissao)
                        .apply();
                DadosGlobais.getInstance().setUsuario(nome);

                startActivity(new Intent(this, MainActivity.class));
                finish();
            } else {
                Toast.makeText(this, "Usuário ou senha incorretos!", Toast.LENGTH_SHORT).show();
            }
        });

        tvCadastrar.setOnClickListener(v -> {
            if (dao.haAlgumUsuario()) {
                // Já existe usuário → mostrar diálogo de contato com CEO
                String nomeCEO = dao.getNomeCEO();
                mostrarDialogContatoCEO(nomeCEO);
            } else {
                // Primeiro acesso: vamos cadastrar o CEO
                Intent it = new Intent(this, CadastroActivity.class);
                it.putExtra("primeiroAcesso", true);
                startActivity(it);
            }
        });
    }

    private void mostrarDialogContatoCEO(String nomeCEO) {
        View view = getLayoutInflater().inflate(R.layout.dialog_contato_ceo, null);

        TextView tvContatoMsg = view.findViewById(R.id.tvContatoMsg);
        View btnContatoOk = view.findViewById(R.id.btnContatoOk);

        String mensagem;
        if (nomeCEO != null && !nomeCEO.trim().isEmpty()) {
            mensagem = "Para criar uma conta neste sistema, entre em contato com o CEO do patrimônio:\n\n"
                    + nomeCEO
                    + "\n\nSomente ele pode realizar novos cadastros de usuários.";
        } else {
            mensagem = "Para criar uma conta neste sistema, entre em contato com o responsável pelo patrimônio.\n\n" +
                    "Somente o CEO pode realizar novos cadastros de usuários.";
        }
        tvContatoMsg.setText(mensagem);

        AlertDialog dialog = new AlertDialog.Builder(this, R.style.AppDialogTheme)
                .setView(view)
                .setCancelable(true)
                .create();

        btnContatoOk.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    @Override
    protected void onDestroy() {
        if (dao != null) dao.fechar();
        super.onDestroy();
    }
}
