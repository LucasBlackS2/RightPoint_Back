package br.com.checkpoint.dto;

import br.com.checkpoint.domain.Contrato;

import java.time.LocalDate;

public record ContratoResponse(Long id, UsuarioResumo aluno, UsuarioResumo professor, EnderecoResponse endereco,
                               LocalDate dataInicio, LocalDate dataFinal, boolean status,
                               Integer cargaHorariaTotal) {

    /** Precisa ser chamado dentro de uma transação (aluno/professor/endereço são carregados sob demanda). */
    public static ContratoResponse from(Contrato c) {
        return new ContratoResponse(c.getId(), UsuarioResumo.from(c.getAluno()), UsuarioResumo.from(c.getProfessor()),
                EnderecoResponse.from(c.getEndereco()), c.getDataInicio(), c.getDataFinal(), c.isStatus(),
                c.getCargaHorariaTotal());
    }
}
