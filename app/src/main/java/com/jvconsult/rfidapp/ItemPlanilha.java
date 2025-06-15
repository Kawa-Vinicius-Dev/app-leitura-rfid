package com.jvconsult.rfidapp;

public class ItemPlanilha {
    public String loja, sqbem, codgrupo, codlocalizacao, nrobem, nroincorp,
            descresumida, descdetalhada, qtdbem, nroplaqueta, nroseriebem, modelobem;

    public ItemPlanilha(String loja, String sqbem, String codgrupo, String codlocalizacao, String nrobem,
                        String nroincorp, String descresumida, String descdetalhada, String qtdbem,
                        String nroplaqueta, String nroseriebem, String modelobem) {
        this.loja = loja;
        this.sqbem = sqbem;
        this.codgrupo = codgrupo;
        this.codlocalizacao = codlocalizacao;
        this.nrobem = nrobem;
        this.nroincorp = nroincorp;
        this.descresumida = descresumida;
        this.descdetalhada = descdetalhada;
        this.qtdbem = qtdbem;
        this.nroplaqueta = nroplaqueta;
        this.nroseriebem = nroseriebem;
        this.modelobem = modelobem;
    }
}
