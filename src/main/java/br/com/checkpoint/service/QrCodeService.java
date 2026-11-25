package br.com.checkpoint.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Map;

/**
 * Gera os QR Codes do CheckPoint:
 * - impresso: código fixo de cada local, para imprimir e colar na empresa;
 * - ao vivo: mostrado no celular do professor, assinado e trocado a cada
 *   {@code janelaSegundos}; uma foto dele para de valer em poucos segundos.
 */
@Service
public class QrCodeService {

    /** Prefixo do QR impresso, para o app reconhecer que é um QR do CheckPoint. */
    public static final String PREFIXO = "CHECKPOINT:";
    /** Prefixo do QR ao vivo: CHECKPOINT-AO-VIVO:{idLocal}.{janela}.{assinatura} */
    public static final String PREFIXO_AO_VIVO = "CHECKPOINT-AO-VIVO:";

    private static final SecureRandom RANDOM = new SecureRandom();

    private final byte[] chaveAoVivo;
    private final int janelaSegundos;

    public QrCodeService(@Value("${checkpoint.jwt.secret}") String segredo,
                         @Value("${checkpoint.qr-ao-vivo.janela-segundos:30}") int janelaSegundos) {
        try {
            // Chave própria para o QR ao vivo, derivada do segredo da aplicação.
            this.chaveAoVivo = MessageDigest.getInstance("SHA-256")
                    .digest(("qr-ao-vivo:" + segredo).getBytes(StandardCharsets.UTF_8));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
        this.janelaSegundos = Math.max(10, janelaSegundos);
    }

    public int getJanelaSegundos() {
        return janelaSegundos;
    }

    /** Número da janela de tempo atual (troca a cada janelaSegundos). */
    public long janelaAtual(Instant agora) {
        return agora.getEpochSecond() / janelaSegundos;
    }

    /** Momento em que o QR ao vivo da janela atual deixa de ser exibido. */
    public Instant fimDaJanela(Instant agora) {
        return Instant.ofEpochSecond((janelaAtual(agora) + 1) * janelaSegundos);
    }

    public String gerarAoVivo(long idLocal, String tokenLocal, Instant agora) {
        long janela = janelaAtual(agora);
        return PREFIXO_AO_VIVO + idLocal + "." + janela + "." + assinar(idLocal, janela, tokenLocal);
    }

    public boolean ehAoVivo(String lido) {
        return lido != null && lido.trim().regionMatches(true, 0, PREFIXO_AO_VIVO, 0, PREFIXO_AO_VIVO.length());
    }

    /**
     * Confere se o QR ao vivo é deste local e é recente: aceita a janela atual
     * e a anterior, para não falhar se o aluno escanear bem na hora da troca.
     */
    public boolean validarAoVivo(String lido, long idLocal, String tokenLocal, Instant agora) {
        if (!ehAoVivo(lido) || tokenLocal == null) return false;
        String[] partes = lido.trim().substring(PREFIXO_AO_VIVO.length()).split("\\.");
        if (partes.length != 3) return false;
        try {
            long id = Long.parseLong(partes[0]);
            long janela = Long.parseLong(partes[1]);
            long atual = janelaAtual(agora);
            if (id != idLocal || janela > atual || janela < atual - 1) return false;
            return MessageDigest.isEqual(assinar(id, janela, tokenLocal).getBytes(StandardCharsets.UTF_8),
                    partes[2].getBytes(StandardCharsets.UTF_8));
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private String assinar(long idLocal, long janela, String tokenLocal) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(chaveAoVivo, "HmacSHA256"));
            byte[] h = mac.doFinal((idLocal + "." + janela + "." + tokenLocal).getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(h).substring(0, 22);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }

    public String novoToken() {
        byte[] bytes = new byte[18];
        RANDOM.nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }

    public String conteudo(String token) {
        return PREFIXO + token;
    }

    /** QR impresso: aceita tanto "CHECKPOINT:abc..." quanto só o token. */
    public String extrairToken(String lido) {
        if (lido == null || ehAoVivo(lido)) return null;
        String texto = lido.trim();
        if (texto.regionMatches(true, 0, PREFIXO, 0, PREFIXO.length())) {
            texto = texto.substring(PREFIXO.length());
        }
        return texto.isEmpty() ? null : texto;
    }

    public byte[] gerarPng(String conteudo, int tamanho) {
        try {
            BitMatrix matriz = new QRCodeWriter().encode(conteudo, BarcodeFormat.QR_CODE, tamanho, tamanho,
                    Map.of(EncodeHintType.MARGIN, 2, EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M));
            BufferedImage imagem = new BufferedImage(matriz.getWidth(), matriz.getHeight(), BufferedImage.TYPE_INT_RGB);
            for (int x = 0; x < matriz.getWidth(); x++) {
                for (int y = 0; y < matriz.getHeight(); y++) {
                    imagem.setRGB(x, y, matriz.get(x, y) ? 0x000000 : 0xFFFFFF);
                }
            }
            ByteArrayOutputStream saida = new ByteArrayOutputStream();
            ImageIO.write(imagem, "png", saida);
            return saida.toByteArray();
        } catch (WriterException | IOException e) {
            throw new IllegalStateException("Não foi possível gerar o QR Code.", e);
        }
    }
}
