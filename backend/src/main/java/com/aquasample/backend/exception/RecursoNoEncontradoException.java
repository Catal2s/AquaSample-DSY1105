package com.aquasample.backend.exception;

/** Se lanza cuando no existe la muestra pedida (responde 404). */
public class RecursoNoEncontradoException extends RuntimeException {
    public RecursoNoEncontradoException(String mensaje) {
        super(mensaje);
    }
}
