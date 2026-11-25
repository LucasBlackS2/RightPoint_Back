package br.com.checkpoint.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "enderecos")
public class Endereco {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 15)
    private String cep;

    @Column(nullable = false, length = 255)
    private String logradouro;

    @Column(nullable = false, length = 120)
    private String cidade;

    @Column(nullable = false, length = 10)
    private String estado;

    @Column(length = 30)
    private String numero;

    @Column(length = 120)
    private String bairro;

    /** Mesmos nomes de coluna da versão antiga (lat / long). */
    @Column(name = "lat")
    private Double latitude;

    @Column(name = "long")
    private Double longitude;

    /** Código secreto impresso no QR Code fixado no local do estágio. */
    @Column(name = "qr_token", length = 64, unique = true)
    private String qrToken;

    /** Nome da rede Wi-Fi do local (opcional). Se preenchido, o ponto só vale conectado nela. */
    @Column(name = "wifi_ssid", length = 64)
    private String wifiSsid;

    /** Endereço MAC do roteador (opcional), mais difícil de imitar que o nome da rede. */
    @Column(name = "wifi_bssid", length = 32)
    private String wifiBssid;

    /** IP público da internet do local (opcional; vários separados por vírgula). Funciona também no iPhone. */
    @Column(name = "ip_publico", length = 255)
    private String ipPublico;

    /** Raio permitido em metros para este local (null = valor padrão GEOFENCE_RAIO_METROS). */
    @Column(name = "raio_metros")
    private Integer raioMetros;

    /** Se false, só vale o QR Code ao vivo mostrado no celular do professor. */
    @Column(name = "aceita_qr_impresso", nullable = false, columnDefinition = "boolean not null default true")
    private boolean aceitaQrImpresso = true;

    /** Local fictício usado no modo demonstração (GPS e Wi-Fi são simulados). */
    @Column(nullable = false, columnDefinition = "boolean not null default false")
    private boolean demonstracao = false;

    public boolean temCoordenadas() {
        return latitude != null && longitude != null;
    }

    /** Texto usado na busca de coordenadas (geocodificação). */
    public String enderecoCompleto() {
        StringBuilder sb = new StringBuilder(logradouro);
        if (numero != null && !numero.isBlank()) sb.append(", ").append(numero);
        if (bairro != null && !bairro.isBlank()) sb.append(" - ").append(bairro);
        sb.append(", ").append(cidade).append(" - ").append(estado);
        if (cep != null && !cep.isBlank()) sb.append(", ").append(cep);
        sb.append(", Brasil");
        return sb.toString();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getCep() { return cep; }
    public void setCep(String cep) { this.cep = cep; }

    public String getLogradouro() { return logradouro; }
    public void setLogradouro(String logradouro) { this.logradouro = logradouro; }

    public String getCidade() { return cidade; }
    public void setCidade(String cidade) { this.cidade = cidade; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    public String getNumero() { return numero; }
    public void setNumero(String numero) { this.numero = numero; }

    public String getBairro() { return bairro; }
    public void setBairro(String bairro) { this.bairro = bairro; }

    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }

    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }

    public String getQrToken() { return qrToken; }
    public void setQrToken(String qrToken) { this.qrToken = qrToken; }

    public String getWifiSsid() { return wifiSsid; }
    public void setWifiSsid(String wifiSsid) { this.wifiSsid = wifiSsid; }

    public String getWifiBssid() { return wifiBssid; }
    public void setWifiBssid(String wifiBssid) { this.wifiBssid = wifiBssid; }

    public String getIpPublico() { return ipPublico; }
    public void setIpPublico(String ipPublico) { this.ipPublico = ipPublico; }

    public Integer getRaioMetros() { return raioMetros; }
    public void setRaioMetros(Integer raioMetros) { this.raioMetros = raioMetros; }

    public boolean isAceitaQrImpresso() { return aceitaQrImpresso; }
    public void setAceitaQrImpresso(boolean aceitaQrImpresso) { this.aceitaQrImpresso = aceitaQrImpresso; }

    public boolean isDemonstracao() { return demonstracao; }
    public void setDemonstracao(boolean demonstracao) { this.demonstracao = demonstracao; }
}
