package com.rktec.rfidapp;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MapeadorSetor {

    /**
     * Substitui item.codlocalizacao pelo nome do setor mapeado.
     * Prioriza chave composta "loja|cod" para não misturar lojas.
     * Mantém fallback por "cod" para compatibilidade.
     */
    public static void aplicar(List<ItemPlanilha> itens, Map<String, String> mapa) {
        if (itens == null || itens.isEmpty() || mapa == null || mapa.isEmpty()) return;

        // Cache por (loja|codigo original) -> nome encontrado (ou "" se não encontrou)
        Map<String, String> cache = new HashMap<>();

        for (ItemPlanilha item : itens) {
            if (item == null) continue;

            String lojaRaw = safe(item.loja);
            String codRaw  = safe(item.codlocalizacao);

            if (codRaw.isEmpty()) continue;

            String lojaNorm = normalizeNumero(lojaRaw);
            String codNorm  = normalizeCodigo(codRaw);

            // chave de cache usando lojaNorm (pode ser vazio) + codRaw (original)
            String cacheKey = lojaNorm + "|" + codRaw;

            String nomeCache = cache.get(cacheKey);
            if (nomeCache != null) {
                if (!nomeCache.isEmpty()) item.codlocalizacao = nomeCache;
                continue;
            }

            String nome = null;

            // 1) tenta pelo formato novo: "loja|cod"
            if (!lojaNorm.isEmpty()) {
                nome = buscarNoMapa(mapa, lojaNorm + "|" + codNorm);
            }

            // 2) fallback compatível: só "cod"
            if (nome == null) {
                nome = buscarNoMapa(mapa, codNorm);
            }

            if (nome != null && !nome.isEmpty()) {
                item.codlocalizacao = nome;
                cache.put(cacheKey, nome);
            } else {
                cache.put(cacheKey, "");
            }
        }
    }

    /** Busca no mapa tentando variações com zeros à esquerda (compatibilidade). */
    private static String buscarNoMapa(Map<String, String> mapa, String chave) {
        if (chave == null) return null;

        String nome = mapa.get(chave);
        if (nome != null) return nome;

        // Se a chave tem formato loja|cod, só aplica zeros na parte do cod
        int p = chave.indexOf('|');
        if (p >= 0) {
            String loja = chave.substring(0, p);
            String cod  = chave.substring(p + 1);
            String alt3 = loja + "|" + addLeadingZeros(cod, 3);
            nome = mapa.get(alt3);
            if (nome != null) return nome;

            String alt2 = loja + "|" + addLeadingZeros(cod, 2);
            return mapa.get(alt2);
        }

        // chave simples (somente cod)
        String alt3 = addLeadingZeros(chave, 3);
        nome = mapa.get(alt3);
        if (nome != null) return nome;

        String alt2 = addLeadingZeros(chave, 2);
        return mapa.get(alt2);
    }

    private static String addLeadingZeros(String v, int width) {
        if (v == null) return "";
        int len = v.length();
        if (len == 0) return v;

        for (int i = 0; i < len; i++) {
            char ch = v.charAt(i);
            if (ch < '0' || ch > '9') return v;
        }

        if (len >= width) return v;

        StringBuilder sb = new StringBuilder(width);
        for (int i = len; i < width; i++) sb.append('0');
        sb.append(v);
        return sb.toString();
    }

    private static String normalizeNumero(String raw) {
        String s = safe(raw).replace('\u00A0', ' ');
        if (s.isEmpty()) return s;

        // só dígitos?
        int len = s.length();
        for (int i = 0; i < len; i++) {
            char ch = s.charAt(i);
            if (ch < '0' || ch > '9') return s.trim();
        }

        int idx = 0;
        while (idx < len - 1 && s.charAt(idx) == '0') idx++;
        return s.substring(idx).trim();
    }

    private static String normalizeCodigo(String raw) {
        String s = safe(raw).replace('\u00A0', ' ');
        if (s.isEmpty()) return s;

        int len = s.length();
        for (int i = 0; i < len; i++) {
            char ch = s.charAt(i);
            if (ch < '0' || ch > '9') return s.trim();
        }

        int idx = 0;
        while (idx < len - 1 && s.charAt(idx) == '0') idx++;
        return s.substring(idx).trim();
    }

    private static String safe(String v) {
        return v == null ? "" : v.trim();
    }
}
