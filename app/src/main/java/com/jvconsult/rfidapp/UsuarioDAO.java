// UsuarioDAO.java
package com.jvconsult.rfidapp;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

public class UsuarioDAO {
    private SQLiteDatabase db;

    public UsuarioDAO(Context context) {
        AppDatabaseHelper helper = new AppDatabaseHelper(context);
        db = helper.getWritableDatabase();
    }

    // Cadastrar novo usuário
    public boolean cadastrarUsuario(String nome, String senha) {
        ContentValues values = new ContentValues();
        values.put("nome", nome);
        values.put("senha", senha);
        long id = db.insert("usuarios", null, values);
        return id != -1;
    }

    // Buscar usuário (login)
    public Usuario autenticar(String nome, String senha) {
        Cursor c = db.rawQuery(
                "SELECT * FROM usuarios WHERE nome=? AND senha=?",
                new String[]{nome, senha}
        );
        if (c.moveToFirst()) {
            int id = c.getInt(c.getColumnIndex("id"));
            String userNome = c.getString(c.getColumnIndex("nome"));
            String userSenha = c.getString(c.getColumnIndex("senha"));
            c.close();
            return new Usuario(id, userNome, userSenha);
        }
        c.close();
        return null;
    }

    // Verifica se já existe
    public boolean existeUsuario(String nome) {
        Cursor c = db.rawQuery(
                "SELECT id FROM usuarios WHERE nome=?",
                new String[]{nome}
        );
        boolean exists = c.moveToFirst();
        c.close();
        return exists;
    }
}
