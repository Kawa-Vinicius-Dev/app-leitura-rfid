package com.jvconsult.rfidapp;

import java.util.List;

public class DadosGlobais {
    private static DadosGlobais instance;

    // --- dados importados ---
    private List<ItemPlanilha> listaPlanilha;
    private List<SetorLocalizacao> listaSetores;

    // --- seleção corrente ---
    private String lojaSelecionada;
    private SetorLocalizacao setorSelecionado;

    // --- usuário logado ---
    private String usuario;

    private DadosGlobais() {}

    public void resetar() {
        listaPlanilha = null;
        listaSetores = null;
        lojaSelecionada = null;
        setorSelecionado = null;
        usuario = null;
    }

    public static DadosGlobais getInstance() {
        if (instance == null) instance = new DadosGlobais();
        return instance;
    }

    // -------- getters / setters --------
    public List<ItemPlanilha> getListaPlanilha() { return listaPlanilha; }
    public void setListaPlanilha(List<ItemPlanilha> l) { this.listaPlanilha = l; }

    public List<SetorLocalizacao> getListaSetores() { return listaSetores; }
    public void setListaSetores(List<SetorLocalizacao> s) { this.listaSetores = s; }

    public String getLojaSelecionada() { return lojaSelecionada; }
    public void setLojaSelecionada(String loja) { this.lojaSelecionada = loja; }

    public SetorLocalizacao getSetorSelecionado() { return setorSelecionado; }
    public void setSetorSelecionado(SetorLocalizacao setor) { this.setorSelecionado = setor; }

    public String getUsuario() { return usuario; }
    public void setUsuario(String usuario) { this.usuario = usuario; }
}
