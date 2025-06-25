package com.rktec.rfidapp;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import com.opencsv.CSVReader;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class ImportadorPlanilha {

    // Caso ainda use lista, retorna a lista
    public static List<ItemPlanilha> importar(Context context, Uri fileUri) {
        List<ItemPlanilha> lista = new ArrayList<>();
        int count = 0;
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
                count++;
            }
            reader.close();
        } catch (Exception e) {
            Log.e("ImportadorPlanilha", "Erro ao importar: " + e.getMessage());
        }
        Log.d("ImportadorPlanilha", "Importados " + count + " itens.");
        return lista;
    }
}
