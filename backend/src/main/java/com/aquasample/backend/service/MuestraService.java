package com.aquasample.backend.service;

import com.aquasample.backend.dto.CambioEstadoRequest;
import com.aquasample.backend.dto.MuestraRequest;
import com.aquasample.backend.dto.ResumenResponse;
import com.aquasample.backend.exception.AccionNoPermitidaException;
import com.aquasample.backend.exception.ConflictoException;
import com.aquasample.backend.exception.DatoInvalidoException;
import com.aquasample.backend.exception.RecursoNoEncontradoException;
import com.aquasample.backend.model.EstadoRevision;
import com.aquasample.backend.model.Muestra;
import com.aquasample.backend.model.Rol;
import com.aquasample.backend.repository.MuestraRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * Logica de negocio de las muestras: crear, sincronizar, filtrar,
 * cambiar estado segun rol y calcular indicadores.
 */
@Service
public class MuestraService {

    private static final Comparator<Muestra> MAS_RECIENTE_PRIMERO =
            Comparator.comparing(Muestra::getFecha).thenComparing(Muestra::getHora).reversed();

    private final MuestraRepository repositorio;
    private final FotoService fotoService;

    public MuestraService(MuestraRepository repositorio, FotoService fotoService) {
        this.repositorio = repositorio;
        this.fotoService = fotoService;
    }

    /** Lista con filtros opcionales. Fechas en formato yyyy-MM-dd (se comparan como texto). */
    @Transactional(readOnly = true)
    public List<Muestra> listar(String centroId, String trenId, String lineaId,
                                EstadoRevision estado, String desde, String hasta) {
        return repositorio.findAll().stream()
                .filter(m -> centroId == null || m.getCentroId().equals(centroId))
                .filter(m -> trenId == null || m.getTrenId().equals(trenId))
                .filter(m -> lineaId == null || m.getLineaId().equals(lineaId))
                .filter(m -> estado == null || m.getEstado() == estado)
                .filter(m -> desde == null || m.getFecha().compareTo(desde) >= 0)
                .filter(m -> hasta == null || m.getFecha().compareTo(hasta) <= 0)
                .sorted(MAS_RECIENTE_PRIMERO)
                .toList();
    }

    @Transactional(readOnly = true)
    public Muestra obtener(int id) {
        return repositorio.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe la muestra " + id));
    }

    /** POST: crea una muestra nueva. Si no trae id, se asigna el siguiente. */
    @Transactional
    public Muestra crear(MuestraRequest datos, Rol rol) {
        exigirRol(rol, Rol.OPERADOR, "Solo el operador registra muestras");
        validarFechaYHora(datos);
        if (datos.id() != null && repositorio.existsById(datos.id())) {
            throw new ConflictoException("Ya existe la muestra " + datos.id() + ". Usa PUT para actualizarla");
        }
        Muestra nueva = new Muestra();
        nueva.setId(datos.id() != null ? datos.id() : repositorio.maxId() + 1);
        copiarDatos(datos, nueva);
        nueva.setEstado(EstadoRevision.PENDIENTE);
        nueva.setCreadoEn(LocalDateTime.now());
        nueva.setActualizadoEn(LocalDateTime.now());
        return repositorio.save(nueva);
    }

    /**
     * PUT: crea o actualiza con el id de la app (usado por la sincronizacion).
     * Si la app reintenta el envio, se actualiza la misma fila: no hay duplicados.
     */
    @Transactional
    public Muestra guardarOSincronizar(int id, MuestraRequest datos, Rol rol) {
        exigirRol(rol, Rol.OPERADOR, "Solo el operador registra o edita muestras");
        if (id <= 0) {
            throw new DatoInvalidoException("id: El id debe ser mayor a 0");
        }
        if (datos.id() != null && datos.id() != id) {
            throw new DatoInvalidoException("id: El id del cuerpo (" + datos.id() + ") no coincide con el de la ruta (" + id + ")");
        }
        validarFechaYHora(datos);
        Muestra muestra = repositorio.findById(id).orElse(null);
        if (muestra == null) {
            muestra = new Muestra();
            muestra.setId(id);
            muestra.setEstado(EstadoRevision.PENDIENTE);
            muestra.setCreadoEn(LocalDateTime.now());
        } else if (!muestra.getOperadorId().equals(datos.operadorId())) {
            // Dos telefonos generaron el mismo id: no se pisa la muestra del otro operador.
            throw new ConflictoException("La muestra " + id + " ya pertenece a otro operador ("
                    + muestra.getOperadorId() + ") y no se puede sobrescribir");
        } else if (muestra.getEstado() == EstadoRevision.VALIDADO) {
            throw new ConflictoException("La muestra " + id + " ya fue validada y no se puede editar");
        }
        copiarDatos(datos, muestra);
        muestra.setActualizadoEn(LocalDateTime.now());
        return repositorio.save(muestra);
    }

