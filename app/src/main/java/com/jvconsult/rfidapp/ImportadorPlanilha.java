package com.jvconsult.rfidapp;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import com.opencsv.CSVReader;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class ImportadorPlanilha {

    public static List<ItemPlanilha> importar(Context context, Uri fileUri) {
        List<ItemPlanilha> lista = new ArrayList<>();
        try {
            InputStream inputStream = context.getContentResolver().openInputStream(fileUri);
            CSVReader reader = new CSVReader(new InputStreamReader(inputStream, "UTF-8"));
            String[] linha;
            boolean primeira = true;
            while ((linha = reader.readNext()) != null) {
                if (primeira) { primeira = false; continue; } // pula o cabeçalho
                if (linha.length < 12) continue;
                lista.add(new ItemPlanilha(
                        linha[0], linha[1], linha[2], linha[3], linha[4], linha[5],
                        linha[6], linha[7], linha[8], linha[9], linha[10], linha[11]
                ));
            }
            reader.close();
        } catch (Exception e) {
            Log.e("ImportadorPlanilha", "Erro ao importar: " + e.getMessage());
        }
        return lista;
    }
}
