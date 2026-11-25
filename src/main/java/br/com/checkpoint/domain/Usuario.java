package br.com.checkpoint.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "usuarios")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String nome;

    @Column(nullable = false, unique = true, length = 50)
    private String matricula;

    @Column(name = "senha_hash", nullable = false, length = 255)
    private String senhaHash;

    @Column(length = 50)
    private String contato;

    @Column(nullable = false, length = 255)
    private String email;

    @Column(length = 50)
    private String turma;

    @Convert(converter = TipoUsuarioConverter.class)
    @Column(name = "tipo_acesso", nullable = false, length = 20)
    private TipoUsuario tipoAcesso = TipoUsuario.ALUNO;

    /** Conta usada no modo demonstração: só enxerga dados de demonstração. */
    @Column(nullable = false, columnDefinition = "boolean not null default false")
    private boolean demonstracao = false;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public String getMatricula() { return matricula; }
    public void setMatricula(String matricula) { this.matricula = matricula; }

    public String getSenhaHash() { return senhaHash; }
    public void setSenhaHash(String senhaHash) { this.senhaHash = senhaHash; }

    public String getContato() { return contato; }
    public void setContato(String contato) { this.contato = contato; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getTurma() { return turma; }
    public void setTurma(String turma) { this.turma = turma; }

    public TipoUsuario getTipoAcesso() { return tipoAcesso; }
    public void setTipoAcesso(TipoUsuario tipoAcesso) { this.tipoAcesso = tipoAcesso; }

    public boolean isDemonstracao() { return demonstracao; }
    public void setDemonstracao(boolean demonstracao) { this.demonstracao = demonstracao; }
}
