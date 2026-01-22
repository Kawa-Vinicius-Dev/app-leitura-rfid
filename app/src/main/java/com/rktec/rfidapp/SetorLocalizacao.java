package com.rktec.rfidapp;

/**
 * Representa o vínculo entre LOJA, código de localização e nome do setor.
 *
 *  - loja: índice numérico da loja (sem zeros à esquerda), ex.: "1", "2"...
 *  - codlocalizacao: código de localização (SEQLOCAL).
 *  - setor: nome já tratado, ex.: "LJ - PADARIA", "MATRIZ".
 */
public class SetorLocalizacao {
    public String loja;
    public String codlocalizacao;
    public String setor;

    // Construtor antigo (compatibilidade)
    public SetorLocalizacao(String codlocalizacao, String setor) {
        this(null, codlocalizacao, setor);
    }

    // Novo construtor completo
    public SetorLocalizacao(String loja, String codlocalizacao, String setor) {
        this.loja = loja;
        this.codlocalizacao = codlocalizacao;
        this.setor = setor;
    }
}
