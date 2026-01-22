package com.rktec.rfidapp;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ImportadorSetor {

    private static final String TAG = "ImportadorSetor";

    public static List<SetorLocalizacao> importar(Context context, Uri fileUri) {
        List<SetorLocalizacao> lista = new ArrayList<>(3000);
        if (context == null || fileUri == null) return lista;

        try (InputStream is = context.getContentResolver().openInputStream(fileUri)) {
            if (is == null) {
                Log.w(TAG, "InputStream nulo para URI: " + fileUri);
                return lista;
            }

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(is, StandardCharsets.ISO_8859_1))) {

                String header = reader.readLine();
                if (header == null) return lista;

                char sep = detectSeparator(header);
                List<String> headerCols = splitCsv(header, sep);
                int idxLoja   = indexOfIgnoreCase(headerCols, "NROEMPRESA");
                int idxCodigo = indexOfIgnoreCase(headerCols, "SEQLOCAL");
                int idxNome   = indexOfIgnoreCase(headerCols, "CAMINHOLOCALIZACAO");

                if (idxLoja   < 0) idxLoja   = 0;
                if (idxCodigo < 0) idxCodigo = 1;
                if (idxNome   < 0) idxNome   = 2;

                String line;
                while ((line = reader.readLine()) != null) {
                    line = safe(line);
                    if (line.isEmpty()) continue;

                    List<String> cols = splitCsv(line, sep);
                    if (cols.size() <= idxNome) continue;

                    String rawLoja   = get(cols, idxLoja);
                    String rawCodigo = get(cols, idxCodigo);
                    String rawNome   = get(cols, idxNome);

                    String loja           = normalizeNumeroLoja(rawLoja);
                    String codlocalizacao = normalizeCodigo(rawCodigo);
                    String setor          = limparNomeSetor(rawNome);

                    if (codlocalizacao.isEmpty() || setor.isEmpty()) continue;

                    lista.add(new SetorLocalizacao(loja, codlocalizacao, setor));
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Erro ao importar setores", e);
        }

        return lista;
    }

    /**
     * (Mantido) mapa por codlocalizacao APENAS.
     * Atenção: isso mistura lojas se existir SEQLOCAL repetido entre lojas.
     */
    public static Map<String, String> toMap(List<SetorLocalizacao> setores) {
        Map<String, String> mapa = new HashMap<>();
        if (setores == null) return mapa;

        for (SetorLocalizacao s : setores) {
            if (s == null) continue;
            String cod  = normalizeCodigo(s.codlocalizacao);
            String nome = safe(s.setor);
            if (!cod.isEmpty() && !nome.isEmpty()) {
                mapa.put(cod, nome);
            }
        }
        return mapa;
    }

    /**
     * (NOVO) mapa por chave composta: "loja|codlocalizacao"
     * Isso evita misturar setores de lojas diferentes.
     */
    public static Map<String, String> toMapLojaCodigo(List<SetorLocalizacao> setores) {
        Map<String, String> mapa = new HashMap<>();
        if (setores == null) return mapa;

        for (SetorLocalizacao s : setores) {
            if (s == null) continue;

            String loja = normalizeNumeroLoja(s.loja);
            String cod  = normalizeCodigo(s.codlocalizacao);
            String nome = safe(s.setor);

            if (!loja.isEmpty() && !cod.isEmpty() && !nome.isEmpty()) {
                mapa.put(loja + "|" + cod, nome);
            }
        }
        return mapa;
    }

    /**
     * (NOVO) Retorna nomes de setores únicos da loja (bom pra Spinner/Dropdown).
     */
    public static List<String> setoresUnicosPorLoja(List<SetorLocalizacao> setores, String lojaSelecionada) {
        List<String> out = new ArrayList<>();
        if (setores == null) return out;

        String loja = normalizeNumeroLoja(lojaSelecionada);
        Set<String> uniq = new LinkedHashSet<>();

        for (SetorLocalizacao s : setores) {
            if (s == null) continue;
            String lojaItem = normalizeNumeroLoja(s.loja);
            if (!loja.equals(lojaItem)) continue;

            String nome = safe(s.setor);
            if (!nome.isEmpty()) uniq.add(nome);
        }

        out.addAll(uniq);
        return out;
    }

    // ------------------- auxiliares -------------------

    private static String get(List<String> cols, int index) {
        if (index < 0 || index >= cols.size()) return "";
        String v = cols.get(index);
        return v == null ? "" : v;
    }

    private static int indexOfIgnoreCase(List<String> cols, String name) {
        if (cols == null || name == null) return -1;
        for (int i = 0; i < cols.size(); i++) {
            String c = cols.get(i);
            if (c != null && c.trim().equalsIgnoreCase(name)) return i;
        }
        return -1;
    }

    private static String safe(String v) {
        return v == null ? "" : v.trim();
    }

    private static String normalizeNumeroLoja(String raw) {
        String s = safe(raw).replace("\u00A0", "");
        if (s.matches("^\\d+$")) s = s.replaceFirst("^0+(?!$)", "");
        return s;
    }

    private static String normalizeCodigo(String raw) {
        String s = safe(raw).replace("\u00A0", "");
        if (s.matches("^\\d+$")) s = s.replaceFirst("^0+(?!$)", "");
        return s;
    }

    private static String limparNomeSetor(String raw) {
        String s = safe(raw);

        while (s.contains("  ")) s = s.replace("  ", " ");
        s = s.trim();

        if (s.startsWith("LOJA >")) {
            s = s.substring("LOJA >".length()).trim();
        }

        String matrizPattern = "MATRIZ > MATRIZ";
        if (s.equalsIgnoreCase(matrizPattern)) {
            s = "MATRIZ";
        } else if (s.startsWith("MATRIZ >")) {
            s = s.substring("MATRIZ >".length()).trim();
        }

        return s;
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
        if (s == null) return 0;
        for (int i = 0; i < s.length(); i++) {
            if (s.charAt(i) == ch) n++;
        }
        return n;
    }

    private static List<String> splitCsv(String line, char sep) {
        List<String> out = new ArrayList<>();
        if (line == null) return out;

        if (line.indexOf('"') < 0) {
            int start = 0;
            int len = line.length();
            for (int i = 0; i < len; i++) {
                if (line.charAt(i) == sep) {
                    out.add(line.substring(start, i));
                    start = i + 1;
                }
            }
            out.add(line.substring(start));
            return out;
        }

        StringBuilder sb = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char ch = line.charAt(i);
            if (ch == '"') {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    sb.append('"');
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
