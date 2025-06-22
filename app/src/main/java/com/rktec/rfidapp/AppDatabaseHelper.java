// AppDatabaseHelper.java
package com.rktec.rfidapp;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class AppDatabaseHelper extends SQLiteOpenHelper {
    private static final String DB_NAME = "app_db.sqlite";
    private static final int DB_VERSION = 2;

    public AppDatabaseHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(
                "CREATE TABLE usuarios (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        "nome TEXT NOT NULL, " +
                        "senha TEXT NOT NULL, " +
                        "permissao TEXT NOT NULL DEFAULT 'membro'" +  // <-- Adiciona isso!
                        ")"
        );
    }

    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE usuarios ADD COLUMN permissao TEXT NOT NULL DEFAULT 'membro'");
        }
        // ...futuras migrações aqui
    }
}
