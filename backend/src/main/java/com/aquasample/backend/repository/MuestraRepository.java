package com.aquasample.backend.repository;

import com.aquasample.backend.model.EstadoRevision;
import com.aquasample.backend.model.Muestra;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

/** Acceso a la tabla muestras. JpaRepository ya trae save, findById, findAll y delete. */
public interface MuestraRepository extends JpaRepository<Muestra, Integer> {

    List<Muestra> findByEstadoOrderByFechaDescHoraDesc(EstadoRevision estado);

    /** Mayor id usado, para asignar uno nuevo cuando el POST no trae id. */
    @Query("select coalesce(max(m.id), 0) from Muestra m")
    int maxId();
}
