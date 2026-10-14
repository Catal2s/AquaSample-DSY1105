# Tareas pendientes AquaSample (DSY1105, La Brigada Fantasma 01)

Estado al 14-10-2026. Basado en los requerimientos del caso (RF01 a RF10) y en lo que hoy existe en el repo.

## Qué ya está hecho

- Proyecto Android con Kotlin, Jetpack Compose y Material 3
- Tema con la paleta AquaSample (`ui/theme`)
- Diseños: logo, 4 wireframes y flujo UML en `docs/diseno/`
- Evidencias de clase 1 y 2

## Qué falta (resumen)

La app hoy solo muestra un "Hello Android". Falta toda la funcionalidad: estructura MVVM, navegación, login con roles, formulario de muestra, base de datos, fotos, historial, filtros y revisión del supervisor.

## Prioridad del equipo (pizarra)

1. Base de datos Room (SQLite)
2. Filtros y búsquedas
3. Fotografías y galería de imágenes

Orden de trabajo: primero Room, porque filtros y fotos dependen de ella.

## Tareas por bloque

### 0. Base del proyecto

| # | Tarea | Requerimiento | Propuesto |
|---|---|---|---|
| 0.1 | Verificar que el proyecto compila y corre en emulador | RNF Compatibilidad | Todos |
| 0.2 | Crear estructura de paquetes MVVM: `data`, `domain`/`model`, `ui`, `viewmodel` | RNF Mantenibilidad | Javier |
| 0.3 | Mover dependencias sueltas de `app/build.gradle.kts` a `libs.versions.toml` | | Javier |
| 0.4 | Navegación entre pantallas con `navigation-compose` | | Daniel |

### 1. Base de datos Room (prioridad 1)

| # | Tarea | Requerimiento | Propuesto |
|---|---|---|---|
| 1.1 | Agregar dependencias de Room y KSP (verificar compatibilidad con AGP 9) | RF07 | Cristobal |
| 1.2 | Entidad `Muestra`: centro, tren, línea, fecha, hora, tramo, conteo, observaciones, estado, operador, `fotoUri` | RF02, RF03 | Cristobal |
| 1.3 | Entidad `Usuario` con rol (Operador, Supervisor) | RF01 | Cristobal |
| 1.4 | DAO con insertar, actualizar, listar y consultar por id | RF07 | Cristobal |
| 1.5 | `AppDatabase` y `MuestraRepository` | RF07, MVVM | Cristobal |
| 1.6 | Datos de prueba ficticios (usuarios, centros, trenes, líneas, muestras) | Privacidad | Vicente |

### 2. Pantallas y flujo

| # | Tarea | Requerimiento | Propuesto |
|---|---|---|---|
| 2.1 | Pantalla de Login con usuarios ficticios y carga del rol | RF01 | Daniel |
| 2.2 | Home Operador: formulario "Nueva muestra" según wireframe 2 | RF02, RF03 | Daniel |
| 2.3 | Validación de campos obligatorios con mensajes de error | RF05 | Vicente |
| 2.4 | `MuestraViewModel` con estado del formulario y envío a revisión | RF02, RF05 | Cristobal |
| 2.5 | Que el registro en curso no se pierda al rotar o cambiar de pantalla | RNF Confiabilidad | Cristobal |
| 2.6 | Pantalla "Mis envíos" del operador | RF07 | Daniel |
| 2.7 | Limitar acciones según el rol | RNF Seguridad | Javier |

### 3. Filtros y búsquedas (prioridad 2)

| # | Tarea | Requerimiento | Propuesto |
|---|---|---|---|
| 3.1 | Consultas en el DAO: por centro, línea, fecha, operador y estado | RF08 | Cristobal |
| 3.2 | Barra de búsqueda en el historial | RF08 | Daniel |
| 3.3 | Filtros (chips o menú) por centro, línea, fecha y estado | RF08 | Daniel |
| 3.4 | Estado vacío cuando no hay resultados | Usabilidad | Daniel |

### 4. Fotografías y galería (prioridad 3)

| # | Tarea | Requerimiento | Propuesto |
|---|---|---|---|
| 4.1 | Permiso de cámara y `FileProvider` en el manifest | RF04 | Javier |
| 4.2 | Tomar foto con la cámara | RF04 | Javier |
| 4.3 | Elegir imagen desde la galería | RF04 | Javier |
| 4.4 | Guardar la URI de la foto en la muestra | RF04, Trazabilidad | Cristobal |
| 4.5 | Mostrar la foto en el formulario y en el detalle del supervisor | RF04 | Daniel |
| 4.6 | Validar que la muestra no se envíe sin foto | RF05 | Vicente |

Importante: usar solo imágenes de prueba, sin personas ni datos identificables.

### 5. Revisión del supervisor

| # | Tarea | Requerimiento | Propuesto |
|---|---|---|---|
| 5.1 | Home Supervisor: bandeja de muestras pendientes (wireframe 3) | RF09 | Daniel |
| 5.2 | Detalle de validación con datos y fotografía (wireframe 4) | RF09 | Daniel |
| 5.3 | Cambiar estado: pendiente, observado, corregido, validado | RF09 | Cristobal |
| 5.4 | Campo de observación del supervisor | RF09 | Cristobal |

### 6. Cierre del MVP

| # | Tarea | Requerimiento | Propuesto |
|---|---|---|---|
| 6.1 | Pantalla de resumen final de la muestra | RF10 | Daniel |
| 6.2 | Exportación simulada a planilla (CSV o mensaje de prueba) | RF10 | Cristobal |
| 6.3 | Probar escenarios: muestra completa, campos faltantes, foto pendiente, observada, validada | Caso, punto 4.4 | Vicente |
| 6.4 | Corregir la evidencia de clase 2 (tiene contenido de otro caso: centros de cultivo, id, nombre, ubicación, producción, encargado) | | Javier |
| 6.5 | Preparar la demostración: qué hace la app, qué problema resuelve y qué queda fuera del MVP | | Todos |

Opcional si sobra tiempo: perfil Analista de reportes, gráficos simples, calibre y peso (RF06).

## Reglas del equipo

- Commits chicos y con mensaje claro, por ejemplo `feat(db): agrega entidad Muestra y DAO`
- Una tarea por commit cuando se pueda
- Todos los datos y fotos son ficticios. No subir información real de ALDEMAR ni de personas.
- No afirmar que la app cuenta automáticamente: el conteo es manual
- Antes de subir, verificar que el proyecto compila

## Dependencias entre tareas

- Bloque 1 (Room) antes de los bloques 3, 4.4 y 5
- 0.2 (estructura MVVM) antes de todo lo demás
- 2.2 (formulario) antes de 4.5 (mostrar foto)
