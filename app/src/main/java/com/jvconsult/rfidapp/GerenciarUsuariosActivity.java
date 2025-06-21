package com.jvconsult.rfidapp;

import android.app.AlertDialog;
import android.os.Bundle;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import java.util.List;

public class GerenciarUsuariosActivity extends AppCompatActivity {

    private ListView listaUsuarios;
    private UsuarioDAO dao;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gerenciar_usuarios);

        listaUsuarios = findViewById(R.id.listaUsuarios);
        dao = new UsuarioDAO(this);

        atualizarListaUsuarios();
    }

    private void atualizarListaUsuarios() {
        List<String> usuarios = dao.getTodosUsuarios();
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, usuarios);
        listaUsuarios.setAdapter(adapter);

        // O CÓDIGO ABAIXO É O QUE VOCÊ QUER!
        listaUsuarios.setOnItemClickListener((parent, view, position, id) -> {
            String nomeSelecionado = usuarios.get(position);

            // --- Diálogo de confirmação ---
            new AlertDialog.Builder(this)
                    .setTitle("Remover usuário")
                    .setMessage("Tem certeza que deseja remover esse usuário?")
                    .setPositiveButton("Remover", (dialog, which) -> {
                        dao.removerUsuario(nomeSelecionado);
                        atualizarListaUsuarios();
                        Toast.makeText(this, "Usuário removido com sucesso!", Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton("Cancelar", null)
                    .show();
        });
    }

    @Override
    protected void onDestroy() {
        dao.fechar();
        super.onDestroy();
    }
}
