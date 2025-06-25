package com.rktec.rfidapp;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

public class BancoHelper extends SQLiteOpenHelper {

    private static final String NOME_BANCO = "inventario.db";
    private static final int VERSAO = 1;

    public BancoHelper(Context context) {
        super(context, NOME_BANCO, null, VERSAO);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // Cria a tabela igual tua planilha
        db.execSQL("CREATE TABLE IF NOT EXISTS item_planilha (" +
                "loja TEXT, sqbem TEXT, codgrupo TEXT, codlocalizacao TEXT, nrobem TEXT, " +
                "nroincorp TEXT, descresumida TEXT, descdetalhada TEXT, qtdbem TEXT, " +
                "nroplaqueta TEXT, nroseriebem TEXT, modelobem TEXT)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS item_planilha");
        onCreate(db);
    }

    // BancoHelper.java
    public void atualizarDescricaoESetor(String nroplaqueta, String novaDesc, String novoCodSetor) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("descresumida", novaDesc);
        values.put("codlocalizacao", novoCodSetor);
        db.update("item_planilha", values, "nroplaqueta = ?", new String[]{nroplaqueta});
        db.close();
    }

    // BancoHelper.java
    public List<ItemPlanilha> buscarItensPorSetor(String codSetor) {
        List<ItemPlanilha> lista = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM item_planilha WHERE codlocalizacao = ?", new String[]{codSetor});
        while (cursor.moveToNext()) {
            // Mapeia os campos para o teu construtor ItemPlanilha
            lista.add(new ItemPlanilha(
                    cursor.getString(cursor.getColumnIndexOrThrow("loja")),
                    cursor.getString(cursor.getColumnIndexOrThrow("sqbem")),
                    cursor.getString(cursor.getColumnIndexOrThrow("codgrupo")),
                    cursor.getString(cursor.getColumnIndexOrThrow("codlocalizacao")),
                    cursor.getString(cursor.getColumnIndexOrThrow("nrobem")),
                    cursor.getString(cursor.getColumnIndexOrThrow("nroincorp")),
                    cursor.getString(cursor.getColumnIndexOrThrow("descresumida")),
                    cursor.getString(cursor.getColumnIndexOrThrow("descdetalhada")),
                    cursor.getString(cursor.getColumnIndexOrThrow("qtdbem")),
                    cursor.getString(cursor.getColumnIndexOrThrow("nroplaqueta")),
                    cursor.getString(cursor.getColumnIndexOrThrow("nroseriebem")),
                    cursor.getString(cursor.getColumnIndexOrThrow("modelobem"))
            ));
        }
        cursor.close();
        db.close();
        return lista;
    }

}
