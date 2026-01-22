package com.rktec.rfidapp;

public class ItemLeituraSessao {

    // Status
    public static final int STATUS_OK = 0;
    public static final int STATUS_SETOR_ERRADO = 1;
    public static final int STATUS_LOJA_ERRADA = 2;
    public static final int STATUS_NAO_ENCONTRADO = 3;

    public String epc;
    public ItemPlanilha item;
    public boolean encontrado;
    public int status;

    // 🔹 NOVOS CAMPOS (BASE GERAL)
    public String setorAntigo; // setor real na base
    public String lojaAntiga;  // loja real na base

    public ItemLeituraSessao(String epc, ItemPlanilha item) {
        this.epc = epc;
        this.item = item;
        this.encontrado = (item != null);
        this.status = (item != null) ? STATUS_OK : STATUS_NAO_ENCONTRADO;

        // valores iniciais
        this.setorAntigo = null;
        this.lojaAntiga = null;
    }
}
