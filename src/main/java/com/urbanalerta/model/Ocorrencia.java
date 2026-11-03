package com.urbanalerta.model;

import com.urbanalerta.exception.TransicaoInvalidaException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

/**
 * Um problema de infraestrutura urbana registrado por um cliente e resolvido por um administrador.
 * É uma entidade JPA (tabela "ocorrencia"); as regras do negócio (ciclo de status, dias em aberto)
 * ficam aqui dentro.
 */
@Entity
@Table(name = "ocorrencia", indexes = {
        @Index(name = "idx_ocorrencia_status", columnList = "status"),
        @Index(name = "idx_ocorrencia_data", columnList = "data_hora_registro")
})
public class Ocorrencia {

    @Id
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private TipoOcorrencia tipo;

    @Column(nullable = false, length = 300)
    private String endereco;

    @Column(nullable = false)
    private double latitude;

    @Column(nullable = false)
    private double longitude;

    @Column(name = "data_hora_registro", nullable = false)
    private LocalDateTime dataHoraRegistro;

    @Column(length = 1000)
    private String descricao;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private volatile StatusOcorrencia status;

    @Column(name = "atualizado_em", nullable = false)
    private volatile LocalDateTime atualizadoEm;

    /** Cliente que registrou a ocorrência (ManyToOne usa busca imediata, o que evita erro de carregamento tardio). */
    @ManyToOne
    @JoinColumn(name = "autor_id")
    private Usuario autor;

    /** Administrador que fez a última mudança de status. */
    @ManyToOne
    @JoinColumn(name = "responsavel_id")
    private volatile Usuario responsavel;

    /** Controle de versão (lock otimista): impede que duas alterações simultâneas se sobrescrevam. */
    @Version
    private Long versao;

    /** Construtor exigido pelo JPA. Prefira nova() ou restaurar(). */
    protected Ocorrencia() {
    }

    private Ocorrencia(UUID id, TipoOcorrencia tipo, String endereco, double latitude, double longitude,
                       LocalDateTime dataHoraRegistro, String descricao, StatusOcorrencia status,
                       LocalDateTime atualizadoEm, Usuario autor, Usuario responsavel) {
        this.id = id;
        this.tipo = tipo;
        this.endereco = endereco;
        this.latitude = latitude;
        this.longitude = longitude;
        this.dataHoraRegistro = dataHoraRegistro;
        this.descricao = descricao;
        this.status = status;
        this.atualizadoEm = atualizadoEm;
        this.autor = autor;
        this.responsavel = responsavel;
    }

    /** Cria uma ocorrência nova: id gerado e status inicial PENDENTE. */
    public static Ocorrencia nova(TipoOcorrencia tipo, String endereco, double latitude, double longitude,
                                  LocalDateTime dataHoraRegistro, String descricao, Usuario autor) {
        return new Ocorrencia(UUID.randomUUID(), tipo, endereco, latitude, longitude, dataHoraRegistro,
                descricao, StatusOcorrencia.PENDENTE, LocalDateTime.now(), autor, null);
    }

    /** Reconstitui uma ocorrência com os campos persistidos. */
    public static Ocorrencia restaurar(UUID id, TipoOcorrencia tipo, String endereco, double latitude, double longitude,
                                       LocalDateTime dataHoraRegistro, String descricao, StatusOcorrencia status,
                                       LocalDateTime atualizadoEm, Usuario autor, Usuario responsavel) {
        return new Ocorrencia(id, tipo, endereco, latitude, longitude, dataHoraRegistro, descricao,
                status, atualizadoEm, autor, responsavel);
    }

    /** Avança o status (Pendente, Em análise, Resolvido) e registra quem fez a mudança. */
    public void mudarStatus(StatusOcorrencia novo, Usuario responsavel) {
        if (!status.podeTransicionarPara(novo)) {
            throw new TransicaoInvalidaException(status, novo);
        }
        this.status = novo;
        this.responsavel = responsavel;
        this.atualizadoEm = LocalDateTime.now();
    }

    /** Dias em aberto: do registro até agora (ou até a resolução, se já resolvida). */
    public long diasEmAberto() {
        return diasEmAberto(LocalDateTime.now());
    }

    public long diasEmAberto(LocalDateTime agora) {
        LocalDateTime fim = (status == StatusOcorrencia.RESOLVIDO) ? atualizadoEm : agora;
        return Math.max(0, ChronoUnit.DAYS.between(dataHoraRegistro, fim));
    }

    public UUID getId() { return id; }
    public TipoOcorrencia getTipo() { return tipo; }
    public String getEndereco() { return endereco; }
    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }
    public LocalDateTime getDataHoraRegistro() { return dataHoraRegistro; }
    public String getDescricao() { return descricao; }
    public StatusOcorrencia getStatus() { return status; }
    public LocalDateTime getAtualizadoEm() { return atualizadoEm; }
    public Usuario getAutor() { return autor; }
    public Usuario getResponsavel() { return responsavel; }
}
