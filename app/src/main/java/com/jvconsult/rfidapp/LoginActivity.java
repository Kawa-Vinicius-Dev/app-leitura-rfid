package com.jvconsult.rfidapp;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class LoginActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        EditText edtNome = findViewById(R.id.edtNomeUsuario);
        EditText edtSenha = findViewById(R.id.edtSenha);
        Button btnEntrar = findViewById(R.id.btnEntrar);
        Button btnCadastrar = findViewById(R.id.btnCadastrar);

        UsuarioDAO dao = new UsuarioDAO(this);

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
                        .putString("usuario_permissao", usuario.permissao) // <-- Adiciona aqui
                        .apply();
                DadosGlobais.getInstance().setUsuario(nome);

                startActivity(new Intent(this, MainActivity.class));
                finish();
            } else {
                Toast.makeText(this, "Usuário ou senha incorretos!", Toast.LENGTH_SHORT).show();
            }
    });

        TextView tvCadastrar = findViewById(R.id.tvCadastro);
        tvCadastrar.setOnClickListener(v -> {
            startActivity(new Intent(this, CadastroActivity.class));
        });

    }
}
