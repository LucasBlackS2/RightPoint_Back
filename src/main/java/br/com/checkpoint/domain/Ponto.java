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

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Um registro de ponto (entrada + saída).
 * Os horários são gravados em UTC, como na versão anterior.
 */
@Entity
@Table(name = "pontos")
public class Ponto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_contrato", nullable = false)
    private Contrato contrato;

    /** Dia do registro no fuso horário local (America/Recife por padrão). */
    @Column(nullable = false)
    private LocalDate data;

    @Column(name = "hora_entrada", nullable = false)
    private LocalDateTime horaEntrada;

    @Column(name = "hora_saida")
    private LocalDateTime horaSaida;

    @Column(name = "tempo_trabalhado_minutos")
    private Integer tempoTrabalhadoMinutos;

    /** true enquanto o ponto está aberto (aluno ainda não registrou saída). */
    @Column(nullable = false)
    private boolean ativo = true;

    @Column(name = "latitude_entrada")
    private Double latitudeEntrada;

    @Column(name = "longitude_entrada")
    private Double longitudeEntrada;

    @Column(name = "distancia_entrada_metros")
    private Double distanciaEntradaMetros;

    @Column(name = "latitude_saida")
    private Double latitudeSaida;

    @Column(name = "longitude_saida")
    private Double longitudeSaida;

    @Column(name = "distancia_saida_metros")
    private Double distanciaSaidaMetros;

    @Column(name = "wifi_entrada", length = 64)
    private String wifiEntrada;

    @Column(name = "ip_entrada", length = 64)
    private String ipEntrada;

    @Column(name = "wifi_saida", length = 64)
    private String wifiSaida;

    @Column(name = "ip_saida", length = 64)
    private String ipSaida;

    @Column(name = "foto_entrada", nullable = false, columnDefinition = "boolean not null default false")
    private boolean fotoEntrada = false;

    @Column(name = "foto_saida", nullable = false, columnDefinition = "boolean not null default false")
    private boolean fotoSaida = false;

    /** Preenchido quando o ponto foi encerrado manualmente por professor/coordenação. */
    @Column(name = "encerrado_por")
    private Long encerradoPor;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Contrato getContrato() { return contrato; }
    public void setContrato(Contrato contrato) { this.contrato = contrato; }

    public LocalDate getData() { return data; }
    public void setData(LocalDate data) { this.data = data; }

    public LocalDateTime getHoraEntrada() { return horaEntrada; }
    public void setHoraEntrada(LocalDateTime horaEntrada) { this.horaEntrada = horaEntrada; }

    public LocalDateTime getHoraSaida() { return horaSaida; }
    public void setHoraSaida(LocalDateTime horaSaida) { this.horaSaida = horaSaida; }

    public Integer getTempoTrabalhadoMinutos() { return tempoTrabalhadoMinutos; }
    public void setTempoTrabalhadoMinutos(Integer tempoTrabalhadoMinutos) { this.tempoTrabalhadoMinutos = tempoTrabalhadoMinutos; }

    public boolean isAtivo() { return ativo; }
    public void setAtivo(boolean ativo) { this.ativo = ativo; }

    public Double getLatitudeEntrada() { return latitudeEntrada; }
    public void setLatitudeEntrada(Double latitudeEntrada) { this.latitudeEntrada = latitudeEntrada; }

    public Double getLongitudeEntrada() { return longitudeEntrada; }
    public void setLongitudeEntrada(Double longitudeEntrada) { this.longitudeEntrada = longitudeEntrada; }

    public Double getDistanciaEntradaMetros() { return distanciaEntradaMetros; }
    public void setDistanciaEntradaMetros(Double distanciaEntradaMetros) { this.distanciaEntradaMetros = distanciaEntradaMetros; }

    public Double getLatitudeSaida() { return latitudeSaida; }
    public void setLatitudeSaida(Double latitudeSaida) { this.latitudeSaida = latitudeSaida; }

    public Double getLongitudeSaida() { return longitudeSaida; }
    public void setLongitudeSaida(Double longitudeSaida) { this.longitudeSaida = longitudeSaida; }

    public Double getDistanciaSaidaMetros() { return distanciaSaidaMetros; }
    public void setDistanciaSaidaMetros(Double distanciaSaidaMetros) { this.distanciaSaidaMetros = distanciaSaidaMetros; }

    public String getWifiEntrada() { return wifiEntrada; }
    public void setWifiEntrada(String wifiEntrada) { this.wifiEntrada = wifiEntrada; }

    public String getIpEntrada() { return ipEntrada; }
    public void setIpEntrada(String ipEntrada) { this.ipEntrada = ipEntrada; }

    public String getWifiSaida() { return wifiSaida; }
    public void setWifiSaida(String wifiSaida) { this.wifiSaida = wifiSaida; }

    public String getIpSaida() { return ipSaida; }
    public void setIpSaida(String ipSaida) { this.ipSaida = ipSaida; }

    public boolean isFotoEntrada() { return fotoEntrada; }
    public void setFotoEntrada(boolean fotoEntrada) { this.fotoEntrada = fotoEntrada; }

    public boolean isFotoSaida() { return fotoSaida; }
    public void setFotoSaida(boolean fotoSaida) { this.fotoSaida = fotoSaida; }

    public Long getEncerradoPor() { return encerradoPor; }
    public void setEncerradoPor(Long encerradoPor) { this.encerradoPor = encerradoPor; }
}