    /**
     * PATCH estado. Reglas por rol:
     * SUPERVISOR: PENDIENTE o CORREGIDO pasa a VALIDADO u OBSERVADO (OBSERVADO exige comentario).
     * OPERADOR: OBSERVADO pasa a CORREGIDO.
     * ANALISTA: no puede cambiar estados.
     * Si el rol tiene permiso y la muestra ya esta en el estado pedido, no se hace nada (reintentos seguros).
     */
    @Transactional
    public Muestra cambiarEstado(int id, CambioEstadoRequest cambio, Rol rol) {
        EstadoRevision nuevo = cambio.estado();
        // Primero el permiso: un rol sin permiso recibe 403 aunque el estado no vaya a cambiar.
        exigirPermisoDeEstado(rol, nuevo);

        Muestra muestra = obtener(id);
        EstadoRevision actual = muestra.getEstado();
        if (actual == nuevo) {
            return muestra;
        }

        if (rol == Rol.SUPERVISOR) {
            if (actual != EstadoRevision.PENDIENTE && actual != EstadoRevision.CORREGIDO) {
                throw new ConflictoException("Solo se revisan muestras pendientes o corregidas (estado actual: " + actual + ")");
            }
            if (nuevo == EstadoRevision.OBSERVADO && esVacio(cambio.comentario())) {
                throw new DatoInvalidoException("Para solicitar correccion se debe escribir un comentario");
            }
            muestra.setComentarioSupervisor(esVacio(cambio.comentario()) ? null : cambio.comentario().trim());
        } else if (actual != EstadoRevision.OBSERVADO) {
            // Operador. Al corregir se mantiene el comentario del supervisor para que vea que pidio.
            throw new ConflictoException("Solo se corrigen muestras observadas (estado actual: " + actual + ")");
        }

        muestra.setEstado(nuevo);
        muestra.setActualizadoEn(LocalDateTime.now());
        return repositorio.save(muestra);
    }

    @Transactional
    public void eliminar(int id, Rol rol) {
        exigirRol(rol, Rol.SUPERVISOR, "Solo el supervisor puede eliminar muestras");
        Muestra muestra = obtener(id);
        repositorio.delete(muestra);
        // flush: si la base rechaza el borrado, falla aqui y la foto se conserva.
        repositorio.flush();
        fotoService.borrarArchivo(muestra.getFotoNombre());
    }

    /** Indicadores para el dashboard. */
    @Transactional(readOnly = true)
    public ResumenResponse resumen() {
        List<Muestra> todas = repositorio.findAll();
        long totalIndividuos = todas.stream().mapToLong(Muestra::getIndividuos).sum();
        double promedio = todas.isEmpty() ? 0.0 : (double) totalIndividuos / todas.size();

        Map<String, Long> porEstado = new TreeMap<>();
        for (EstadoRevision e : EstadoRevision.values()) {
            porEstado.put(e.name(), 0L);
        }
        todas.forEach(m -> porEstado.merge(m.getEstado().name(), 1L, Long::sum));

        Map<String, Long> porCentro = todas.stream()
                .collect(Collectors.groupingBy(Muestra::getCentroId, TreeMap::new, Collectors.counting()));

        return new ResumenResponse(todas.size(), totalIndividuos,
                Math.round(promedio * 10) / 10.0, porEstado, porCentro);
    }

    /** Registros validados, para exportar a Excel o PDF. */
    @Transactional(readOnly = true)
    public List<Muestra> validadas() {
        return repositorio.findByEstadoOrderByFechaDescHoraDesc(EstadoRevision.VALIDADO);
    }

    private void copiarDatos(MuestraRequest d, Muestra m) {
        m.setCentroId(d.centroId());
        m.setTrenId(d.trenId());
        m.setLineaId(d.lineaId());
        m.setFecha(d.fecha());
        m.setHora(d.hora());
        m.setTramoMetros(d.tramoMetros());
        m.setOperadorId(d.operadorId());
        m.setIndividuos(d.individuos());
        m.setCalibrePromedioMm(d.calibrePromedioMm());
        m.setAsistidoPorFoto(d.asistidoPorFoto());
        m.setMarcasRegistradas(d.marcasRegistradas());
        m.setAjusteManual(d.ajusteManual());
        m.setObservaciones(d.observaciones() == null ? "" : d.observaciones().trim());
    }

    /** El @Pattern revisa la forma; aqui se revisa que la fecha y la hora existan de verdad. */
    private void validarFechaYHora(MuestraRequest datos) {
        try {
            LocalDate.parse(datos.fecha());
        } catch (DateTimeParseException e) {
            throw new DatoInvalidoException("fecha: La fecha no existe");
        }
        try {
            LocalTime.parse(datos.hora());
        } catch (DateTimeParseException e) {
            throw new DatoInvalidoException("hora: La hora no existe");
        }
    }

    private void exigirPermisoDeEstado(Rol rol, EstadoRevision nuevo) {
        switch (rol) {
            case SUPERVISOR -> {
                if (nuevo != EstadoRevision.VALIDADO && nuevo != EstadoRevision.OBSERVADO) {
                    throw new AccionNoPermitidaException("El supervisor solo puede validar u observar");
                }
            }
            case OPERADOR -> {
                if (nuevo != EstadoRevision.CORREGIDO) {
                    throw new AccionNoPermitidaException("El operador solo puede marcar una muestra como corregida");
                }
            }
            default -> throw new AccionNoPermitidaException("El rol " + rol + " no puede cambiar estados");
        }
    }

    private void exigirRol(Rol actual, Rol requerido, String mensaje) {
        if (actual != requerido) {
            throw new AccionNoPermitidaException(mensaje);
        }
    }

    private boolean esVacio(String texto) {
        return texto == null || texto.isBlank();
    }
}
