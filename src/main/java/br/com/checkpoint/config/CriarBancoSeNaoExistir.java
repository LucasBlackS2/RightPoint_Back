package br.com.checkpoint.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.env.Environment;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Antes de o Spring abrir a conexão principal, confere se o banco do
 * PostgreSQL (ex.: "checkpoint") existe e o cria se for preciso.
 * Se a senha estiver errada ou o PostgreSQL estiver desligado, para a
 * aplicação com uma mensagem clara dizendo o que corrigir.
 */
public class CriarBancoSeNaoExistir implements ApplicationListener<ApplicationEnvironmentPreparedEvent> {

    private static final Logger log = LoggerFactory.getLogger(CriarBancoSeNaoExistir.class);
    private static final String PREFIXO = "jdbc:postgresql://";

    @Override
    public void onApplicationEvent(ApplicationEnvironmentPreparedEvent event) {
        Environment env = event.getEnvironment();
        String url = env.getProperty("spring.datasource.url");
        String usuario = env.getProperty("spring.datasource.username");
        String senha = env.getProperty("spring.datasource.password");
        if (url == null || !url.startsWith(PREFIXO)) {
            return;
        }

        // jdbc:postgresql://host:porta/banco?parametros
        String semPrefixo = url.substring(PREFIXO.length());
        int barra = semPrefixo.indexOf('/');
        if (barra < 0) {
            return;
        }
        String hostPorta = semPrefixo.substring(0, barra);
        String resto = semPrefixo.substring(barra + 1);
        int interrogacao = resto.indexOf('?');
        String banco = interrogacao >= 0 ? resto.substring(0, interrogacao) : resto;
        String parametros = interrogacao >= 0 ? resto.substring(interrogacao) : "";
        if (!banco.matches("[A-Za-z0-9_]+")) {
            return;
        }

        // Conecta no banco padrão "postgres" só para verificar/criar o banco da aplicação.
        String urlAdmin = PREFIXO + hostPorta + "/postgres" + parametros;
        try (Connection conexao = DriverManager.getConnection(urlAdmin, usuario, senha)) {
            if (existe(conexao, banco)) {
                return;
            }
            try (Statement st = conexao.createStatement()) {
                st.executeUpdate("CREATE DATABASE \"" + banco + "\"");
            }
            log.info("Banco de dados '{}' não existia e foi criado automaticamente.", banco);
        } catch (SQLException e) {
            throw new IllegalStateException(explicar(e, hostPorta, usuario, banco), e);
        }
    }

    private static boolean existe(Connection conexao, String banco) throws SQLException {
        try (PreparedStatement ps = conexao.prepareStatement("select 1 from pg_database where datname = ?")) {
            ps.setString(1, banco);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    private static String explicar(SQLException e, String hostPorta, String usuario, String banco) {
        String estado = e.getSQLState() == null ? "" : e.getSQLState();
        String dica;
        if (estado.equals("28P01") || estado.equals("28000")) {
            dica = "Usuário ou senha do PostgreSQL incorretos. Confira POSTGRES_USER e POSTGRES_PASSWORD no arquivo .env.";
        } else if (estado.startsWith("08")) {
            dica = "Não foi possível conectar ao PostgreSQL em " + hostPorta + ". Verifique se ele está instalado e "
                    + "rodando (Serviços do Windows > postgresql) e se POSTGRES_HOST/POSTGRES_PORT no .env estão certos.";
        } else if (estado.equals("42501")) {
            dica = "O usuário '" + usuario + "' não tem permissão para criar o banco '" + banco
                    + "'. Crie o banco manualmente no pgAdmin ou use o usuário postgres.";
        } else {
            dica = "Erro ao preparar o banco '" + banco + "' (SQLState " + estado + ").";
        }
        return "\n\n*** PROBLEMA COM O BANCO DE DADOS ***\n" + dica + "\nDetalhe: " + e.getMessage() + "\n";
    }
}
