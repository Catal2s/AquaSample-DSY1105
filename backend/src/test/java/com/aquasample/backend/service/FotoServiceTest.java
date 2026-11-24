package com.aquasample.backend.service;

import com.aquasample.backend.exception.AccionNoPermitidaException;
import com.aquasample.backend.exception.ConflictoException;
import com.aquasample.backend.exception.DatoInvalidoException;
import com.aquasample.backend.model.EstadoRevision;
import com.aquasample.backend.model.Muestra;
import com.aquasample.backend.model.Rol;
import com.aquasample.backend.repository.MuestraRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Pruebas de las fotos usando una carpeta temporal. */
class FotoServiceTest {

    private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 1};
    private static final byte[] JPEG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 1};

    @TempDir
    Path carpeta;
    private Muestra muestra;
    private FotoService servicio;

    @BeforeEach
    void preparar() {
        MuestraRepository repositorio = mock(MuestraRepository.class);
        muestra = new Muestra();
        muestra.setId(1);
        when(repositorio.findById(1)).thenReturn(Optional.of(muestra));
        when(repositorio.save(any(Muestra.class))).thenAnswer(inv -> inv.getArgument(0));
        servicio = new FotoService(repositorio, carpeta.toString());
    }

    private MockMultipartFile archivo(String tipo, byte[] contenido) {
        return new MockMultipartFile("archivo", "foto", tipo, contenido);
    }

    @Test
    void elTipoSaleDelContenido_noDelContentType() throws Exception {
        servicio.guardar(1, archivo("image/jpeg", PNG), Rol.OPERADOR);
        assertEquals("image/png", muestra.getFotoTipo());
        assertTrue(Files.exists(carpeta.resolve("muestra-1.png")));
    }

    @Test
    void textoOSvgDisfrazados_sonInvalidos() {
        assertThrows(DatoInvalidoException.class,
                () -> servicio.guardar(1, archivo("image/jpeg", "hola".getBytes()), Rol.OPERADOR));
        assertThrows(DatoInvalidoException.class,
                () -> servicio.guardar(1, archivo("image/svg+xml", "<svg></svg>".getBytes()), Rol.OPERADOR));
    }

    @Test
    void archivoVacioOMuyCorto_esInvalido() {
        assertThrows(DatoInvalidoException.class,
                () -> servicio.guardar(1, archivo("image/png", new byte[0]), Rol.OPERADOR));
        assertThrows(DatoInvalidoException.class,
                () -> servicio.guardar(1, archivo("image/png", new byte[]{(byte) 0xFF}), Rol.OPERADOR));
    }

    @Test
    void muestraValidada_daConflicto() {
        muestra.setEstado(EstadoRevision.VALIDADO);
        assertThrows(ConflictoException.class,
                () -> servicio.guardar(1, archivo("image/png", PNG), Rol.OPERADOR));
    }

    @Test
    void alReemplazarPngPorJpeg_borraLaAnterior() throws Exception {
        servicio.guardar(1, archivo("image/png", PNG), Rol.OPERADOR);
        servicio.guardar(1, archivo("image/jpeg", JPEG), Rol.OPERADOR);
        assertFalse(Files.exists(carpeta.resolve("muestra-1.png")));
        assertTrue(Files.exists(carpeta.resolve("muestra-1.jpg")));
    }

    @Test
    void mismoFormato_conservaLaFotoNueva() throws Exception {
        servicio.guardar(1, archivo("image/png", PNG), Rol.OPERADOR);
        servicio.guardar(1, archivo("image/png", PNG), Rol.OPERADOR);
        assertTrue(Files.exists(carpeta.resolve("muestra-1.png")));
    }

    @Test
    void borrarArchivo_loEliminaYAceptaNull() throws Exception {
        servicio.guardar(1, archivo("image/png", PNG), Rol.OPERADOR);
        servicio.borrarArchivo("muestra-1.png");
        servicio.borrarArchivo(null);
        assertFalse(Files.exists(carpeta.resolve("muestra-1.png")));
    }

    @Test
    void supervisor_noSubeFotos() {
        assertThrows(AccionNoPermitidaException.class,
                () -> servicio.guardar(1, archivo("image/png", PNG), Rol.SUPERVISOR));
    }
}
