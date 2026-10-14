# Tareas AquaSample (DSY1105, La Brigada Fantasma 01)

Estado al 14-10-2026, después del merge del PR #2 en `main` (commit `ea53045`). Revisado contra el código real del repositorio.

## Fechas clave (pizarra)

- Próxima semana (desde el lunes 19): salen 3 clases en video en AVA: (1) Filtros y Búsqueda, (2) Base de Datos Room (SQLite), (3) Fotografías y Galería de Imágenes.
- **Esa semana es la última para avanzar en la app.** Del 22 al 26 solo se corrigen errores y se prepara la demostración.
- **Evaluación 2: martes 27 y miércoles 28 de octubre.**

## Qué ya está hecho en `main`

- Proyecto Android con Kotlin, Jetpack Compose y Material 3, con la paleta AquaSample en `ui/theme`.
- Estructura MVVM: `model`, `data`, `viewmodel`, `ui/screens`, `ui/components`, `ui/navigation`.
- Navegación con Navigation Compose: Login → Inicio → Nueva muestra → Conteo → Resumen, más Historial.
- Login con roles (Operador y Supervisor) y usuarios ficticios: `operador1`, `operador2`, `supervisor1` (clave `1234`).
- Formulario de muestra con validación de campos y mensajes de error (centro, tren, línea, fecha, hora, tramo).
- Conteo manual de individuos con calibre opcional (RF06) y pantalla de resumen.
- Historial con filtros por centro, línea y estado; el operador ve solo sus muestras.
- `MuestraViewModel` ya tiene `aprobar()` y `solicitarCorreccion()` (lógica del supervisor), pero todavía sin pantalla.
- Datos ficticios en memoria (`DatosFicticios` y `MuestraRepository`).
- Diseños en `docs/diseno/` (logo, 4 wireframes, flujo UML) y evidencia de clase 2 en `docs/evidencias/clase-02/`.

Nota: todo lo anterior hay que confirmarlo corriendo la app en el emulador antes de darlo por cerrado (tarea 0.1).

## Qué falta

Persistencia (Room), fotografías, búsqueda por texto, pantallas del supervisor, exportación simulada y la documentación final. Hoy los datos viven solo en memoria y se pierden al cerrar la app.

## Tareas pendientes por bloque

### 0. Verificación general

| # | Tarea | Propuesto |
|---|---|---|
| 0.1 | Probar la app de `main` en emulador: flujo completo con `operador1` y con `supervisor1`; rotar la pantalla a mitad de un registro (RNF Confiabilidad) | Todos |
| 0.2 | Sacar capturas de las pantallas a `docs/capturas/` y actualizar el README (hoy dice "Pendiente") | Vicente |
| 0.3 | Mover dependencias sueltas de `app/build.gradle.kts` a `libs.versions.toml` | Javier |

### 1. Base de datos Room (prioridad 1, bloquea los bloques 3, 4 y 5)

| # | Tarea | Requerimiento | Propuesto |
|---|---|---|---|
| 1.1 | Agregar dependencias de Room y KSP; hacer Sync y verificar compatibilidad con AGP 9.4.1 y Kotlin 2.2.10 | RF07 | Cristobal |
| 1.2 | `MuestraEntity` (incluye `fotoUri` desde el inicio) y `UsuarioEntity`, con funciones que las convierten al modelo actual | RF01, RF02, RF03 | Cristobal |
| 1.3 | DAO con insertar, actualizar, listar y consultar por id | RF07 | Cristobal |
| 1.4 | `AppDatabase` y conectar `MuestraRepository` a Room **sin cambiar su interfaz**, para no romper las pantallas | RF07, MVVM | Cristobal |
| 1.5 | Poblar la base con los datos ficticios existentes la primera vez que se abre la app | Privacidad | Vicente |

### 2. Filtros y búsqueda (prioridad 2)

| # | Tarea | Requerimiento | Propuesto |
|---|---|---|---|
| 2.1 | Consultas en el DAO por centro, línea, fecha, operador y estado | RF08 | Cristobal |
| 2.2 | Agregar filtro por fecha al Historial (hoy filtra por centro, línea y estado) | RF08 | Daniel |
| 2.3 | Barra de búsqueda de texto en el Historial | RF08 | Daniel |
| 2.4 | Estado vacío cuando no hay resultados | Usabilidad | Daniel |

