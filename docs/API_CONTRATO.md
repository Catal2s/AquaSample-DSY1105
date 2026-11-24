# Contrato de la API · AquaSample

Microservicio propio (Spring Boot) que recibe las muestras de la app. Este documento es la referencia para Retrofit (Cristobal), la sincronización con WorkManager (Javier, en desarrollo), el dashboard (Daniel) y las pruebas (Vicente).

- URL base en el PC: `http://localhost:8080`
- URL base desde el **emulador Android**: `http://10.0.2.2:8080`
- URL base desde un **teléfono físico**: `http://IP_DEL_PC:8080` (ver abajo)
- Documentación interactiva: `http://localhost:8080/swagger-ui.html`
- Formato: JSON. Fechas `yyyy-MM-dd`, horas `HH:mm` (igual que en la app). Las fechas con hora de salida son `yyyy-MM-ddTHH:mm:ss`, sin fracciones.

## Conectar la app desde un teléfono físico

El teléfono y el PC deben estar en la misma red WiFi.

1. En el PC, ejecutar `ipconfig` y copiar la **Dirección IPv4** del adaptador WiFi (por ejemplo `192.168.1.50`).
2. En `local.properties`, en la raíz del proyecto (no se sube a GitHub), agregar:
   ```properties
   backend.url=http://192.168.1.50:8080/
   ```
   Sin esa línea se usa `http://10.0.2.2:8080/`, la del emulador.
3. Volver a **compilar e instalar** la app. La IP queda dentro del APK (debug o release): si cambia la red, hay que repetir los pasos. Al compilar se genera `network_security_config.xml`, que permite HTTP solo hacia esa IP.
4. Levantar el backend y, si Windows lo pregunta, permitir Java en el Firewall para la red en uso.
5. Comprobar en el navegador del teléfono: `http://192.168.1.50:8080/api/muestras`. Si no abre, es la red o el Firewall (algunas redes WiFi no dejan que los equipos se vean entre sí; se puede usar la zona WiFi de un teléfono).
6. Desde Android 17 la app pide permiso para conectarse a dispositivos cercanos (red local): hay que **permitirlo**, o no llegará al backend.

## Roles

Las operaciones de escritura llevan el header **`X-Rol`** con `OPERADOR`, `SUPERVISOR` o `ANALISTA`. Las consultas (GET) no lo necesitan. Si falta o no existe ese rol, responde 400. No hay login: el servidor confía en el header (en producción se usaría Spring Security).

| Acción | OPERADOR | SUPERVISOR | ANALISTA |
|---|---|---|---|
| Consultar muestras y reportes | Sí | Sí | Sí |
| Crear / actualizar muestra | Sí | No | No |
| Subir foto | Sí (si no está VALIDADO) | No | No |
| Validar u observar | No | Sí | No |
| Marcar corregida | Sí (si está OBSERVADO) | No | No |
| Eliminar | No | Sí | No |

`ANALISTA` existe solo en el backend: la app no tiene un usuario con ese rol.

## Endpoints

| Método | Ruta | Qué hace | Respuesta |
|---|---|---|---|
| GET | `/api/muestras` | Lista con filtros opcionales: `centroId`, `trenId`, `lineaId`, `estado`, `desde`, `hasta` (las fechas se comparan como texto y no se validan) | 200 + lista / 400 |
| GET | `/api/muestras/{id}` | Una muestra | 200 / 400 / 404 |
| POST | `/api/muestras` | Crea una muestra (queda PENDIENTE). Si no trae `id`, se asigna el siguiente | 201 / 400 / 403 / 409 / 415 |
| PUT | `/api/muestras/{id}` | **Crea o actualiza con el id de la app.** Es el que usa la sincronización: si se reintenta, actualiza la misma fila (sin duplicados). Responde 200 también cuando la crea | 200 / 400 / 403 / 409 / 415 |
| PATCH | `/api/muestras/{id}/estado` | Cambia el estado de revisión según el rol | 200 / 400 / 403 / 404 / 409 / 415 |
| DELETE | `/api/muestras/{id}` | Elimina una muestra y su foto | 204 / 400 / 403 / 404 |
| POST | `/api/muestras/{id}/foto` | Sube la foto (multipart, campo `archivo`). Solo JPEG o PNG, hasta 10 MB. Reemplaza la anterior | 200 / 400 / 403 / 404 / 409 / 413 / 415 |
| GET | `/api/muestras/{id}/foto` | Descarga la foto | 200 (imagen) / 400 / 404 |
| GET | `/api/reportes/resumen` | Indicadores de las muestras. Disponible, pero la app hoy no lo usa | 200 |
| GET | `/api/reportes/validadas` | Muestras validadas, para exportar a Excel o PDF. Disponible, pero la app hoy no lo usa | 200 + lista |

