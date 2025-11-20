package com.rktec.rfidapp;

public class ItemLeituraSessao {

    // Status de comparação entre a leitura e a planilha
    public static final int STATUS_OK = 0;              // Loja e setor corretos
    public static final int STATUS_SETOR_ERRADO = 1;    // Loja correta, setor diferente
    public static final int STATUS_LOJA_ERRADA = 2;     // Item existe, mas está em outra loja
    public static final int STATUS_NAO_ENCONTRADO = 3;  // EPC não encontrado na base

    public String epc;
    public ItemPlanilha item; // se encontrado na planilha (ou cadastrado manualmente)
    public boolean encontrado; // mantido por compatibilidade com código antigo
    public int status;         // um dos STATUS_*

    public ItemLeituraSessao(String epc, ItemPlanilha item) {
        this.epc = epc;
        this.item = item;
        this.encontrado = (item != null);
        // Valor inicial; a LeituraActivity vai recalcular com base em loja/setor
        this.status = (item != null) ? STATUS_OK : STATUS_NAO_ENCONTRADO;
    }
}
