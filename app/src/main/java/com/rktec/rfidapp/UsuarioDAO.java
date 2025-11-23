package com.rktec.rfidapp;

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

    public boolean existeCEO() {
        Cursor c = db.rawQuery(
                "SELECT COUNT(*) FROM usuarios WHERE UPPER(permissao) = 'CEO'",
                null
        );
        boolean existe = false;
        if (c.moveToFirst()) {
            existe = c.getInt(0) > 0;
        }
        c.close();
        return existe;
    }

    public boolean haAlgumUsuario() {
        Cursor c = db.rawQuery("SELECT COUNT(*) FROM usuarios", null);
        boolean existe = false;
        if (c.moveToFirst()) {
            existe = c.getInt(0) > 0;
        }
        c.close();
        return existe;
    }


    public boolean cadastrarUsuario(String nome, String senha, String permissao) {
        // Normaliza permissão
        if (permissao == null || permissao.trim().isEmpty()) {
            permissao = "MEMBRO";
        } else {
            permissao = permissao.trim().toUpperCase();
        }

        // SE NÃO EXISTE NENHUM USUÁRIO → PRIMEIRO SEMPRE É CEO
        if (!haAlgumUsuario()) {
            permissao = "CEO";
        }

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

    public boolean atualizarPermissaoUsuario(String nome, String novaPermissao) {
        if (nome == null || nome.trim().isEmpty()) return false;

        ContentValues values = new ContentValues();
        values.put("permissao", novaPermissao);

        int linhas = db.update("usuarios", values, "nome = ?", new String[]{nome});
        return linhas > 0;
    }

    public String getNomeCEO() {
        String nomeCEO = null;
        Cursor c = db.rawQuery(
                "SELECT nome FROM usuarios WHERE UPPER(permissao) = 'CEO' LIMIT 1",
                null
        );
        if (c.moveToFirst()) {
            nomeCEO = c.getString(0);
        }
        c.close();
        return nomeCEO;
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

    public List<Usuario> getTodosUsuariosObj() {
        List<Usuario> lista = new ArrayList<>();
        Cursor c = db.rawQuery("SELECT id, nome, senha, permissao FROM usuarios", null);
        while (c.moveToNext()) {
            int id = c.getInt(0);
            String nome = c.getString(1);
            String senha = c.getString(2);
            String permissao = c.getString(3);
            lista.add(new Usuario(id, nome, senha, permissao));
        }
        c.close();
        return lista;
    }

}
