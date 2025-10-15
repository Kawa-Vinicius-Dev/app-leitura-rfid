package com.rktec.rfidapp;

import java.util.List;
import java.util.Map;

public class MapeadorSetor {

    /** Substitui item.codlocalizacao pelo nome mapeado. Mantém código original se não achar. */
    public static void aplicar(List<ItemPlanilha> itens, Map<String, String> mapa) {
        if (itens == null || itens.isEmpty() || mapa == null || mapa.isEmpty()) return;

        for (ItemPlanilha item : itens) {
            if (item == null) continue;
            String original = safe(item.codlocalizacao);
            if (original.isEmpty()) continue;

            String chave = normalizeCodigo(original);
            String nome = mapa.get(chave);
            if (nome == null) {
                // tentativa com zeros à esquerda (ex.: width 3)
                String alt3 = addLeadingZeros(chave, 3);
                nome = mapa.get(alt3);
                if (nome == null) {
                    String alt2 = addLeadingZeros(chave, 2);
                    nome = mapa.get(alt2);
                }
            }
            if (nome != null && !nome.isEmpty()) {
                item.codlocalizacao = nome; // troca código pelo rótulo amigável
            }
        }
    }

    private static String addLeadingZeros(String v, int width) {
        if (!v.matches("^\\d+$")) return v;
        try { return String.format("%0" + width + "d", Integer.parseInt(v)); }
        catch (Exception e) { return v; }
    }
    private static String normalizeCodigo(String raw) {
        String s = safe(raw).replace("\u00A0", "");
        if (s.matches("^\\d+$")) s = s.replaceFirst("^0+(?!$)", "");
        return s;
    }
    private static String safe(String v) { return v == null ? "" : v.trim(); }
}
