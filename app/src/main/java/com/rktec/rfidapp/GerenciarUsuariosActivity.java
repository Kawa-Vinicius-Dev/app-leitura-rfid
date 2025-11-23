package com.rktec.rfidapp;

import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.ListView;
import androidx.appcompat.app.AppCompatActivity;
import java.util.List;

public class GerenciarUsuariosActivity extends AppCompatActivity {

    private ListView listaUsuarios;
    private UsuarioDAO dao;
    private UsuarioAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gerenciar_usuarios);

        listaUsuarios = findViewById(R.id.listaUsuarios);
        dao = new UsuarioDAO(this);

        ImageButton btnVoltar = findViewById(R.id.btnBack);
        btnVoltar.setOnClickListener(v -> finish());

        atualizarListaUsuarios();
    }

    private void atualizarListaUsuarios() {
        List<Usuario> usuarios = dao.getTodosUsuariosObj();
        adapter = new UsuarioAdapter(this, usuarios, dao, this::atualizarListaUsuarios);
        listaUsuarios.setAdapter(adapter);
    }

    @Override
    protected void onDestroy() {
        dao.fechar();
        super.onDestroy();
    }
}
