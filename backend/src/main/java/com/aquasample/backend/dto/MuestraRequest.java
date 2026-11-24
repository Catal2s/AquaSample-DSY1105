package com.aquasample.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * Datos que envia la app para crear o actualizar una muestra.
 * El estado no viene aqui: solo cambia con PATCH /estado segun el rol.
 */
@Schema(description = "Datos de una muestra enviados por la app")
public record MuestraRequest(
        @Schema(description = "Id de la muestra en la app (Room). Opcional en POST", example = "7")
        @Positive(message = "El id debe ser mayor a 0")
        Integer id,

        @Schema(example = "C1") @NotBlank(message = "El centro es obligatorio")
        @Size(max = 50, message = "No puede superar 50 caracteres")
        String centroId,

        @Schema(example = "T1") @NotBlank(message = "El tren es obligatorio")
        @Size(max = 50, message = "No puede superar 50 caracteres")
        String trenId,

        @Schema(example = "L1") @NotBlank(message = "La linea es obligatoria")
        @Size(max = 50, message = "No puede superar 50 caracteres")
        String lineaId,

        @Schema(example = "2026-11-24")
        @NotBlank(message = "La fecha es obligatoria")
        @Pattern(regexp = "\\d{4}-\\d{2}-\\d{2}", message = "La fecha debe tener formato yyyy-MM-dd")
        String fecha,

        @Schema(example = "10:30")
        @NotBlank(message = "La hora es obligatoria")
        @Pattern(regexp = "\\d{2}:\\d{2}", message = "La hora debe tener formato HH:mm")
        String hora,

        @Schema(example = "0.2")
        @DecimalMin(value = "0.01", message = "El tramo debe ser al menos 0.01")
        double tramoMetros,

        @Schema(example = "U1") @NotBlank(message = "El operador es obligatorio")
        @Size(max = 50, message = "No puede superar 50 caracteres")
        String operadorId,

        @Schema(example = "312")
        @Min(value = 1, message = "Los individuos deben ser al menos 1")
        int individuos,

        @Schema(example = "38.5")
        @Positive(message = "El calibre debe ser mayor a 0")
        Double calibrePromedioMm,

        @Schema(example = "true")
        boolean asistidoPorFoto,

        @Schema(example = "300")
        @Min(value = 0, message = "Las marcas no pueden ser negativas")
        int marcasRegistradas,

        @Schema(example = "12")
        int ajusteManual,

        @Schema(example = "Muestra sin novedades")
        @Size(max = 500, message = "Las observaciones no pueden superar 500 caracteres")
        String observaciones
) {
}
