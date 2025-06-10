package com.jvconsult.rfidapp;

import android.os.Environment;
import android.util.Log;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

public class CsvExporter {

    public static void exportToCsv(List<String> dataList, String fileName) {
        try {
            File exportDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            if (!exportDir.exists()) {
                exportDir.mkdirs();
            }

            File file = new File(exportDir, fileName);

            BufferedWriter bw = new BufferedWriter(new FileWriter(file));

            // Cabeçalho
            bw.write("RFID_TAG");
            bw.newLine();

            // Dados
            for (String data : dataList) {
                bw.write(data);
                bw.newLine();
            }

            bw.flush();
            bw.close();

            Log.d("CSV Export", "Arquivo exportado para: " + file.getAbsolutePath());

        } catch (IOException e) {
            Log.e("CSV Export", "Erro ao exportar CSV: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
