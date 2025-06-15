package com.jvconsult.rfidapp;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class LoginActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        EditText edtNome = findViewById(R.id.edtNomeUsuario);
        Button   btnEntrar = findViewById(R.id.btnEntrar);

        btnEntrar.setOnClickListener(v -> {
            String nome = edtNome.getText().toString().trim();
            if (nome.isEmpty()) {
                Toast.makeText(this, "Digite seu nome!", Toast.LENGTH_SHORT).show();
                return;
            }

            // Salva localmente
            getSharedPreferences("prefs", MODE_PRIVATE)
                    .edit()
                    .putString("usuario_nome", nome)
                    .apply();

            // Deixa no singleton para acesso rápido
            DadosGlobais.getInstance().setUsuario(nome);

            startActivity(new Intent(this, MainActivity.class));
            finish();
        });
    }
}