## JSON de entrada (POST y PUT)

```json
{
  "id": 7,
  "centroId": "C1",
  "trenId": "T1",
  "lineaId": "L1",
  "fecha": "2026-11-24",
  "hora": "10:30",
  "tramoMetros": 0.2,
  "operadorId": "U1",
  "individuos": 312,
  "calibrePromedioMm": 38.5,
  "asistidoPorFoto": true,
  "marcasRegistradas": 300,
  "ajusteManual": 12,
  "observaciones": "Muestra sin novedades"
}
```

Validaciones (si alguna falla, 400):

- `id`: mayor a 0. Opcional en POST. En PUT también es opcional, pero si viene debe coincidir con el de la ruta.
- `centroId`, `trenId`, `lineaId`, `operadorId`: obligatorios, hasta 50 caracteres. No se comparan con un catálogo.
- `fecha` (`yyyy-MM-dd`) y `hora` (`HH:mm`): obligatorias y deben existir de verdad (`2026-02-30` y `24:00` se rechazan).
- `tramoMetros` ≥ 0.01; `individuos` ≥ 1; `marcasRegistradas` ≥ 0.
- `calibrePromedioMm`: opcional; si viene, mayor a 0.
- `observaciones`: opcional, hasta 500 caracteres.

El estado **no** se envía aquí: solo cambia con PATCH.

### Límite conocido: ids repetidos entre teléfonos

La app genera el id como "el mayor + 1" en cada teléfono, así que dos teléfonos pueden crear el mismo id.

- Si el PUT llega con un `operadorId` distinto al guardado, responde **409** y no se pierde la muestra guardada.
- Si es **el mismo operador en dos teléfonos** (o reinstala la app), **no se detecta**: el segundo PUT reemplaza al primero, salvo que la muestra ya esté validada (409).
- Si un POST del mismo operador ya ocupó ese id, pasa lo mismo.

La solución real sería que la app genere el id como UUID.

## JSON de salida (muestra)

```json
{
  "id": 7,
  "centroId": "C1",
  "trenId": "T1",
  "lineaId": "L1",
  "fecha": "2026-11-24",
  "hora": "10:30",
  "tramoMetros": 0.2,
  "operadorId": "U1",
  "individuos": 312,
  "calibrePromedioMm": 38.5,
  "asistidoPorFoto": true,
  "marcasRegistradas": 300,
  "ajusteManual": 12,
  "observaciones": "Muestra sin novedades",
  "estado": "PENDIENTE",
  "comentarioSupervisor": null,
  "tieneFoto": false,
  "fotoTipo": null,
  "fotoTamanoBytes": null,
  "creadoEn": "2026-11-24T10:31:02",
  "actualizadoEn": "2026-11-24T10:31:02"
}
```

## Cambio de estado (PATCH `/api/muestras/{id}/estado`)

```json
{ "estado": "OBSERVADO", "comentario": "Revisar el conteo, parece bajo" }
```

