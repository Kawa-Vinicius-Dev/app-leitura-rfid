package com.jvconsult.rfidapp;

import android.content.Context;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class LogHelper {
    // Grava log em TXT (um por linha, fácil ler)
    public static void registrarAcao(Context context, String usuario, String acao) {
        try {
            File file = new File(context.getExternalFilesDir(null), "log_app.txt");
            FileWriter writer = new FileWriter(file, true);
            String data = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
            writer.write(data + " [" + usuario + "] " + acao + "\n");
            writer.close();
        } catch (Exception e) { e.printStackTrace(); }
    }

    // Grava log em CSV (para importar no Excel, etc)
    public static void registrarCSV(Context context, String usuario, String acao) {
        try {
            File file = new File(context.getExternalFilesDir(null), "log_app.csv");
            boolean novo = !file.exists();
            FileWriter writer = new FileWriter(file, true);
            if (novo) writer.write("Data,Usuario,Ação\n");
            String data = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
            writer.write(String.format("\"%s\",\"%s\",\"%s\"\n", data, usuario, acao.replace("\"", "'")));
            writer.close();
        } catch (Exception e) { e.printStackTrace(); }
    }
}
