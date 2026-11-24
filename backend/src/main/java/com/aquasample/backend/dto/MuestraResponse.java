package com.aquasample.backend.dto;

import com.aquasample.backend.model.EstadoRevision;
import com.aquasample.backend.model.Muestra;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

/** Lo que devuelve la API al consultar una muestra. */
public record MuestraResponse(
        Integer id,
        String centroId,
        String trenId,
        String lineaId,
        String fecha,
        String hora,
        double tramoMetros,
        String operadorId,
        int individuos,
        Double calibrePromedioMm,
        boolean asistidoPorFoto,
        int marcasRegistradas,
        int ajusteManual,
        String observaciones,
        EstadoRevision estado,
        String comentarioSupervisor,
        boolean tieneFoto,
        String fotoTipo,
        Long fotoTamanoBytes,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime creadoEn,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime actualizadoEn
) {
    public static MuestraResponse desde(Muestra m) {
        return new MuestraResponse(
                m.getId(), m.getCentroId(), m.getTrenId(), m.getLineaId(),
                m.getFecha(), m.getHora(), m.getTramoMetros(), m.getOperadorId(),
                m.getIndividuos(), m.getCalibrePromedioMm(), m.isAsistidoPorFoto(),
                m.getMarcasRegistradas(), m.getAjusteManual(), m.getObservaciones(),
                m.getEstado(), m.getComentarioSupervisor(),
                m.getFotoNombre() != null, m.getFotoTipo(), m.getFotoTamanoBytes(),
                m.getCreadoEn(), m.getActualizadoEn()
        );
    }
}
