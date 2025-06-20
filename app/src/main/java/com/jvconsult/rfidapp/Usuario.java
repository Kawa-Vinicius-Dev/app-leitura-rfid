// Usuario.java
package com.jvconsult.rfidapp;

public class Usuario {
    public int id;
    public String nome;
    public String senha;

    public Usuario(int id, String nome, String senha) {
        this.id = id;
        this.nome = nome;
        this.senha = senha;
    }

    // Construtor sem id pra cadastrar novo usuário
    public Usuario(String nome, String senha) {
        this.nome = nome;
        this.senha = senha;
    }
}
