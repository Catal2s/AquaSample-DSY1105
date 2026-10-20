# Tareas AquaSample (DSY1105, La Brigada Fantasma 01)

Actualizado el 20-10-2026, después del merge del PR #4 en `main`. Alineado con la rúbrica de la Evaluación Parcial 2 (avance funcional, contenidos de las clases 1 a 11).

## Fechas clave

- Esta semana (lunes 19 al viernes 23) es la **última para avanzar en la app**. Salieron en AVA las clases en video: Filtros y Búsqueda, Room (SQLite), Fotografías y Galería.
- Del 24 al 26: solo corregir errores, README, capturas y ensayo.
- Pizarra: Evaluación 2 el martes 27 y miércoles 28 de octubre.
- **Por confirmar en AVA (responsable: Javier):** la fecha y hora exactas de entrega del código. La rúbrica dice que se entrega por AVA antes de la primera sesión de la Evaluación 3 y que **después de entregar no se puede modificar el repositorio**.

## Qué exige la Evaluación 2 (y cuánto pesa)

| Indicador | Peso | Estado hoy |
|---|---|---|
| GitHub, README, commits de cada integrante y planificación en **Trello** | 20% | Falta Trello; README con "Pendiente" |
| Interfaz y navegación (Material 3) | 15% | Hecho, falta probar y sumar pantallas del supervisor |
| Formularios con validación y mensajes | 15% | Hecho |
| MVVM + **Room** (los datos deben persistir al reiniciar la app) | 15% | **Room sin empezar** |
| **Dos recursos del dispositivo** funcionando (cámara, galería, fecha/hora, estado de conexión) | 15% | **Sin empezar** |
| StateFlow y ViewModel | 10% | Hecho |
| Listas, búsqueda, filtros y detalle | 10% | Filtros hechos; falta búsqueda de texto y detalle |

Importante: la rúbrica pide que todo esté integrado en el caso de uso real (muestras de choritos), no pantallas sueltas. Además, la **Evaluación 3 es una defensa individual** sin apoyo de IA: cada uno tendrá que mostrar Room o el recurso que implementó, explicar el código y hacer un cambio pequeño en vivo. Quien escribe una parte, tiene que poder explicarla.

## Qué ya está hecho en `main`

- Proyecto Android con Kotlin, Jetpack Compose y Material 3, con la paleta AquaSample.
- Estructura MVVM: `model`, `data`, `viewmodel`, `ui/screens`, `ui/components`, `ui/navigation`.
- Navegación: Login → Inicio → Nueva muestra → Conteo → Resumen, más Historial.
- Login con roles (Operador y Supervisor); usuarios ficticios `operador1`, `operador2`, `supervisor1` (clave `1234`).
- Formulario de muestra con validación y mensajes de error; conteo manual con calibre opcional.
- Historial con filtros por centro, línea y estado; el operador ve solo sus muestras.
- `MuestraViewModel` con `aprobar()` y `solicitarCorreccion()` (lógica del supervisor, aún sin pantalla).
- Datos ficticios **en memoria**: se pierden al cerrar la app.
- Diseños en `docs/diseno/` y evidencia de clase 2 en `docs/evidencias/clase-02/`.

Todo esto hay que confirmarlo corriendo la app en el emulador (tarea 0.1).

## Tareas pendientes por bloque

### 0. Planificación y documentación (20% de la nota de equipo)

| # | Tarea | Responsable |
|---|---|---|
| 0.1 | Crear el tablero de **Trello** (Por hacer / En curso / Revisión / Hecho), con una tarjeta por tarea de este archivo y su responsable; invitar a los 4 y al profesor si lo piden | Javier |
| 0.2 | Mantener Trello al día: mover la tarjeta al empezar y al terminar | Todos |
| 0.3 | Probar la app de `main` en emulador: flujo completo con `operador1` y con `supervisor1`; rotar la pantalla a mitad de un registro | Todos |
| 0.4 | Sacar capturas a `docs/capturas/` y completar el README: descripción, integrantes, funcionalidades, instrucciones de ejecución, enlace al Trello | Vicente |
| 0.5 | Revisar que cada integrante tenga commits propios en el historial (con su usuario de GitHub) | Todos |
| 0.6 | Confirmar en AVA la fecha límite y quién sube el código | Javier |
| 0.7 | Mover dependencias sueltas de `app/build.gradle.kts` a `libs.versions.toml` | Javier |

### 1. Base de datos Room (prioridad 1, bloquea los bloques 2, 3 y 4)

