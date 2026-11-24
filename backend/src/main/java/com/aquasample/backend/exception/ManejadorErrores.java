package com.aquasample.backend.exception;

import com.aquasample.backend.dto.ErrorResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.List;

/** Convierte las excepciones en respuestas JSON con el codigo HTTP correcto. */
@RestControllerAdvice
public class ManejadorErrores {

    private static final Logger LOG = LoggerFactory.getLogger(ManejadorErrores.class);

    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ResponseEntity<ErrorResponse> noEncontrado(RecursoNoEncontradoException e) {
        return respuesta(HttpStatus.NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler(AccionNoPermitidaException.class)
    public ResponseEntity<ErrorResponse> noPermitido(AccionNoPermitidaException e) {
        return respuesta(HttpStatus.FORBIDDEN, e.getMessage());
    }

    @ExceptionHandler(ConflictoException.class)
    public ResponseEntity<ErrorResponse> conflicto(ConflictoException e) {
        return respuesta(HttpStatus.CONFLICT, e.getMessage());
    }

    @ExceptionHandler(DatoInvalidoException.class)
    public ResponseEntity<ErrorResponse> datoInvalido(DatoInvalidoException e) {
        return respuesta(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    /** Errores de las anotaciones @NotBlank, @Min, @Pattern, etc. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> validacion(MethodArgumentNotValidException e) {
        List<String> mensajes = e.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .toList();
        return respuesta(HttpStatus.BAD_REQUEST, HttpHeaders.EMPTY, mensajes);
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<ErrorResponse> faltaHeader(MissingRequestHeaderException e) {
        return respuesta(HttpStatus.BAD_REQUEST, "Falta el header " + e.getHeaderName());
    }

    /** Por ejemplo un rol o estado que no existe en el enum. */
    @ExceptionHandler({MethodArgumentTypeMismatchException.class, HttpMessageNotReadableException.class})
    public ResponseEntity<ErrorResponse> formatoInvalido(Exception e) {
        return respuesta(HttpStatus.BAD_REQUEST, "Valor con formato invalido");
    }

    @ExceptionHandler(MissingServletRequestPartException.class)
    public ResponseEntity<ErrorResponse> faltaArchivo(MissingServletRequestPartException e) {
        return respuesta(HttpStatus.BAD_REQUEST, "Falta el campo " + e.getRequestPartName());
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponse> archivoMuyGrande(MaxUploadSizeExceededException e) {
        return respuesta(HttpStatus.PAYLOAD_TOO_LARGE, "El archivo supera el maximo de 10 MB");
    }

    @ExceptionHandler(MultipartException.class)
    public ResponseEntity<ErrorResponse> multipartInvalido(MultipartException e) {
        return respuesta(HttpStatus.BAD_REQUEST, "El envio del archivo no es valido");
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> rutaNoExiste(NoResourceFoundException e) {
        return respuesta(HttpStatus.NOT_FOUND, "La ruta no existe");
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> metodoNoPermitido(HttpRequestMethodNotSupportedException e) {
        return respuesta(HttpStatus.METHOD_NOT_ALLOWED, e.getHeaders(),
                List.of("El metodo " + e.getMethod() + " no esta permitido en esta ruta"));
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErrorResponse> tipoNoSoportado(HttpMediaTypeNotSupportedException e) {
        return respuesta(HttpStatus.UNSUPPORTED_MEDIA_TYPE, e.getHeaders(),
                List.of("El Content-Type enviado no esta soportado"));
    }

    /**
     * Todo lo demas. Si Spring ya sabe el codigo (por ejemplo 406) se respeta;
     * si no, es un error nuestro: se registra en el log y responde 500 sin mostrar detalles.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> inesperado(Exception e) {
        if (e instanceof org.springframework.web.ErrorResponse conocido) {
            return respuesta(conocido.getStatusCode(), conocido.getHeaders(),
                    List.of("No se pudo procesar la solicitud"));
        }
        LOG.error("Error inesperado", e);
        return respuesta(HttpStatus.INTERNAL_SERVER_ERROR, "Error interno del servidor");
    }

    private ResponseEntity<ErrorResponse> respuesta(HttpStatus status, String mensaje) {
        return respuesta(status, HttpHeaders.EMPTY, List.of(mensaje));
    }

    private ResponseEntity<ErrorResponse> respuesta(HttpStatusCode codigo, HttpHeaders headers, List<String> mensajes) {
        HttpStatus status = HttpStatus.resolve(codigo.value());
        String error = status == null ? "Error" : status.getReasonPhrase();
        return ResponseEntity.status(codigo)
                .headers(headers)
                .contentType(MediaType.APPLICATION_JSON)
                .body(ErrorResponse.de(codigo.value(), error, mensajes));
    }
}
