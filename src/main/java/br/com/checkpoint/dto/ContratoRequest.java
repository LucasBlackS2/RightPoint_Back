package br.com.checkpoint.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDate;

/** JSON: id_aluno, id_professor, id_endereco, data_inicio, data_final, carga_horaria_total (horas). */
public record ContratoRequest(
        @NotNull(message = "Informe o aluno.") Long idAluno,
        @NotNull(message = "Informe o professor orientador.") Long idProfessor,
        @NotNull(message = "Informe o endereço do estágio.") Long idEndereco,
        LocalDate dataInicio,
        LocalDate dataFinal,
        @Positive(message = "A carga horária deve ser maior que zero.") Integer cargaHorariaTotal) {
}
