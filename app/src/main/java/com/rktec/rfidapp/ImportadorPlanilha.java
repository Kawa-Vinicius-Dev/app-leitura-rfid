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

    private static final String TAG = "ImportadorPlanilha";

    public static List<ItemPlanilha> importar(Context context, Uri fileUri) {
        List<ItemPlanilha> lista = new ArrayList<>();
        if (context == null || fileUri == null) return lista;

        BufferedReader reader = null;
        try {
            InputStream is = context.getContentResolver().openInputStream(fileUri);
            if (is == null) return lista;

            // Mesmo esquema do setor: ISO-8859-1 pra planilha legada
            reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.ISO_8859_1));

            String header = reader.readLine();
            if (header == null) return lista;

            char sep = detectSeparator(header);
            List<String> headerCols = splitCsv(header, sep);

            int idxLoja         = indexOfIgnoreCase(headerCols, "LOJA");
            int idxSeqBem       = indexOfIgnoreCase(headerCols, "SEQBEM");
            int idxCodGrupo     = indexOfIgnoreCase(headerCols, "CODGRUPO");
            int idxCodLocal     = indexOfIgnoreCase(headerCols, "CODLOCALIZACAO");
            int idxNroBem       = indexOfIgnoreCase(headerCols, "NROBEM");
            int idxNroIncorp    = indexOfIgnoreCase(headerCols, "NROINCORP");
            int idxDescRes      = indexOfIgnoreCase(headerCols, "DESCRESUMIDA");
            int idxDescDet      = indexOfIgnoreCase(headerCols, "DESCDETALHADA");
            int idxQtdBem       = indexOfIgnoreCase(headerCols, "QTDBEM");
            int idxNroPlaqueta  = indexOfIgnoreCase(headerCols, "NROPLAQUETA");
            int idxNroSerie     = indexOfIgnoreCase(headerCols, "NROSERIEBEM");
            int idxModelo       = indexOfIgnoreCase(headerCols, "MODELOBEM");

            // Fallback simples caso o header venha estranho
            if (idxLoja        < 0) idxLoja        = 0;
            if (idxSeqBem      < 0) idxSeqBem      = 1;
            if (idxCodGrupo    < 0) idxCodGrupo    = 2;
            if (idxCodLocal    < 0) idxCodLocal    = 3;
            if (idxNroBem      < 0) idxNroBem      = 4;
            if (idxNroIncorp   < 0) idxNroIncorp   = 5;
            if (idxDescRes     < 0) idxDescRes     = 6;
            if (idxDescDet     < 0) idxDescDet     = 7;
            if (idxQtdBem      < 0) idxQtdBem      = 8;
            if (idxNroPlaqueta < 0) idxNroPlaqueta = 9;
            if (idxNroSerie    < 0) idxNroSerie    = 10;
            if (idxModelo      < 0) idxModelo      = 11;

            // Aqui vem a mágica: juntar linhas quebradas
            String current = null;
            String line;
            while ((line = reader.readLine()) != null) {
                if (line == null) break;

                // remove NBSP e espaços extras de borda
                String trimmed = safe(line);
                if (trimmed.isEmpty()) {
                    continue;
                }

                if (current == null) {
                    // Primeiro registro
                    current = trimmed;
                    continue;
                }

                if (isInicioDeRegistro(trimmed)) {
                    // A linha atual parece o início de uma nova loja (ex.: "001 CARPINA", "500-MATRIZ")
                    // então fechamos o registro anterior e começamos um novo
                    adicionarItem(lista, current, sep,
                            idxLoja, idxSeqBem, idxCodGrupo, idxCodLocal,
                            idxNroBem, idxNroIncorp, idxDescRes, idxDescDet,
                            idxQtdBem, idxNroPlaqueta, idxNroSerie, idxModelo);

                    current = trimmed;
                } else {
                    // NÃO é início de registro → é continuação de texto
                    // Exemplo clássico: "DESCRIÇÃO DO I" + "\n" + "TEM"
                    current = current + " " + trimmed;
                }
            }

            // Flush do último registro
            if (current != null) {
                adicionarItem(lista, current, sep,
                        idxLoja, idxSeqBem, idxCodGrupo, idxCodLocal,
                        idxNroBem, idxNroIncorp, idxDescRes, idxDescDet,
                        idxQtdBem, idxNroPlaqueta, idxNroSerie, idxModelo);
            }

            Log.d(TAG, "Itens importados: " + lista.size());
        } catch (Exception e) {
            Log.e(TAG, "Erro ao importar planilha", e);
        } finally {
            if (reader != null) {
                try { reader.close(); } catch (Exception ignored) {}
            }
        }

        return lista;
    }

    // ----------------------------------------------------
    //  Helpers de parsing / detecção de início de registro
    // ----------------------------------------------------

    /**
     * Detecta se a linha parece ser o INÍCIO de um novo registro da planilha.
     *
     * Aqui usamos o padrão da sua planilha:
     *   "001 CARPINA"
     *   "001-CARPINA"
     *   "500-MATRIZ"
     *   "999-CD SECOS"
     *
     * Ou seja: começa com 3 dígitos.
     */
    private static boolean isInicioDeRegistro(String line) {
        if (line == null) return false;
        String s = line.trim();
        if (s.length() < 3) return false;

        char c0 = s.charAt(0);
        char c1 = s.charAt(1);
        char c2 = s.charAt(2);

        return Character.isDigit(c0) && Character.isDigit(c1) && Character.isDigit(c2);
    }

    private static void adicionarItem(
            List<ItemPlanilha> lista,
            String rawLine,
            char sep,
            int idxLoja,
            int idxSeqBem,
            int idxCodGrupo,
            int idxCodLocal,
            int idxNroBem,
            int idxNroIncorp,
            int idxDescRes,
            int idxDescDet,
            int idxQtdBem,
            int idxNroPlaqueta,
            int idxNroSerie,
            int idxModelo
    ) {
        if (rawLine == null) return;
        String line = rawLine.trim();
        if (line.isEmpty()) return;

        List<String> cols = splitCsv(line, sep);

        // Garante que temos pelo menos colunas suficientes
        int minIndex = max(
                idxLoja, idxSeqBem, idxCodGrupo, idxCodLocal,
                idxNroBem, idxNroIncorp, idxDescRes, idxDescDet,
                idxQtdBem, idxNroPlaqueta, idxNroSerie, idxModelo
        );

        if (cols.size() <= minIndex) {
            Log.w(TAG, "Linha ignorada (colunas insuficientes): " + line);
            return;
        }

        String loja        = safe(get(cols, idxLoja));
        String seqBem      = safe(get(cols, idxSeqBem));
        String codGrupo    = safe(get(cols, idxCodGrupo));
        String codLocal    = safe(get(cols, idxCodLocal));
        String nroBem      = safe(get(cols, idxNroBem));
        String nroIncorp   = safe(get(cols, idxNroIncorp));
        String descRes     = safe(get(cols, idxDescRes));
        String descDet     = safe(get(cols, idxDescDet));
        String qtdBem      = safe(get(cols, idxQtdBem));
        String nroPlaqueta = safe(get(cols, idxNroPlaqueta));
        String nroSerie    = safe(get(cols, idxNroSerie));
        String modelo      = safe(get(cols, idxModelo));

        ItemPlanilha item = new ItemPlanilha(
                loja,
                seqBem,
                codGrupo,
                codLocal,
                nroBem,
                nroIncorp,
                descRes,
                descDet,
                qtdBem,
                nroPlaqueta,
                nroSerie,
                modelo
        );
        lista.add(item);
    }

    private static int max(int... values) {
        int m = values[0];
        for (int v : values) {
            if (v > m) m = v;
        }
        return m;
    }

    // ------------------- utilitários genéricos -------------------

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
        if (v == null) return "";
        // troca NBSP por espaço normal e trim
        return v.replace('\u00A0', ' ').trim();
    }

    /** Detecta o separador mais provável na linha de cabeçalho. */
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
