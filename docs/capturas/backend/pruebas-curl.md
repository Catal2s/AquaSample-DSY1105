# Pruebas con curl: ciclo de revisión

Pasos del ciclo que no tienen captura de Swagger. Se hicieron con el servidor real y una base de datos temporal recién creada, con las 5 muestras ficticias. Las peticiones y respuestas están copiadas tal como salieron (solo se ordenó el JSON en varias líneas).

- **Responsable:** Cristobal Gonzalez (Backend)
- **Fecha:** 23-11-2026
- **Rama:** `feature/backend-spring`

## 1. Observar sin comentario (400)

Petición:

```bash
curl -X PATCH http://localhost:8080/api/muestras/4/estado \
  -H 'X-Rol: SUPERVISOR' \
  -H 'Content-Type: application/json' \
  -d '{"estado":"OBSERVADO"}'
```

Respuesta: **HTTP 400**

```json
{
  "status": 400,
  "error": "Bad Request",
  "mensajes": [
    "Para solicitar correccion se debe escribir un comentario"
  ],
  "fecha": "2026-11-23T23:13:06"
}
```

## 2. La muestra 4 no cambió

Petición:

```bash
curl -X GET http://localhost:8080/api/muestras/4
```

Respuesta: **HTTP 200**

```json
{
  "id": 4,
  "centroId": "C1",
  "trenId": "T2",
  "lineaId": "L3",
  "fecha": "2026-09-29",
  "hora": "08:50",
  "tramoMetros": 0.2,
  "operadorId": "U2",
  "individuos": 265,
  "calibrePromedioMm": null,
  "asistidoPorFoto": false,
  "marcasRegistradas": 0,
  "ajusteManual": 0,
  "observaciones": "Tramo con algo de fouling",
  "estado": "CORREGIDO",
  "comentarioSupervisor": null,
  "tieneFoto": false,
  "fotoTipo": null,
  "fotoTamanoBytes": null,
  "creadoEn": "2026-11-23T23:13:05",
  "actualizadoEn": "2026-11-23T23:13:05"
}
```

## 3. Estado inicial de la muestra 5 (PENDIENTE)

Petición:

```bash
curl -X GET http://localhost:8080/api/muestras/5
```

Respuesta: **HTTP 200**

```json
{
  "id": 5,
  "centroId": "C2",
  "trenId": "T3",
  "lineaId": "L5",
  "fecha": "2026-10-01",
  "hora": "09:30",
  "tramoMetros": 0.2,
  "operadorId": "U1",
  "individuos": 298,
  "calibrePromedioMm": 40.2,
  "asistidoPorFoto": false,
  "marcasRegistradas": 0,
  "ajusteManual": 0,
  "observaciones": "",
  "estado": "PENDIENTE",
  "comentarioSupervisor": null,
  "tieneFoto": false,
  "fotoTipo": null,
  "fotoTamanoBytes": null,
  "creadoEn": "2026-11-23T23:13:05",
  "actualizadoEn": "2026-11-23T23:13:05"
}
```

## 4. Supervisor observa la muestra 5 (OBSERVADO)

Petición:

```bash
curl -X PATCH http://localhost:8080/api/muestras/5/estado \
  -H 'X-Rol: SUPERVISOR' \
  -H 'Content-Type: application/json' \
  -d '{"estado":"OBSERVADO","comentario":"Revisar el conteo, parece bajo para esta linea"}'
```

Respuesta: **HTTP 200**

```json
{
  "id": 5,
  "centroId": "C2",
  "trenId": "T3",
  "lineaId": "L5",
  "fecha": "2026-10-01",
  "hora": "09:30",
  "tramoMetros": 0.2,
  "operadorId": "U1",
  "individuos": 298,
  "calibrePromedioMm": 40.2,
  "asistidoPorFoto": false,
  "marcasRegistradas": 0,
  "ajusteManual": 0,
  "observaciones": "",
  "estado": "OBSERVADO",
  "comentarioSupervisor": "Revisar el conteo, parece bajo para esta linea",
  "tieneFoto": false,
  "fotoTipo": null,
  "fotoTamanoBytes": null,
  "creadoEn": "2026-11-23T23:13:05",
  "actualizadoEn": "2026-11-23T23:13:06"
}
```

