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

        EditText edtCodigoAdm = findViewById(R.id.edtCodigoAdm); // novo campo

        btnCadastrar.setOnClickListener(v -> {
            String nome = edtNome.getText().toString().trim();
            String senha = edtSenha.getText().toString().trim();
            String codigoAdm = edtCodigoAdm.getText().toString().trim();

            String permissao = "membro";
            if (codigoAdm.equals("ADM2025")) { // pode colocar o código que tu quiser
                permissao = "adm";
            }

            if (dao.existeUsuario(nome)) {
                Toast.makeText(this, "Usuário já existe!", Toast.LENGTH_SHORT).show();
                return;
            }

            if (dao.cadastrarUsuario(nome, senha, permissao)) {
                Toast.makeText(this, "Usuário cadastrado com sucesso!", Toast.LENGTH_SHORT).show();
                finish(); // Volta pra tela de login
            } else {
                Toast.makeText(this, "Erro ao cadastrar!", Toast.LENGTH_SHORT).show();
            }
        });

        tvVoltarLogin.setOnClickListener(v -> finish());
    }
}
