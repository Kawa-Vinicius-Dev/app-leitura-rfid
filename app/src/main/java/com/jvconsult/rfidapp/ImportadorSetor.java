package com.jvconsult.rfidapp;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import com.opencsv.CSVReader;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class ImportadorSetor {

    public static List<SetorLocalizacao> importar(Context context, Uri fileUri) {
        List<SetorLocalizacao> lista = new ArrayList<>();
        try {
            InputStream inputStream = context.getContentResolver().openInputStream(fileUri);
            CSVReader reader = new CSVReader(new InputStreamReader(inputStream, "UTF-8"));
            String[] linha;
            boolean primeira = true;
            while ((linha = reader.readNext()) != null) {
                if (primeira) { primeira = false; continue; } // pula o cabeçalho
                if (linha.length < 2) continue;
                lista.add(new SetorLocalizacao(
                        linha[0], linha[1]
                ));
            }
            reader.close();
        } catch (Exception e) {
            Log.e("ImportadorSetor", "Erro ao importar setores: " + e.getMessage());
        }
        return lista;
    }
}
