package com.rktec.rfidapp;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class ImportadorSetor {

    /** Lê planilha com 2 colunas: [codigo, nome] (cabeçalho ignorado) → List<SetorLocalizacao> */
    public static List<SetorLocalizacao> importar(Context context, Uri fileUri) {
        List<SetorLocalizacao> lista = new ArrayList<>();

        try (InputStream inputStream = context.getContentResolver().openInputStream(fileUri);
             BufferedReader br = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {

            String line = br.readLine(); // cabeçalho
            if (line == null) return lista;
            char sep = detectSeparator(line);

            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                List<String> cols = splitCsv(line, sep);
                if (cols.size() >= 2) {
                    String cod = safe(cols.get(0));
                    String nome = safe(cols.get(1));
                    if (!cod.isEmpty()) {
                        lista.add(new SetorLocalizacao(normalizeCodigo(cod), nome));
                    }
                }
            }
        } catch (Exception e) {
            Log.e("ImportadorSetor", "Erro ao importar setores", e);
        }
        return lista;
    }

    /** Converte lista para mapa codigo->nome (útil para aplicar no ItemPlanilha) */
    // substitua o toMap antigo por este:
    public static Map<String, String> toMap(List<SetorLocalizacao> lista) {
        Map<String, String> mapa = new LinkedHashMap<>();
        if (lista == null) return mapa;
        for (SetorLocalizacao s : lista) {
            if (s == null) continue;
            String cod = normalizeCodigo(getCodigoFlexible(s));
            String nome = safe(getNomeFlexible(s));
            if (!cod.isEmpty()) mapa.put(cod, nome);
        }
        return mapa;
    }

    /** Tenta vários nomes de campo comuns para "código" (codigo, cod, codlocalizacao, code) */
    private static String getCodigoFlexible(Object o) {
        return getFieldFlexible(o, "codigo", "cod", "codlocalizacao", "code", "id", "chave");
    }

    /** Tenta vários nomes de campo comuns para "nome" (nome, setor, descricao, label, name) */
    private static String getNomeFlexible(Object o) {
        return getFieldFlexible(o, "nome", "setor", "descricao", "label", "name", "titulo");
    }

    /** Busca um dos campos fornecidos na classe, via reflexão, e retorna o valor como String */
    private static String getFieldFlexible(Object o, String... candidates) {
        for (String name : candidates) {
            try {
                java.lang.reflect.Field f = o.getClass().getDeclaredField(name);
                f.setAccessible(true);
                Object v = f.get(o);
                if (v != null) return v.toString();
            } catch (NoSuchFieldException | IllegalAccessException ignored) {}
        }
        return "";
    }


    private static String safe(String v) { return v == null ? "" : v.trim(); }

    private static String normalizeCodigo(String raw) {
        String s = safe(raw).replace("\u00A0", ""); // NBSP
        if (s.matches("^\\d+$")) s = s.replaceFirst("^0+(?!$)", "");
        return s;
    }

    private static char detectSeparator(String header) {
        int c = count(header, ',');
        int s = count(header, ';');
        int t = count(header, '\t');
        if (t >= c && t >= s) return '\t';
        return (s > c) ? ';' : ',';
    }
    private static int count(String s, char ch) { int n=0; for (int i=0;i<s.length();i++) if (s.charAt(i)==ch) n++; return n; }

    private static List<String> splitCsv(String line, char sep) {
        List<String> out = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        boolean inQuotes = false;

        for (int i=0;i<line.length();i++) {
            char ch = line.charAt(i);
            if (ch=='"') {
                if (inQuotes && i+1<line.length() && line.charAt(i+1)=='"') { sb.append('"'); i++; }
                else inQuotes = !inQuotes;
            } else if (ch==sep && !inQuotes) {
                out.add(sb.toString()); sb.setLength(0);
            } else { sb.append(ch); }
        }
        out.add(sb.toString());
        return out;
    }
}
