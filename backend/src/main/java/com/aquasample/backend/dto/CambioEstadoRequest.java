package com.aquasample.backend.dto;

import com.aquasample.backend.model.EstadoRevision;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Cambio de estado de revision (aprobar, observar o marcar corregida). */
public record CambioEstadoRequest(
        @Schema(example = "OBSERVADO")
        @NotNull(message = "El estado es obligatorio")
        EstadoRevision estado,

        @Schema(example = "Revisar el conteo, parece bajo para esta linea")
        @Size(max = 500, message = "El comentario no puede superar 500 caracteres")
        String comentario
) {
}