| # | Tarea | Responsable |
|---|---|---|
| 1.1 | Agregar dependencias de Room y KSP y hacer Sync. El proyecto usa AGP 9.4.1 con Kotlin integrado (2.2.10): hay que verificar qué versión de KSP corresponde. Si el Sync falla, resolverlo antes de seguir | Cristobal |
| 1.2 | `MuestraEntity` (con `fotoUri`) y `UsuarioEntity`, con funciones que las convierten al modelo actual | Cristobal |
| 1.3 | DAO: insertar, actualizar, listar y consultar por id | Cristobal |
| 1.4 | `AppDatabase` y conectar `MuestraRepository` a Room **sin cambiar su interfaz**, para no romper las pantallas | Cristobal |
| 1.5 | Poblar la base con los datos ficticios existentes la primera vez que se abre la app | Vicente |
| 1.6 | Probar que una muestra creada sigue ahí al cerrar y volver a abrir la app | Vicente |

### 2. Filtros, búsqueda y detalle (prioridad 2)

| # | Tarea | Responsable |
|---|---|---|
| 2.1 | Consultas en el DAO por centro, línea, fecha, operador y estado | Cristobal |
| 2.2 | Filtro por fecha en el Historial | Daniel |
| 2.3 | Barra de búsqueda de texto en el Historial | Daniel |
| 2.4 | Estado vacío cuando no hay resultados | Daniel |
| 2.5 | Pantalla de detalle de una muestra al tocarla en el Historial | Daniel |

### 3. Recursos del dispositivo (prioridad 3, se necesitan **dos**)

Plan principal: **cámara y galería**. Plan B si las fotos se complican: **fecha/hora actual** en el formulario (selector de fecha y hora que parte con el momento actual) y/o **estado de conexión a Internet**.

| # | Tarea | Responsable |
|---|---|---|
| 3.1 | Permiso de cámara y `FileProvider` en el manifest | Javier |
| 3.2 | Tomar foto con la cámara y elegir imagen desde la galería | Javier |
| 3.3 | Guardar la URI de la foto en la muestra (campo `fotoUri` en Room) | Cristobal |
| 3.4 | Mostrar la foto en el formulario, el resumen y el detalle | Daniel |
| 3.5 | No permitir enviar la muestra sin foto | Vicente |
| 3.6 | Plan B: selector de fecha/hora o aviso de conexión, listo si las fotos no alcanzan | Vicente |

Importante: usar solo imágenes de prueba, sin personas ni datos identificables.

### 4. Revisión del supervisor

La lógica ya existe en el ViewModel. Faltan las pantallas y su ruta en `Rutas`.

| # | Tarea | Responsable |
|---|---|---|
| 4.1 | Home Supervisor: bandeja de muestras pendientes (wireframe 3) | Daniel |
| 4.2 | Detalle de validación con datos y foto (wireframe 4) | Daniel |
| 4.3 | Conectar Aprobar y Solicitar corrección a `aprobar()` y `solicitarCorreccion()`, guardando el cambio en Room | Cristobal |
| 4.4 | El operador corrige una muestra observada y la reenvía (estado Corregido) | Cristobal |
| 4.5 | Limitar acciones según el rol | Javier |

### 5. Cierre

| # | Tarea | Responsable |
|---|---|---|
| 5.1 | Exportación simulada a planilla (CSV o mensaje de prueba) | Cristobal |
| 5.2 | Probar escenarios: muestra completa, campos faltantes, foto pendiente, observada, validada | Vicente |
| 5.3 | Revisar que la evidencia de clase 2 del repo sea la versión Operador/Supervisor | Javier |
| 5.4 | Preparar la defensa: cada uno ensaya explicar y modificar en vivo la parte que escribió | Todos |

Opcional si sobra tiempo: perfil Analista de reportes, gráficos simples.

## Reglas del equipo

- Una rama por tarea (`feature/room-database`, `feature/fotos`, etc.), PR hacia `main` y revisión de otra persona. Nunca `push --force`.
- Commits chicos y con mensaje claro, por ejemplo `feat(db): agrega entidad Muestra y DAO`.
- Antes de subir, verificar que el proyecto compila y corre en el emulador.
- Antes de empezar una rama nueva: `git checkout main` y `git pull origin main`.
- Todos los datos y fotos son ficticios. No subir información real de ALDEMAR ni de personas.
- No afirmar que la app cuenta automáticamente: el conteo es manual.
- **No tocar el repositorio después de entregar en AVA**, salvo indicación del docente.

## Dependencias entre tareas

- Room (bloque 1) antes de las consultas (2.1), de guardar la foto (3.3) y de persistir el estado del supervisor (4.3).
- Cámara y galería (3.2) antes de mostrar la foto (3.4) y de exigirla (3.5).
- Bandeja del supervisor (4.1) antes del detalle (4.2).

## Calendario

- **Martes 20 y miércoles 21:** Trello (0.1), prueba de la app (0.3), Sync de Room (1.1), permisos de cámara (3.1). Primeros PR con Room y búsqueda.
- **Jueves 22 y viernes 23:** Room conectado, filtros, fotos y pantallas del supervisor mergeados a `main`. Cierre de desarrollo el viernes.
- **Sábado 24 al lunes 26:** pruebas de escenarios, README con capturas, corrección de errores, ensayo de la defensa y entrega en AVA según la fecha confirmada.
- **27 y 28:** Evaluación.

