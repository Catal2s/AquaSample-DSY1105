package com.aquasample.backend.exception;

/** Se lanza cuando un dato enviado no es valido (responde 400). */
public class DatoInvalidoException extends RuntimeException {
    public DatoInvalidoException(String mensaje) {
        super(mensaje);
    }
}