- SUPERVISOR: de `PENDIENTE` o `CORREGIDO` a `VALIDADO` u `OBSERVADO`. Para `OBSERVADO` el comentario es obligatorio (hasta 500 caracteres). Al validar sin comentario se borra el anterior.
- OPERADOR: de `OBSERVADO` a `CORREGIDO`. Se conserva el comentario del supervisor.
- ANALISTA: no puede cambiar estados (403).
- Primero se revisa el permiso del rol (403). Si tiene permiso y la muestra ya está en el estado pedido, responde 200 sin cambios (los reintentos de la sincronización no fallan).
- Una muestra `VALIDADO` ya no se puede editar con PUT ni cambiar su foto (409).

### Cómo se corrige una muestra observada

Con `X-Rol: OPERADOR`, en este orden:

1. **PUT** `/api/muestras/{id}` con los datos corregidos. Es **opcional**: solo aplica si se editan datos. La muestra **sigue en `OBSERVADO`**.
2. **PATCH** `/api/muestras/{id}/estado` con `{ "estado": "CORREGIDO" }`.

**Hoy la app envía solo el PATCH**, sin PUT previo, porque no permite editar los datos. (Si los datos originales aún no llegaron al servidor, antes se envía el PUT de la muestra nueva.)

## Resumen (GET `/api/reportes/resumen`)

El dashboard de la app no usa este endpoint: se calcula con los datos de Room.

```json
{
  "totalMuestras": 5,
  "totalIndividuos": 1563,
  "promedioIndividuos": 312.6,
  "muestrasPorEstado": { "CORREGIDO": 1, "OBSERVADO": 1, "PENDIENTE": 1, "VALIDADO": 2 },
  "muestrasPorCentro": { "C1": 3, "C2": 2 }
}
```

## Errores

Todos los errores de la API tienen el mismo formato:

```json
{
  "status": 400,
  "error": "Bad Request",
  "mensajes": ["individuos: Los individuos deben ser al menos 1"],
  "fecha": "2026-11-24T10:31:02"
}
```

| Código | Cuándo |
|---|---|
| 400 | Validación fallida, falta el header `X-Rol` o el archivo, la foto no es JPEG o PNG, o un valor con formato inválido |
| 403 | El rol no puede hacer esa acción |
| 404 | No existe la muestra, la foto o la ruta |
| 405 | Método no permitido en esa ruta |
| 406 | El `Accept` pedido no está disponible |
| 409 | Id ya existente en POST, PUT de otro operador, editar una muestra validada (o su foto) o un cambio de estado no válido |
| 413 | La foto supera 10 MB |
| 415 | `Content-Type` no soportado |
| 500 | Error interno (sin detalles; quedan en el log del servidor) |

## Notas para la sincronización

**Cuándo se envía hoy:** al guardar, revisar (aprobar, pedir corrección o marcar corregida) o eliminar una muestra, y con el botón **Sincronizar** del Historial. El reintento automático con **WorkManager está en desarrollo (Javier)**.

1. Si la muestra tiene datos por enviar, se mandan con **PUT** `/api/muestras/{id}` y header `X-Rol: OPERADOR`.
2. Si tiene una foto por enviar, se sube después con POST `/api/muestras/{id}/foto`, antes de que sea validada.
3. Los cambios de estado se envían con PATCH, en orden y con el rol de quien los hizo. **Si solo cambió el estado, se envía solo el PATCH.**
4. Con respuesta 2xx se marca la muestra como sincronizada en Room. Con 409 no se reintenta: repetir el mismo envío daría lo mismo. El campo `mensajes` indica la causa.
5. Las muestras de ejemplo 1 a 5 ya existen en el servidor. Las 1 y 3 están validadas, así que su PUT responde 409.
6. Al eliminar una muestra que ya está en el servidor, queda oculta como **eliminación pendiente** y se envía el **DELETE** con el rol de quien la eliminó. Un **404 cuenta como éxito**, porque ya no existía. Si nunca llegó al servidor, se borra solo del teléfono.
7. Cuando esté listo, el Worker debe llamar a `MuestraRepository.instancia.sincronizarPendientes()`: devuelve `true` si no queda nada que reintentar y `false` si hay que reintentar más tarde.
