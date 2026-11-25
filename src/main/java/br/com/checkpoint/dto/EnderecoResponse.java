package br.com.checkpoint.dto;

import br.com.checkpoint.domain.Endereco;

public record EnderecoResponse(Long id, String cep, String logradouro, String cidade, String estado,
                               String numero, String bairro, Double latitude, Double longitude,
                               String wifiSsid, String wifiBssid, String ipPublico, Integer raioMetros,
                               boolean aceitaQrImpresso, boolean demonstracao) {

    public static EnderecoResponse from(Endereco e) {
        return new EnderecoResponse(e.getId(), e.getCep(), e.getLogradouro(), e.getCidade(), e.getEstado(),
                e.getNumero(), e.getBairro(), e.getLatitude(), e.getLongitude(), e.getWifiSsid(), e.getWifiBssid(),
                e.getIpPublico(), e.getRaioMetros(), e.isAceitaQrImpresso(), e.isDemonstracao());
    }
}
