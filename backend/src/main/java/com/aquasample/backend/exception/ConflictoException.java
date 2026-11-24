package com.aquasample.backend.exception;

/** Se lanza cuando la operacion choca con el estado actual (responde 409). */
public class ConflictoException extends RuntimeException {
    public ConflictoException(String mensaje) {
        super(mensaje);
    }
}
