package br.com.checkpoint.service;

/**
 * Tudo que o aluno envia para provar que está no local ao bater o ponto.
 *
 * @param qrCode    conteúdo lido do QR Code fixado no local
 * @param wifiSsid  nome da rede Wi-Fi em que o celular está conectado (pode ser null)
 * @param wifiBssid MAC do roteador (pode ser null)
 * @param ip        IP de onde a requisição chegou (preenchido pelo servidor)
 * @param selfie    foto tirada na hora (pode ser null)
 */
public record ProvaPresenca(double latitude, double longitude, String qrCode, String wifiSsid, String wifiBssid,
                            String ip, Selfie selfie) {

    public record Selfie(byte[] dados, String contentType) {
    }
}