### 3. Fotografías y galería (prioridad 3)

| # | Tarea | Requerimiento | Propuesto |
|---|---|---|---|
| 3.1 | Permiso de cámara y `FileProvider` en el manifest | RF04 | Javier |
| 3.2 | Tomar foto con la cámara y elegir imagen desde la galería | RF04 | Javier |
| 3.3 | Guardar la URI de la foto en la muestra (campo `fotoUri`) | RF04, Trazabilidad | Cristobal |
| 3.4 | Mostrar la foto en el formulario, en el resumen y en el detalle del supervisor | RF04 | Daniel |
| 3.5 | No permitir enviar la muestra sin foto | RF05 | Vicente |

Importante: usar solo imágenes de prueba, sin personas ni datos identificables.

### 4. Revisión del supervisor

La lógica base ya existe en el ViewModel. Faltan las pantallas y la ruta en la navegación.

| # | Tarea | Requerimiento | Propuesto |
|---|---|---|---|
| 4.1 | Home Supervisor: bandeja de muestras pendientes (wireframe 3) y su ruta en `Rutas` | RF09 | Daniel |
| 4.2 | Detalle de validación con datos y fotografía (wireframe 4) | RF09 | Daniel |
| 4.3 | Conectar los botones Aprobar y Solicitar corrección a `aprobar()` y `solicitarCorreccion()`; guardar el cambio en Room | RF09 | Cristobal |
| 4.4 | El operador puede corregir una muestra observada y reenviarla (estado Corregido) | RF09 | Cristobal |
| 4.5 | Limitar acciones según el rol (el supervisor no crea muestras, el operador no valida) | RNF Seguridad | Javier |

### 5. Cierre del MVP

| # | Tarea | Requerimiento | Propuesto |
|---|---|---|---|
| 5.1 | Exportación simulada a planilla (CSV o mensaje de prueba) | RF10 | Cristobal |
| 5.2 | Probar escenarios: muestra completa, campos faltantes, foto pendiente, observada, validada | Caso, punto 4.4 | Vicente |
| 5.3 | Revisar la evidencia de clase 2 del repo: debe ser la versión de Operador/Supervisor (la anterior tenía contenido de otro caso: centros de cultivo, producción, encargado) | | Javier |
| 5.4 | Preparar la demostración: qué hace la app, qué problema resuelve y qué queda fuera del MVP | | Todos |

Opcional si sobra tiempo: perfil Analista de reportes, gráficos simples.

## Reglas del equipo

- Una rama por tarea (`feature/room-database`, `feature/fotos`, etc.), PR hacia `main` y revisión de otra persona. Nunca `push --force`.
- Commits chicos y con mensaje claro, por ejemplo `feat(db): agrega entidad Muestra y DAO`.
- Antes de subir, verificar que el proyecto compila.
- Antes de empezar una rama nueva: `git checkout main` y `git pull origin main`.
- Todos los datos y fotos son ficticios. No subir información real de ALDEMAR ni de personas.
- No afirmar que la app cuenta automáticamente: el conteo es manual.

## Dependencias entre tareas

- Room (bloque 1) antes de las consultas (2.1), de guardar la foto (3.3) y de persistir el estado del supervisor (4.3).
- Formulario (ya hecho) antes de mostrar la foto (3.4).
- Bandeja del supervisor (4.1) antes del detalle (4.2).

## Calendario sugerido

- **Esta semana (hasta el 18):** mergear lo pendiente, probar la app (0.1), capturas y README (0.2), 5.3, y que Cristobal deje listas las dependencias de Room (1.1).
- **Semana del 19 al 23 (última de desarrollo):** Room, filtros y fotos en paralelo, supervisor y CSV. Todo mergeado a `main` el viernes 23.
- **Del 24 al 26:** pruebas de escenarios, corrección de errores y ensayo de la demostración.
- **27 y 28:** Evaluación 2.
