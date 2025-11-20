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
import java.util.List;
import java.util.Map;

/**
 * Importa a planilha de SETORES.
 *
 * Formato esperado do arquivo texto:
 *
 *  NROEMPRESA;SEQLOCAL;CAMINHOLOCALIZACAO
 *  1;2291;LOJA > LJ - ANTE SALA DA TESOURARIA
 *  1;1288;LOJA > LJ - ATENDIMENTO DE ATACADO
 *  ...
 *
 * Regras de tratamento:
 *  - NROEMPRESA: índice numérico da loja → remove zeros à esquerda ("001" -> "1").
 *  - SEQLOCAL: código da localização → normaliza (trim, NBSP, zeros à esquerda).
 *  - CAMINHOLOCALIZACAO:
 *      • remove "LOJA > " do início;
 *      • "MATRIZ > MATRIZ" vira apenas "MATRIZ";
 *      • para "MATRIZ > X", remove "MATRIZ > " e mantém "X".
 */
public class ImportadorSetor {

    /**
     * Lê o arquivo de setores e retorna uma lista de SetorLocalizacao.
     */
    public static List<SetorLocalizacao> importar(Context context, Uri fileUri) {
        List<SetorLocalizacao> lista = new ArrayList<>();
        if (context == null || fileUri == null) return lista;

        BufferedReader reader = null;
        try {
            InputStream is = context.getContentResolver().openInputStream(fileUri);
            if (is == null) return lista;

            // Charset comum para arquivo exportado de sistema legado
            reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.ISO_8859_1));

            String header = reader.readLine();
            if (header == null) return lista;

            char sep = detectSeparator(header);
            List<String> headerCols = splitCsv(header, sep);
            int idxLoja   = indexOfIgnoreCase(headerCols, "NROEMPRESA");
            int idxCodigo = indexOfIgnoreCase(headerCols, "SEQLOCAL");
            int idxNome   = indexOfIgnoreCase(headerCols, "CAMINHOLOCALIZACAO");

            // fallback se o header estiver diferente
            if (idxLoja   < 0) idxLoja   = 0;
            if (idxCodigo < 0) idxCodigo = 1;
            if (idxNome   < 0) idxNome   = 2;

            String line;
            while ((line = reader.readLine()) != null) {
                line = safe(line);
                if (line.isEmpty()) continue;

                List<String> cols = splitCsv(line, sep);
                if (cols.size() <= idxNome) continue; // linha incompleta

                String rawLoja   = get(cols, idxLoja);
                String rawCodigo = get(cols, idxCodigo);
                String rawNome   = get(cols, idxNome);

                String loja          = normalizeNumeroLoja(rawLoja);
                String codlocalizacao = normalizeCodigo(rawCodigo);
                String setor         = limparNomeSetor(rawNome);

                if (codlocalizacao.isEmpty() || setor.isEmpty()) {
                    continue;
                }

                lista.add(new SetorLocalizacao(loja, codlocalizacao, setor));
            }
        } catch (Exception e) {
            Log.e("ImportadorSetor", "Erro ao importar setores", e);
        } finally {
            if (reader != null) {
                try { reader.close(); } catch (Exception ignored) {}
            }
        }
        return lista;
    }

    /**
     * Constrói um mapa código -> nome do setor a partir da lista importada.
     * Usado em MapeadorSetor.aplicar().
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
            if (c != null && c.trim().equalsIgnoreCase(name)) {
                return i;
            }
        }
        return -1;
    }

    private static String safe(String v) {
        return v == null ? "" : v.trim();
    }

    /** Normaliza número da loja removendo zeros à esquerda ("001" -> "1"). */
    private static String normalizeNumeroLoja(String raw) {
        String s = safe(raw).replace("\u00A0", ""); // NBSP
        if (s.matches("^\\d+$")) {
            s = s.replaceFirst("^0+(?!$)", "");
        }
        return s;
    }

    /** Normaliza código de localização. */
    private static String normalizeCodigo(String raw) {
        String s = safe(raw).replace("\u00A0", "");
        if (s.matches("^\\d+$")) {
            s = s.replaceFirst("^0+(?!$)", "");
        }
        return s;
    }

    /**
     * Tratamento do nome do setor:
     *  - remove "LOJA > ";
     *  - "MATRIZ > MATRIZ" → "MATRIZ";
     *  - "MATRIZ > X" → "X".
     */
    private static String limparNomeSetor(String raw) {
        String s = safe(raw);

        // compacta espaços
        while (s.contains("  ")) {
            s = s.replace("  ", " ");
        }
        s = s.trim();

        // LOJA >
        if (s.startsWith("LOJA >")) {
            s = s.substring("LOJA >".length()).trim();
        }

        // MATRIZ >
        String matrizPattern = "MATRIZ > MATRIZ";
        if (s.equalsIgnoreCase(matrizPattern)) {
            s = "MATRIZ";
        } else if (s.startsWith("MATRIZ >")) {
            s = s.substring("MATRIZ >".length()).trim();
        }

        return s;
    }

    /** Detecta separador mais provável na primeira linha. */
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

    /**
     * Split de CSV simples com suporte a aspas.
     * Ex: a;"b;c";d  →  ["a","b;c","d"]
     */
    private static List<String> splitCsv(String line, char sep) {
        List<String> out = new ArrayList<>();
        if (line == null) return out;

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
