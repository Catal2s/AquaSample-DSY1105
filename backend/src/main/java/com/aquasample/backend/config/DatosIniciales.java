package com.aquasample.backend.config;

import com.aquasample.backend.model.EstadoRevision;
import com.aquasample.backend.model.Muestra;
import com.aquasample.backend.repository.MuestraRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Carga las mismas 5 muestras ficticias que tiene la app (DatosFicticios.kt)
 * solo si la base esta vacia, para que la demo no parta en blanco.
 */
@Component
public class DatosIniciales implements CommandLineRunner {

    private final MuestraRepository repositorio;

    public DatosIniciales(MuestraRepository repositorio) {
        this.repositorio = repositorio;
    }

    @Override
    public void run(String... args) {
        if (repositorio.count() > 0) {
            return;
        }
        repositorio.save(muestra(1, "C1", "T1", "L1", "2026-09-22", "09:15", "U1", 312, 38.5, "Muestra sin novedades", EstadoRevision.VALIDADO, null));
        repositorio.save(muestra(2, "C1", "T1", "L2", "2026-09-22", "10:40", "U1", 287, null, "", EstadoRevision.OBSERVADO, "Revisar el conteo, parece bajo para esta linea"));
        repositorio.save(muestra(3, "C2", "T3", "L4", "2026-09-24", "11:05", "U2", 401, 42.0, "Alta densidad de individuos", EstadoRevision.VALIDADO, null));
        repositorio.save(muestra(4, "C1", "T2", "L3", "2026-09-29", "08:50", "U2", 265, null, "Tramo con algo de fouling", EstadoRevision.CORREGIDO, null));
        repositorio.save(muestra(5, "C2", "T3", "L5", "2026-10-01", "09:30", "U1", 298, 40.2, "", EstadoRevision.PENDIENTE, null));
    }

    private Muestra muestra(int id, String centro, String tren, String linea, String fecha, String hora,
                            String operador, int individuos, Double calibre, String obs,
                            EstadoRevision estado, String comentario) {
        Muestra m = new Muestra();
        m.setId(id);
        m.setCentroId(centro);
        m.setTrenId(tren);
        m.setLineaId(linea);
        m.setFecha(fecha);
        m.setHora(hora);
        m.setTramoMetros(id == 3 ? 0.3 : 0.2);
        m.setOperadorId(operador);
        m.setIndividuos(individuos);
        m.setCalibrePromedioMm(calibre);
        m.setObservaciones(obs);
        m.setEstado(estado);
        m.setComentarioSupervisor(comentario);
        m.setCreadoEn(LocalDateTime.now());
        m.setActualizadoEn(LocalDateTime.now());
        return m;
    }
}
