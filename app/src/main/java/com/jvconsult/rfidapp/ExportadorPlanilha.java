package com.jvconsult.rfidapp;

import android.content.Context;
import android.util.Log;
import java.io.File;
import java.io.FileWriter;
import java.util.List;

public class ExportadorPlanilha {
    public static File exportarCSV(Context context, List<ItemPlanilha> lista) {
        File pasta = context.getExternalFilesDir(null);
        File arquivo = new File(pasta, "inventario_editado.csv");
        try (FileWriter writer = new FileWriter(arquivo, false)) {
            writer.write("loja,sqbem,codgrupo,codlocalizacao,nrobem,nroincorp,descresumida,descdetalhada,qtdbem,nroplaqueta,nroseriebem,modelobem\n");
            for (ItemPlanilha item : lista) {
                writer.write(item.loja + "," + item.sqbem + "," + item.codgrupo + "," + item.codlocalizacao + "," +
                        item.nrobem + "," + item.nroincorp + "," + item.descresumida + "," + item.descdetalhada + "," +
                        item.qtdbem + "," + item.nroplaqueta + "," + item.nroseriebem + "," + item.modelobem + "\n");
            }
            writer.flush();
            Log.d("ExportadorPlanilha", "Exportado para: " + arquivo.getAbsolutePath());
        } catch (Exception e) {
            Log.e("ExportadorPlanilha", "Erro ao exportar: " + e.getMessage());
        }
        return arquivo;
    }
}
