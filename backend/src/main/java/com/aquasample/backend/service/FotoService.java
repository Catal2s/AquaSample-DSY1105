package com.aquasample.backend.service;

import com.aquasample.backend.exception.AccionNoPermitidaException;
import com.aquasample.backend.exception.ConflictoException;
import com.aquasample.backend.exception.DatoInvalidoException;
import com.aquasample.backend.exception.RecursoNoEncontradoException;
import com.aquasample.backend.model.EstadoRevision;
import com.aquasample.backend.model.Muestra;
import com.aquasample.backend.model.Rol;
import com.aquasample.backend.repository.MuestraRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.Arrays;

/**
 * Guarda la foto de cada muestra en una carpeta del servidor
 * y deja sus metadatos (nombre, tipo, tamano) en la tabla muestras.
 */
@Service
public class FotoService {

    private static final Logger LOG = LoggerFactory.getLogger(FotoService.class);

    /** Primeros bytes que identifican a cada formato. */
    private static final byte[] FIRMA_JPEG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};
    private static final byte[] FIRMA_PNG = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A};

    private final MuestraRepository repositorio;
    private final Path carpeta;

    public FotoService(MuestraRepository repositorio,
                       @Value("${app.fotos.directorio:uploads}") String directorio) {
        this.repositorio = repositorio;
        this.carpeta = Path.of(directorio);
    }

    @Transactional
    public Muestra guardar(int idMuestra, MultipartFile archivo, Rol rol) throws IOException {
        if (rol != Rol.OPERADOR) {
            throw new AccionNoPermitidaException("Solo el operador sube fotos de la muestra");
        }
        Muestra muestra = repositorio.findById(idMuestra)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe la muestra " + idMuestra));
        if (muestra.getEstado() == EstadoRevision.VALIDADO) {
            throw new ConflictoException("La muestra " + idMuestra + " ya fue validada y no se puede cambiar su foto");
        }
        if (archivo == null || archivo.isEmpty()) {
            throw new DatoInvalidoException("La foto esta vacia");
        }
        // No se confia en el Content-Type del cliente: se mira el contenido.
        String tipo = detectarTipo(archivo);

        Files.createDirectories(carpeta);
        String anterior = muestra.getFotoNombre();
        // Un archivo por muestra: si se vuelve a subir, se reemplaza (sin duplicados).
        String nombre = "muestra-" + idMuestra + (tipo.equals("image/png") ? ".png" : ".jpg");
        try (InputStream entrada = archivo.getInputStream()) {
            Files.copy(entrada, carpeta.resolve(nombre), StandardCopyOption.REPLACE_EXISTING);
        }

        muestra.setFotoNombre(nombre);
        muestra.setFotoTipo(tipo);
        muestra.setFotoTamanoBytes(archivo.getSize());
        muestra.setActualizadoEn(LocalDateTime.now());
        Muestra guardada = repositorio.save(muestra);

        // Si cambio la extension (png a jpg), el archivo viejo tiene otro nombre.
        if (!nombre.equals(anterior)) {
            // flush: si la base rechaza el cambio, falla aqui y la foto anterior se conserva.
            repositorio.flush();
            borrarArchivo(anterior);
        }
        return guardada;
    }

    @Transactional(readOnly = true)
    public Path rutaFoto(int idMuestra) {
        Muestra muestra = repositorio.findById(idMuestra)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe la muestra " + idMuestra));
        if (muestra.getFotoNombre() == null) {
            throw new RecursoNoEncontradoException("La muestra " + idMuestra + " no tiene foto");
        }
        Path ruta = carpeta.resolve(muestra.getFotoNombre());
        if (!Files.exists(ruta)) {
            throw new RecursoNoEncontradoException("No se encontro el archivo de la foto");
        }
        return ruta;
    }

    @Transactional(readOnly = true)
    public String tipoFoto(int idMuestra) {
        return repositorio.findById(idMuestra).map(Muestra::getFotoTipo).orElse("image/jpeg");
    }

    /** Borra el archivo si existe. Si falla solo se registra: no debe impedir la operacion principal. */
    public void borrarArchivo(String nombre) {
        if (nombre == null) {
            return;
        }
        try {
            Files.deleteIfExists(carpeta.resolve(nombre));
        } catch (IOException e) {
            LOG.warn("No se pudo borrar la foto {}", nombre, e);
        }
    }

    private String detectarTipo(MultipartFile archivo) throws IOException {
        byte[] inicio;
        try (InputStream entrada = archivo.getInputStream()) {
            inicio = entrada.readNBytes(FIRMA_PNG.length);
        }
        if (empiezaCon(inicio, FIRMA_JPEG)) {
            return "image/jpeg";
        }
        if (empiezaCon(inicio, FIRMA_PNG)) {
            return "image/png";
        }
        throw new DatoInvalidoException("La foto debe ser JPEG o PNG");
    }

    private boolean empiezaCon(byte[] datos, byte[] firma) {
        return datos.length >= firma.length
                && Arrays.equals(datos, 0, firma.length, firma, 0, firma.length);
    }
}
