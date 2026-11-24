package com.aquasample.backend.dto;

import java.util.Map;

/** Indicadores para el dashboard y los reportes. */
public record ResumenResponse(
        long totalMuestras,
        long totalIndividuos,
        double promedioIndividuos,
        Map<String, Long> muestrasPorEstado,
        Map<String, Long> muestrasPorCentro
) {
}
