package com.jvconsult.rfidapp;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.util.ArrayList;
import java.util.List;

public class UsuarioDAO {
    private SQLiteDatabase db;

    public UsuarioDAO(Context context) {
        AppDatabaseHelper helper = new AppDatabaseHelper(context);
        db = helper.getWritableDatabase();
    }

    public boolean cadastrarUsuario(String nome, String senha, String permissao) {
        ContentValues values = new ContentValues();
        values.put("nome", nome);
        values.put("senha", senha);
        values.put("permissao", permissao);
        long id = db.insert("usuarios", null, values);
        return id != -1;
    }

    public Usuario autenticar(String nome, String senha) {
        Cursor c = db.rawQuery(
                "SELECT * FROM usuarios WHERE nome=? AND senha=?",
                new String[]{nome, senha}
        );
        if (c.moveToFirst()) {
            int id = c.getInt(c.getColumnIndex("id"));
            String userNome = c.getString(c.getColumnIndex("nome"));
            String userSenha = c.getString(c.getColumnIndex("senha"));
            String permissao = c.getString(c.getColumnIndex("permissao"));
            c.close();
            return new Usuario(id, userNome, userSenha, permissao);
        }
        c.close();
        return null;
    }

    public boolean existeUsuario(String nome) {
        Cursor c = db.rawQuery(
                "SELECT id FROM usuarios WHERE nome=?",
                new String[]{nome}
        );
        boolean exists = c.moveToFirst();
        c.close();
        return exists;
    }

    // NOVOS MÉTODOS

    public String getPermissaoUsuario(String nome) {
        Cursor c = db.rawQuery(
                "SELECT permissao FROM usuarios WHERE nome=?",
                new String[]{nome}
        );
        String permissao = "membro";
        if (c.moveToFirst()) {
            permissao = c.getString(0);
        }
        c.close();
        return permissao;
    }

    // Lista todos usuários (inclusive admins)
    public List<String> getTodosUsuarios() {
        List<String> lista = new ArrayList<>();
        Cursor c = db.rawQuery("SELECT nome FROM usuarios", null);
        while (c.moveToNext()) {
            lista.add(c.getString(0));
        }
        c.close();
        return lista;
    }


    public void removerUsuario(String nome) {
        db.delete("usuarios", "nome=?", new String[]{nome});
    }


    public void fechar() {
        db.close();
    }
}
