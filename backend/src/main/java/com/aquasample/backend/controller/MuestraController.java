package com.aquasample.backend.controller;

import com.aquasample.backend.dto.CambioEstadoRequest;
import com.aquasample.backend.dto.ErrorResponse;
import com.aquasample.backend.dto.MuestraRequest;
import com.aquasample.backend.dto.MuestraResponse;
import com.aquasample.backend.model.EstadoRevision;
import com.aquasample.backend.model.Rol;
import com.aquasample.backend.service.FotoService;
import com.aquasample.backend.service.MuestraService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

/** Endpoints CRUD de muestras, estados de revision y fotos. */
@RestController
@RequestMapping("/api/muestras")
@Tag(name = "Muestras", description = "CRUD de muestras, revision por rol y fotos")
public class MuestraController {

    private static final String ROL = "X-Rol";

    private final MuestraService servicio;
    private final FotoService fotoService;

    public MuestraController(MuestraService servicio, FotoService fotoService) {
        this.servicio = servicio;
        this.fotoService = fotoService;
    }

    @GetMapping
    @ApiResponse(responseCode = "200", description = "OK", useReturnTypeSchema = true)
    @ApiResponse(responseCode = "400", description = "Parametro con formato invalido",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    @Operation(summary = "Listar muestras", description = "Filtros opcionales por centro, tren, linea, estado y rango de fechas (yyyy-MM-dd)")
    public List<MuestraResponse> listar(
            @RequestParam(required = false) String centroId,
            @RequestParam(required = false) String trenId,
            @RequestParam(required = false) String lineaId,
            @RequestParam(required = false) EstadoRevision estado,
            @RequestParam(required = false) String desde,
            @RequestParam(required = false) String hasta) {
        return servicio.listar(centroId, trenId, lineaId, estado, desde, hasta).stream()
                .map(MuestraResponse::desde)
                .toList();
    }

    @GetMapping("/{id}")
    @ApiResponse(responseCode = "200", description = "OK", useReturnTypeSchema = true)
    @ApiResponse(responseCode = "400", description = "Parametro con formato invalido",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "404", description = "No existe",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    @Operation(summary = "Obtener una muestra por id")
    public MuestraResponse obtener(@PathVariable int id) {
        return MuestraResponse.desde(servicio.obtener(id));
    }

    @PostMapping
    @ApiResponse(responseCode = "201", description = "Creada", useReturnTypeSchema = true)
    @ApiResponse(responseCode = "400", description = "Datos invalidos o falta X-Rol",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "403", description = "El rol no puede hacer esta accion",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "409", description = "Conflicto con los datos guardados",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "415", description = "Content-Type no soportado",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Crear muestra (operador)", description = "Queda en estado PENDIENTE. Si no se envia id, se asigna el siguiente")
    public MuestraResponse crear(
            @Parameter(description = "Rol de quien llama", example = "OPERADOR") @RequestHeader(ROL) Rol rol,
            @Valid @RequestBody MuestraRequest datos) {
        return MuestraResponse.desde(servicio.crear(datos, rol));
    }

    @PutMapping("/{id}")
    @ApiResponse(responseCode = "200", description = "OK", useReturnTypeSchema = true)
    @ApiResponse(responseCode = "400", description = "Datos invalidos o falta X-Rol",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "403", description = "El rol no puede hacer esta accion",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "409", description = "Conflicto con los datos guardados",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "415", description = "Content-Type no soportado",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    @Operation(summary = "Crear o actualizar muestra (operador)",
            description = "Usado por la sincronizacion: con el mismo id se actualiza la misma fila, sin duplicados. No permite editar muestras validadas")
    public MuestraResponse guardar(
            @PathVariable int id,
            @Parameter(description = "Rol de quien llama", example = "OPERADOR") @RequestHeader(ROL) Rol rol,
            @Valid @RequestBody MuestraRequest datos) {
        return MuestraResponse.desde(servicio.guardarOSincronizar(id, datos, rol));
    }

    @PatchMapping("/{id}/estado")
    @ApiResponse(responseCode = "200", description = "OK", useReturnTypeSchema = true)
    @ApiResponse(responseCode = "400", description = "Datos invalidos o falta X-Rol",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "403", description = "El rol no puede hacer esta accion",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "404", description = "No existe",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "409", description = "Conflicto con los datos guardados",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "415", description = "Content-Type no soportado",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    @Operation(summary = "Cambiar estado de revision",
            description = "SUPERVISOR: VALIDADO u OBSERVADO (con comentario). OPERADOR: CORREGIDO si estaba OBSERVADO")
    public MuestraResponse cambiarEstado(
            @PathVariable int id,
            @Parameter(description = "Rol de quien llama", example = "SUPERVISOR") @RequestHeader(ROL) Rol rol,
            @Valid @RequestBody CambioEstadoRequest cambio) {
        return MuestraResponse.desde(servicio.cambiarEstado(id, cambio, rol));
    }

    @DeleteMapping("/{id}")
    @ApiResponse(responseCode = "204", description = "Eliminada", content = @Content)
    @ApiResponse(responseCode = "400", description = "Datos invalidos o falta X-Rol",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "403", description = "El rol no puede hacer esta accion",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "404", description = "No existe",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Eliminar muestra (supervisor)")
    public void eliminar(
            @PathVariable int id,
            @Parameter(description = "Rol de quien llama", example = "SUPERVISOR") @RequestHeader(ROL) Rol rol) {
        servicio.eliminar(id, rol);
    }

    @PostMapping(value = "/{id}/foto", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ApiResponse(responseCode = "200", description = "OK", useReturnTypeSchema = true)
    @ApiResponse(responseCode = "400", description = "Datos invalidos o falta X-Rol",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "403", description = "El rol no puede hacer esta accion",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "404", description = "No existe",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "409", description = "Conflicto con los datos guardados",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "413", description = "La foto supera 10 MB",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "415", description = "Content-Type no soportado",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    @Operation(summary = "Subir la foto de la muestra (operador)", description = "Reemplaza la foto anterior si ya existia")
    public MuestraResponse subirFoto(
            @PathVariable int id,
            @Parameter(description = "Rol de quien llama", example = "OPERADOR") @RequestHeader(ROL) Rol rol,
            @RequestPart("archivo") MultipartFile archivo) throws IOException {
        return MuestraResponse.desde(fotoService.guardar(id, archivo, rol));
    }

    @GetMapping("/{id}/foto")
    @ApiResponse(responseCode = "200", description = "OK", useReturnTypeSchema = true)
    @ApiResponse(responseCode = "400", description = "Parametro con formato invalido",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "404", description = "No existe",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    @Operation(summary = "Descargar la foto de la muestra")
    public ResponseEntity<Resource> verFoto(@PathVariable int id) {
        Resource foto = new FileSystemResource(fotoService.rutaFoto(id));
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(fotoService.tipoFoto(id)))
                .body(foto);
    }
}
