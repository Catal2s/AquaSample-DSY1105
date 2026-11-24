package com.aquasample.backend.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;
import java.util.List;

/** Formato comun de error para que la app siempre reciba lo mismo. */
public record ErrorResponse(
        int status,
        String error,
        List<String> mensajes,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime fecha
) {
    public static ErrorResponse de(int status, String error, List<String> mensajes) {
        return new ErrorResponse(status, error, mensajes, LocalDateTime.now());
    }
}
