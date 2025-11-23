package com.rktec.rfidapp;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class CadastroActivity extends AppCompatActivity {

    private boolean primeiroAcesso;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cadastro);

        primeiroAcesso = getIntent().getBooleanExtra("primeiroAcesso", false);

        EditText edtNome = findViewById(R.id.edtNomeCadastro);
        EditText edtSenha = findViewById(R.id.edtSenhaCadastro);
        EditText edtRepetirSenha = findViewById(R.id.edtRepetirSenhaCadastro);
        Button btnCadastrar = findViewById(R.id.btnCadastrar);
        TextView tvVoltarLogin = findViewById(R.id.tvVoltarLogin);

        UsuarioDAO dao = new UsuarioDAO(this);

        // SE NÃO FOR PRIMEIRO ACESSO, GARANTE QUE O LOGADO É CEO
        if (!primeiroAcesso) {
            String nomeLogado = getSharedPreferences("prefs", MODE_PRIVATE)
                    .getString("usuario_nome", "");
            String permissaoLogado = getSharedPreferences("prefs", MODE_PRIVATE)
                    .getString("usuario_permissao", "MEMBRO");

            if (!"CEO".equalsIgnoreCase(permissaoLogado)) {
                Toast.makeText(this,
                        "Somente o CEO pode cadastrar novos usuários.",
                        Toast.LENGTH_LONG).show();
                finish();
                return;
            }
        }

        btnCadastrar.setOnClickListener(v -> {
            String nome = edtNome.getText().toString().trim();
            String senha = edtSenha.getText().toString().trim();
            String repetirSenha = edtRepetirSenha.getText().toString().trim();

            if (nome.isEmpty() || senha.isEmpty() || repetirSenha.isEmpty()) {
                Toast.makeText(this, "Preencha todos os campos!", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!senha.equals(repetirSenha)) {
                Toast.makeText(this, "As senhas não coincidem.", Toast.LENGTH_SHORT).show();
                return;
            }

            if (dao.existeUsuario(nome)) {
                Toast.makeText(this, "Usuário já existe!", Toast.LENGTH_SHORT).show();
                return;
            }

            // A permissão é decidida dentro do DAO (primeiro usuário vira CEO).
            // Aqui podemos passar null ou uma string padrão.
            String permissao = null;

            if (dao.cadastrarUsuario(nome, senha, permissao)) {
                Toast.makeText(this, "Usuário cadastrado com sucesso!", Toast.LENGTH_SHORT).show();
                finish(); // volta pra tela anterior (login ou tela de gestão, dependendo de onde veio)
            } else {
                Toast.makeText(this, "Erro ao cadastrar!", Toast.LENGTH_SHORT).show();
            }
        });

        tvVoltarLogin.setOnClickListener(v -> finish());
    }
}
