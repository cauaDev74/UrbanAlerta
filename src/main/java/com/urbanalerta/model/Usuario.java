package com.urbanalerta.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Pessoa que usa o sistema. O perfil define o que ela pode fazer:
 * CLIENTE registra ocorrências; ADMIN altera o status delas.
 * Nunca guardamos a senha, apenas o hash (veja SenhaService).
 */
@Entity
@Table(name = "usuario")
public class Usuario {

    @Id
    private UUID id;

    @Column(nullable = false, length = 100)
    private String nome;

    @Column(nullable = false, unique = true, length = 150)
    private String email;

    @Column(name = "senha_hash", nullable = false, length = 200)
    private String senhaHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PerfilUsuario perfil;

    @Column(name = "criado_em", nullable = false)
    private LocalDateTime criadoEm;

    @Version
    private Long versao;

    /** Construtor exigido pelo JPA. */
    protected Usuario() {
    }

    private Usuario(UUID id, String nome, String email, String senhaHash, PerfilUsuario perfil) {
        this.id = id;
        this.nome = nome;
        this.email = email;
        this.senhaHash = senhaHash;
        this.perfil = perfil;
        this.criadoEm = LocalDateTime.now();
    }

    public static Usuario novo(String nome, String email, String senhaHash, PerfilUsuario perfil) {
        return new Usuario(UUID.randomUUID(), nome, email, senhaHash, perfil);
    }

    public boolean isAdmin() {
        return perfil == PerfilUsuario.ADMIN;
    }

    public UUID getId() { return id; }
    public String getNome() { return nome; }
    public String getEmail() { return email; }
    public String getSenhaHash() { return senhaHash; }
    public PerfilUsuario getPerfil() { return perfil; }
    public LocalDateTime getCriadoEm() { return criadoEm; }
}
