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
    public static void logRelatorio(
            Context context,
            String usuario,
            List<ItemPlanilha> itensMovidos,
            List<ItemPlanilha> itensOutrasLojas,
            List<String> epcsNaoCadastrados
    ) {
        try {
            File file = new File(context.getExternalFilesDir(null), "log_app.txt");
            FileWriter writer = new FileWriter(file, true);
            String data = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
            writer.write("\n===== RELATÓRIO INVENTÁRIO - " + data + " [" + usuario + "] =====\n");

            writer.write("Itens movidos para o setor selecionado:\n");
            for (ItemPlanilha item : itensMovidos)
                writer.write("- " + item.descresumida + " (Plaqueta: " + item.nroplaqueta + ")\n");

            writer.write("\nItens que estavam em outras lojas/setores:\n");
            for (ItemPlanilha item : itensOutrasLojas)
                writer.write("- " + item.descresumida +
                        " (Plaqueta: " + item.nroplaqueta +
                        ", Loja: " + item.loja +
                        ", Setor: " + item.codlocalizacao + ")\n");

            writer.write("\nEPCs não cadastrados em nenhuma loja:\n");

            for (String epc : epcsNaoCadastrados)
                writer.write("- " + epc + "\n");


            writer.write("===============================================\n");
            writer.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
