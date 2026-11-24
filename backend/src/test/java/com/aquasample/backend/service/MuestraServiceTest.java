package com.aquasample.backend.service;

import com.aquasample.backend.dto.CambioEstadoRequest;
import com.aquasample.backend.dto.MuestraRequest;
import com.aquasample.backend.exception.AccionNoPermitidaException;
import com.aquasample.backend.exception.ConflictoException;
import com.aquasample.backend.exception.DatoInvalidoException;
import com.aquasample.backend.model.EstadoRevision;
import com.aquasample.backend.model.Muestra;
import com.aquasample.backend.model.Rol;
import com.aquasample.backend.repository.MuestraRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Pruebas de las reglas de negocio, sin base de datos (repositorio simulado con Mockito). */
class MuestraServiceTest {

    private MuestraRepository repositorio;
    private FotoService fotoService;
    private MuestraService servicio;

    @BeforeEach
    void preparar() {
        repositorio = mock(MuestraRepository.class);
        fotoService = mock(FotoService.class);
        servicio = new MuestraService(repositorio, fotoService);
        when(repositorio.save(any(Muestra.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private MuestraRequest datos(Integer id) {
        return new MuestraRequest(id, "C1", "T1", "L1", "2026-11-24", "10:30", 0.2, "U1",
                300, 38.0, true, 290, 10, "Prueba");
    }

    private Muestra muestraEnEstado(EstadoRevision estado) {
        Muestra m = new Muestra();
        m.setId(1);
        m.setOperadorId("U1");
        m.setEstado(estado);
        when(repositorio.findById(1)).thenReturn(Optional.of(m));
        return m;
    }

    @Test
    void crear_sinId_asignaSiguienteYQuedaPendiente() {
        when(repositorio.maxId()).thenReturn(5);
        Muestra creada = servicio.crear(datos(null), Rol.OPERADOR);
        assertEquals(6, creada.getId());
        assertEquals(EstadoRevision.PENDIENTE, creada.getEstado());
    }

    @Test
    void crear_conSupervisor_noPermitido() {
        assertThrows(AccionNoPermitidaException.class, () -> servicio.crear(datos(null), Rol.SUPERVISOR));
    }

    @Test
    void crear_conIdExistente_daConflicto() {
        when(repositorio.existsById(7)).thenReturn(true);
        assertThrows(ConflictoException.class, () -> servicio.crear(datos(7), Rol.OPERADOR));
    }

    @Test
    void sincronizar_mismoId_actualizaSinDuplicar() {
        Muestra existente = muestraEnEstado(EstadoRevision.PENDIENTE);
        Muestra resultado = servicio.guardarOSincronizar(1, datos(1), Rol.OPERADOR);
        assertEquals(existente, resultado);
        assertEquals(300, resultado.getIndividuos());
    }

    @Test
    void sincronizar_muestraValidada_daConflicto() {
        muestraEnEstado(EstadoRevision.VALIDADO);
        assertThrows(ConflictoException.class, () -> servicio.guardarOSincronizar(1, datos(1), Rol.OPERADOR));
    }

    @Test
    void supervisor_validaMuestraPendiente() {
        muestraEnEstado(EstadoRevision.PENDIENTE);
        Muestra r = servicio.cambiarEstado(1, new CambioEstadoRequest(EstadoRevision.VALIDADO, null), Rol.SUPERVISOR);
        assertEquals(EstadoRevision.VALIDADO, r.getEstado());
    }

    @Test
    void supervisor_observarSinComentario_esInvalido() {
        muestraEnEstado(EstadoRevision.PENDIENTE);
        assertThrows(DatoInvalidoException.class,
                () -> servicio.cambiarEstado(1, new CambioEstadoRequest(EstadoRevision.OBSERVADO, "  "), Rol.SUPERVISOR));
    }

    @Test
    void operador_marcaCorregidaUnaObservada() {
        muestraEnEstado(EstadoRevision.OBSERVADO);
        Muestra r = servicio.cambiarEstado(1, new CambioEstadoRequest(EstadoRevision.CORREGIDO, null), Rol.OPERADOR);
        assertEquals(EstadoRevision.CORREGIDO, r.getEstado());
    }

    @Test
    void operador_noPuedeValidar() {
        muestraEnEstado(EstadoRevision.PENDIENTE);
        assertThrows(AccionNoPermitidaException.class,
                () -> servicio.cambiarEstado(1, new CambioEstadoRequest(EstadoRevision.VALIDADO, null), Rol.OPERADOR));
    }

    @Test
    void mismoEstado_noGuardaDeNuevo() {
        muestraEnEstado(EstadoRevision.VALIDADO);
        servicio.cambiarEstado(1, new CambioEstadoRequest(EstadoRevision.VALIDADO, null), Rol.SUPERVISOR);
        verify(repositorio, never()).save(any(Muestra.class));
    }

    @Test
    void analista_noPuedeEliminar() {
        assertThrows(AccionNoPermitidaException.class, () -> servicio.eliminar(1, Rol.ANALISTA));
    }

    private MuestraRequest datosCon(Integer id, String fecha, String hora, String operador) {
        return new MuestraRequest(id, "C1", "T1", "L1", fecha, hora, 0.2, operador,
                300, 38.0, true, 290, 10, "Prueba");
    }

    @Test
    void sincronizar_otroOperador_daConflictoYNoGuarda() {
        muestraEnEstado(EstadoRevision.PENDIENTE);
        MuestraRequest deOtro = datosCon(1, "2026-11-24", "10:30", "U2");
        assertThrows(ConflictoException.class, () -> servicio.guardarOSincronizar(1, deOtro, Rol.OPERADOR));
        verify(repositorio, never()).save(any(Muestra.class));
    }

    @Test
    void sincronizar_idDelCuerpoDistinto_esInvalido() {
        assertThrows(DatoInvalidoException.class, () -> servicio.guardarOSincronizar(1, datos(99), Rol.OPERADOR));
    }

    @Test
    void sincronizar_sinIdEnElCuerpo_usaElDeLaRuta() {
        when(repositorio.findById(200)).thenReturn(Optional.empty());
        assertEquals(200, servicio.guardarOSincronizar(200, datos(null), Rol.OPERADOR).getId());
    }

    @Test
    void sincronizar_idCeroONegativo_esInvalido() {
        assertThrows(DatoInvalidoException.class, () -> servicio.guardarOSincronizar(0, datos(null), Rol.OPERADOR));
        assertThrows(DatoInvalidoException.class, () -> servicio.guardarOSincronizar(-3, datos(null), Rol.OPERADOR));
    }

    @Test
    void fechaUHoraQueNoExisten_sonInvalidas() {
        assertThrows(DatoInvalidoException.class,
                () -> servicio.crear(datosCon(null, "2026-02-30", "10:30", "U1"), Rol.OPERADOR));
        assertThrows(DatoInvalidoException.class,
                () -> servicio.guardarOSincronizar(1, datosCon(1, "2026-11-24", "24:00", "U1"), Rol.OPERADOR));
    }

    @Test
    void cambiarEstado_sinPermiso_noPermitidoAunqueSeaElMismoEstado() {
        muestraEnEstado(EstadoRevision.VALIDADO);
        CambioEstadoRequest validar = new CambioEstadoRequest(EstadoRevision.VALIDADO, null);
        assertThrows(AccionNoPermitidaException.class, () -> servicio.cambiarEstado(1, validar, Rol.ANALISTA));
        assertThrows(AccionNoPermitidaException.class, () -> servicio.cambiarEstado(1, validar, Rol.OPERADOR));
    }

    @Test
    void operador_noCorrigeUnaPendiente() {
        muestraEnEstado(EstadoRevision.PENDIENTE);
        assertThrows(ConflictoException.class,
                () -> servicio.cambiarEstado(1, new CambioEstadoRequest(EstadoRevision.CORREGIDO, null), Rol.OPERADOR));
    }

    @Test
    void eliminar_borraTambienLaFoto() {
        Muestra m = muestraEnEstado(EstadoRevision.PENDIENTE);
        m.setFotoNombre("muestra-1.png");
        servicio.eliminar(1, Rol.SUPERVISOR);
        verify(repositorio).delete(m);
        verify(fotoService).borrarArchivo("muestra-1.png");
    }
}
