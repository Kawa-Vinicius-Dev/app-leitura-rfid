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

    /**
     * Cabeçalho esperado (na ordem que você mandou):
     *
     * EMPRESA;NOTA_FISCAL;SERIE;FORNECEDOR;DTA_EMISSAO;DTA_AQUISICAO;DTA_INCORP;
     * TIPO_MOVIMENTO;CONTA;DESCRICAO_CONTA;SEQBEM;NROBEM;LOCALIZACAO;NROPLAQUETA;
     * SEQPRODUTO;DESCRICAO;DESC_BEM;MODELO;SERIE;QTDE;NROITEM;CUSTO_AQUISICAO;
     * CUSTO_CORRIGIDO;PERCCONTAB;DPR_ACUMULADA;VLR_CONTAB_LIQ
     *
     * Mapeamento para ItemPlanilha:
     *  - loja          ← EMPRESA        (normaliza zeros à esquerda)
     *  - codgrupo      ← CONTA
     *  - sqbem         ← SEQBEM
     *  - nrobem        ← NROBEM
     *  - codlocalizacao← LOCALIZACAO    (setor)
     *  - nroplaqueta   ← NROPLAQUETA    (normaliza zeros à esquerda)
     *  - descresumida  ← DESCRICAO
     *  - descdetalhada ← DESC_BEM
     *  - modelobem     ← MODELO
     *  - nroseriebem   ← SERIE
     *  - qtdbem        ← QTDE
     */
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

            // --- Índices baseados no cabeçalho REAL da planilha ---
            int idxEmpresa     = indexOfIgnoreCase(headerCols, "EMPRESA");
            int idxConta       = indexOfIgnoreCase(headerCols, "CONTA");
            int idxSeqBem      = indexOfIgnoreCase(headerCols, "SEQBEM");
            int idxNroBem      = indexOfIgnoreCase(headerCols, "NROBEM");
            int idxLocalizacao = indexOfIgnoreCase(headerCols, "LOCALIZACAO");
            int idxNroPlaqueta = indexOfIgnoreCase(headerCols, "NROPLAQUETA");
            int idxDescricao   = indexOfIgnoreCase(headerCols, "DESCRICAO");
            int idxDescBem     = indexOfIgnoreCase(headerCols, "DESC_BEM");
            int idxModelo      = indexOfIgnoreCase(headerCols, "MODELO");
            int idxSerieBem    = indexOfIgnoreCase(headerCols, "SERIE");
            int idxQtde        = indexOfIgnoreCase(headerCols, "QTDE");

            // Fallback para posição fixa se algum não for encontrado
            // (com base na ordem que você mandou)
            if (idxEmpresa     < 0) idxEmpresa     = 0;
            if (idxConta       < 0) idxConta       = 8;
            if (idxSeqBem      < 0) idxSeqBem      = 10;
            if (idxNroBem      < 0) idxNroBem      = 11;
            if (idxLocalizacao < 0) idxLocalizacao = 12;
            if (idxNroPlaqueta < 0) idxNroPlaqueta = 13;
            if (idxDescricao   < 0) idxDescricao   = 15;
            if (idxDescBem     < 0) idxDescBem     = 16;
            if (idxModelo      < 0) idxModelo      = 17;
            if (idxSerieBem    < 0) idxSerieBem    = 18;
            if (idxQtde        < 0) idxQtde        = 19;

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
                    adicionarItem(
                            lista,
                            current,
                            sep,
                            idxEmpresa,
                            idxSeqBem,
                            idxConta,
                            idxLocalizacao,
                            idxNroBem,
                            /* idxNroIncorp (não temos coluna específica, deixa -1 no uso) */ -1,
                            idxDescricao,
                            idxDescBem,
                            idxQtde,
                            idxNroPlaqueta,
                            idxSerieBem,
                            idxModelo
                    );

                    current = trimmed;
                } else {
                    // NÃO é início de registro → é continuação de texto
                    // Exemplo clássico: "DESCRIÇÃO DO I" + "\n" + "TEM"
                    current = current + " " + trimmed;
                }
            }

            // Flush do último registro
            if (current != null) {
                adicionarItem(
                        lista,
                        current,
                        sep,
                        idxEmpresa,
                        idxSeqBem,
                        idxConta,
                        idxLocalizacao,
                        idxNroBem,
                        -1,
                        idxDescricao,
                        idxDescBem,
                        idxQtde,
                        idxNroPlaqueta,
                        idxSerieBem,
                        idxModelo
                );
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
            int idxEmpresa,      // EMPRESA -> loja
            int idxSeqBem,       // SEQBEM
            int idxConta,        // CONTA -> codgrupo
            int idxLocalizacao,  // LOCALIZACAO -> codlocalizacao (setor)
            int idxNroBem,       // NROBEM
            int idxNroIncorp,    // (não temos, normalmente -1)
            int idxDescricao,    // DESCRICAO -> descresumida
            int idxDescBem,      // DESC_BEM -> descdetalhada
            int idxQtde,         // QTDE -> qtdbem
            int idxNroPlaqueta,  // NROPLAQUETA -> nroplaqueta
            int idxSerieBem,     // SERIE -> nroseriebem
            int idxModelo        // MODELO -> modelobem
    ) {
        if (rawLine == null) return;
        String line = rawLine.trim();
        if (line.isEmpty()) return;

        List<String> cols = splitCsv(line, sep);

        // Garante que temos pelo menos colunas suficientes
        int minIndex = max(
                idxEmpresa, idxSeqBem, idxConta, idxLocalizacao,
                idxNroBem, idxNroIncorp, idxDescricao, idxDescBem,
                idxQtde, idxNroPlaqueta, idxSerieBem, idxModelo
        );

        if (cols.size() <= minIndex) {
            Log.w(TAG, "Linha ignorada (colunas insuficientes): " + line);
            return;
        }

        String loja        = normalizeLoja(safe(get(cols, idxEmpresa)));
        String seqBem      = safe(get(cols, idxSeqBem));
        String codGrupo    = safe(get(cols, idxConta));
        String codLocal    = safe(get(cols, idxLocalizacao));
        String nroBem      = safe(get(cols, idxNroBem));
        String nroIncorp   = (idxNroIncorp >= 0 ? safe(get(cols, idxNroIncorp)) : "");
        String descRes     = safe(get(cols, idxDescricao));
        String descDet     = safe(get(cols, idxDescBem));
        String qtdBem      = safe(get(cols, idxQtde));
        String nroPlaqueta = normalizePlaqueta(safe(get(cols, idxNroPlaqueta)));
        String nroSerie    = safe(get(cols, idxSerieBem));
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

    /** Normaliza número da loja removendo zeros à esquerda ("001" -> "1"). */
    private static String normalizeLoja(String raw) {
        String s = safe(raw);
        if (s.matches("^\\d+$")) {
            s = s.replaceFirst("^0+(?!$)", "");
        }
        return s;
    }

    /** Normaliza plaqueta, removendo zeros à esquerda. */
    private static String normalizePlaqueta(String raw) {
        String s = safe(raw);
        if (s.matches("^\\d+$")) {
            s = s.replaceFirst("^0+(?!$)", "");
        }
        return s;
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
