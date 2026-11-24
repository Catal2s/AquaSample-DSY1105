package com.aquasample.backend.model;

/**
 * Roles que pueden llamar a la API. Se envian en el header X-Rol.
 * ANALISTA solo consulta historial y reportes.
 */
public enum Rol {
    OPERADOR,
    SUPERVISOR,
    ANALISTA
}
