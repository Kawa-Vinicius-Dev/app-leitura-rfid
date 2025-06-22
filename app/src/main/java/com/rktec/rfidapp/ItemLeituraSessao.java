package com.rktec.rfidapp;

public class ItemLeituraSessao {
    public String epc;
    public ItemPlanilha item; // se encontrado na planilha
    public boolean encontrado;

    public ItemLeituraSessao(String epc, ItemPlanilha item) {
        this.epc = epc;
        this.item = item;
        this.encontrado = (item != null);
    }
}