## 5. Operador corrige los datos con PUT (sigue OBSERVADO)

Petición:

```bash
curl -X PUT http://localhost:8080/api/muestras/5 \
  -H 'X-Rol: OPERADOR' \
  -H 'Content-Type: application/json' \
  -d '{"id":5,"centroId":"C2","trenId":"T3","lineaId":"L5","fecha":"2026-10-01","hora":"09:30","tramoMetros":0.2,"operadorId":"U1","individuos":310,"calibrePromedioMm":40.2,"observaciones":"Conteo revisado"}'
```

Respuesta: **HTTP 200**

```json
{
  "id": 5,
  "centroId": "C2",
  "trenId": "T3",
  "lineaId": "L5",
  "fecha": "2026-10-01",
  "hora": "09:30",
  "tramoMetros": 0.2,
  "operadorId": "U1",
  "individuos": 310,
  "calibrePromedioMm": 40.2,
  "asistidoPorFoto": false,
  "marcasRegistradas": 0,
  "ajusteManual": 0,
  "observaciones": "Conteo revisado",
  "estado": "OBSERVADO",
  "comentarioSupervisor": "Revisar el conteo, parece bajo para esta linea",
  "tieneFoto": false,
  "fotoTipo": null,
  "fotoTamanoBytes": null,
  "creadoEn": "2026-11-23T23:13:05",
  "actualizadoEn": "2026-11-23T23:13:07"
}
```

## 6. Operador marca la muestra 5 como CORREGIDO

Petición:

```bash
curl -X PATCH http://localhost:8080/api/muestras/5/estado \
  -H 'X-Rol: OPERADOR' \
  -H 'Content-Type: application/json' \
  -d '{"estado":"CORREGIDO"}'
```

Respuesta: **HTTP 200**

```json
{
  "id": 5,
  "centroId": "C2",
  "trenId": "T3",
  "lineaId": "L5",
  "fecha": "2026-10-01",
  "hora": "09:30",
  "tramoMetros": 0.2,
  "operadorId": "U1",
  "individuos": 310,
  "calibrePromedioMm": 40.2,
  "asistidoPorFoto": false,
  "marcasRegistradas": 0,
  "ajusteManual": 0,
  "observaciones": "Conteo revisado",
  "estado": "CORREGIDO",
  "comentarioSupervisor": "Revisar el conteo, parece bajo para esta linea",
  "tieneFoto": false,
  "fotoTipo": null,
  "fotoTamanoBytes": null,
  "creadoEn": "2026-11-23T23:13:05",
  "actualizadoEn": "2026-11-23T23:13:07"
}
```

## 7. Supervisor valida la muestra 5 (VALIDADO)

Petición:

```bash
curl -X PATCH http://localhost:8080/api/muestras/5/estado \
  -H 'X-Rol: SUPERVISOR' \
  -H 'Content-Type: application/json' \
  -d '{"estado":"VALIDADO"}'
```

Respuesta: **HTTP 200**

```json
{
  "id": 5,
  "centroId": "C2",
  "trenId": "T3",
  "lineaId": "L5",
  "fecha": "2026-10-01",
  "hora": "09:30",
  "tramoMetros": 0.2,
  "operadorId": "U1",
  "individuos": 310,
  "calibrePromedioMm": 40.2,
  "asistidoPorFoto": false,
  "marcasRegistradas": 0,
  "ajusteManual": 0,
  "observaciones": "Conteo revisado",
  "estado": "VALIDADO",
  "comentarioSupervisor": null,
  "tieneFoto": false,
  "fotoTipo": null,
  "fotoTamanoBytes": null,
  "creadoEn": "2026-11-23T23:13:05",
  "actualizadoEn": "2026-11-23T23:13:07"
}
```

## 8. Resumen final

Petición:

```bash
curl -X GET http://localhost:8080/api/reportes/resumen
```

Respuesta: **HTTP 200**

```json
{
  "totalMuestras": 5,
  "totalIndividuos": 1575,
  "promedioIndividuos": 315,
  "muestrasPorEstado": {
    "CORREGIDO": 1,
    "OBSERVADO": 1,
    "PENDIENTE": 0,
    "VALIDADO": 3
  },
  "muestrasPorCentro": {
    "C1": 3,
    "C2": 2
  }
}
```
