package com.jvconsult.rfidapp;

import android.content.Context;
import android.os.Environment;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class LogHelper {

    // Relatório consolidado CSV por loja
    public static void logRelatorioPorLoja(
            Context context,
            String usuario,
            String loja,
            String setor,
            List<ItemPlanilha> itensMovidos,
            List<ItemPlanilha> itensOutrasLojas,
            List<String> epcsNaoCadastrados
    ) {
        try {
            // Cria a pasta da loja
            File pastaLoja = new File(context.getExternalFilesDir(null), loja + "_RELAT");
            if (!pastaLoja.exists()) pastaLoja.mkdirs();

            // Nome do arquivo final, sempre o mesmo pra loja
            File arquivo = new File(pastaLoja, loja + "_RELAT.csv");
            boolean novoArquivo = !arquivo.exists();

            FileWriter writer = new FileWriter(arquivo, true); // Append
            PrintWriter pw = new PrintWriter(writer);

            // Cabeçalho só se for novo arquivo!
            if (novoArquivo) {
                pw.println("Data/Hora,Usuário,Loja,Setor,Tipo,Desc. Item,Plaqueta,Cód. Localização");
            }
            String data = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());

            for (ItemPlanilha item : itensMovidos) {
                pw.printf("%s,%s,%s,%s,MOVIDO,%s,%s,%s%n",
                        data, usuario, loja, setor, item.descresumida, item.nroplaqueta, item.codlocalizacao);
            }
            for (ItemPlanilha item : itensOutrasLojas) {
                pw.printf("%s,%s,%s,%s,OUTRA LOJA/SETOR,%s,%s,%s%n",
                        data, usuario, item.loja, item.codlocalizacao, item.descresumida, item.nroplaqueta, item.codlocalizacao);
            }
            for (String epc : epcsNaoCadastrados) {
                pw.printf("%s,%s,%s,%s,NAO CADASTRADO,EPC:%s,,%n",
                        data, usuario, loja, setor, epc);
            }

            pw.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
