package br.com.checkpoint.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.LocalDateTime;

/**
 * Selfie tirada pelo aluno ao registrar a entrada ou a saída.
 * Fica numa tabela separada para não carregar a imagem toda vez que se lê um ponto.
 */
@Entity
@Table(name = "fotos_ponto", uniqueConstraints = @UniqueConstraint(columnNames = {"id_ponto", "momento"}))
public class FotoPonto {

    public static final String ENTRADA = "entrada";
    public static final String SAIDA = "saida";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_ponto", nullable = false)
    private Ponto ponto;

    /** "entrada" ou "saida". */
    @Column(nullable = false, length = 10)
    private String momento;

    @Column(name = "content_type", nullable = false, length = 50)
    private String contentType;

    @Column(nullable = false)
    private byte[] dados;

    @Column(name = "criado_em", nullable = false)
    private LocalDateTime criadoEm;

    public Long getId() { return id; }

    public Ponto getPonto() { return ponto; }
    public void setPonto(Ponto ponto) { this.ponto = ponto; }

    public String getMomento() { return momento; }
    public void setMomento(String momento) { this.momento = momento; }

    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }

    public byte[] getDados() { return dados; }
    public void setDados(byte[] dados) { this.dados = dados; }

    public LocalDateTime getCriadoEm() { return criadoEm; }
    public void setCriadoEm(LocalDateTime criadoEm) { this.criadoEm = criadoEm; }
}
