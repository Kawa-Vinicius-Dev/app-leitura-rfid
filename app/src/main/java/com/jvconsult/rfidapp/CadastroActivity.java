package com.jvconsult.rfidapp;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class CadastroActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cadastro);

        EditText edtNome = findViewById(R.id.edtNomeCadastro);
        EditText edtSenha = findViewById(R.id.edtSenhaCadastro);
        Button btnCadastrar = findViewById(R.id.btnCadastrar);
        TextView tvVoltarLogin = findViewById(R.id.tvVoltarLogin);

        UsuarioDAO dao = new UsuarioDAO(this);

        btnCadastrar.setOnClickListener(v -> {
            String nome = edtNome.getText().toString().trim();
            String senha = edtSenha.getText().toString().trim();

            if (nome.isEmpty() || senha.isEmpty()) {
                Toast.makeText(this, "Preencha todos os campos!", Toast.LENGTH_SHORT).show();
                return;
            }

            if (dao.existeUsuario(nome)) {
                Toast.makeText(this, "Usuário já existe!", Toast.LENGTH_SHORT).show();
                return;
            }

            if (dao.cadastrarUsuario(nome, senha)) {
                Toast.makeText(this, "Usuário cadastrado com sucesso!", Toast.LENGTH_SHORT).show();
                finish(); // Volta pra tela de login
            } else {
                Toast.makeText(this, "Erro ao cadastrar!", Toast.LENGTH_SHORT).show();
            }
        });

        tvVoltarLogin.setOnClickListener(v -> finish());
    }
}
