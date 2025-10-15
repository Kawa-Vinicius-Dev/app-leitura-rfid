package com.rktec.rfidapp;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class ImportadorPlanilha {

    public static List<ItemPlanilha> importar(Context context, Uri fileUri) {
        List<ItemPlanilha> itens = new ArrayList<>();

        try (InputStream inputStream = context.getContentResolver().openInputStream(fileUri);
             BufferedReader br = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {

            // 1) Lê e ignora apenas o cabeçalho (NÃO ignora colunas)
            String header = br.readLine();
            if (header == null) return itens;

            char sep = detectSeparator(header);

            // 2) Lê dados (todas as colunas; 0 = loja)
            String line;
            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty()) continue;

                List<String> cols = splitCsv(line, sep);
                if (cols.isEmpty()) continue;

                // Mapeamento direto 0..11 (se faltar, getOrEmpty devolve "")
                String loja         = getOrEmpty(cols, 0);
                String sqbem        = getOrEmpty(cols, 1);
                String codgrupo     = getOrEmpty(cols, 2);
                String codlocaliz   = getOrEmpty(cols, 3); // codlocalizacao
                String nrobem       = getOrEmpty(cols, 4);
                String nroincorp    = getOrEmpty(cols, 5);
                String descresumida = getOrEmpty(cols, 6);
                String descdetalhada= getOrEmpty(cols, 7);
                String qtdbem       = getOrEmpty(cols, 8);
                String nroplaqueta  = getOrEmpty(cols, 9);
                String nroseriebem  = getOrEmpty(cols,10);
                String modelobem    = getOrEmpty(cols,11);

                ItemPlanilha item = new ItemPlanilha(
                        loja, sqbem, codgrupo, codlocaliz, nrobem, nroincorp,
                        descresumida, descdetalhada, qtdbem, nroplaqueta, nroseriebem, modelobem
                );

                itens.add(item);
            }
        } catch (Exception e) {
            Log.e("ImportadorPlanilha", "Erro ao importar planilha", e);
        }
        return itens;
    }

    private static String getOrEmpty(List<String> cols, int idx) {
        if (idx < 0 || idx >= cols.size()) return "";
        String v = cols.get(idx);
        return v == null ? "" : v.trim();
    }

    private static char detectSeparator(String header) {
        int c = count(header, ',');
        int s = count(header, ';');
        int t = count(header, '\t');
        if (t >= c && t >= s) return '\t';
        return (s > c) ? ';' : ',';
    }

    private static int count(String s, char ch) {
        int n = 0;
        for (int i = 0; i < s.length(); i++) if (s.charAt(i) == ch) n++;
        return n;
    }

    // CSV simples com suporte a aspas e aspas dobradas ("")
    private static List<String> splitCsv(String line, char sep) {
        List<String> out = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char ch = line.charAt(i);
            if (ch == '"') {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    sb.append('"'); // trata ""
                    i++;
                } else {
                    inQuotes = !inQuotes;
                }
            } else if (ch == sep && !inQuotes) {
                out.add(sb.toString());
                sb.setLength(0);
            } else {
                sb.append(ch);
            }
        }
        out.add(sb.toString());
        return out;
    }
}
