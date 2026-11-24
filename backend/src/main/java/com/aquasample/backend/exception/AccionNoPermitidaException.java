package com.aquasample.backend.exception;

/** Se lanza cuando el rol no puede hacer esa accion (responde 403). */
public class AccionNoPermitidaException extends RuntimeException {
    public AccionNoPermitidaException(String mensaje) {
        super(mensaje);
    }
}
