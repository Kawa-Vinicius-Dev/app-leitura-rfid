// Usuario.java
package com.rktec.rfidapp;

public class Usuario {
    public int id;
    public String nome;
    public String senha;
    public String permissao; // <- AQUI!

    public Usuario(int id, String nome, String senha, String permissao) {
        this.id = id;
        this.nome = nome;
        this.senha = senha;
        this.permissao = permissao;
    }

    // Construtor sem id pra cadastrar novo usuário
    public Usuario(String nome, String senha, String permissao) {
        this.nome = nome;
        this.senha = senha;
        this.permissao = permissao;
    }
}
