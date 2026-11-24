# TAREAS - Evaluación Parcial 4 (EA4) · AquaSample / ALDEMAR SpA

Equipo **La Brigada Fantasma 01** · DSY1105 Desarrollo de Aplicaciones Móviles
Asignación: semana 13 · **Entrega: semana 15** (antes de la primera sesión de Evaluación Parcial 5)

> Regla clave: si la app no corre en la defensa, los ítems siguientes quedan en **0**. `main` siempre debe compilar y ejecutarse.
> Nunca subir a GitHub: `.jks`, contraseñas, credenciales ni datos reales de ALDEMAR.

---

## Cómo se evalúa (rúbrica)

| Indicador | Peso | Responsable principal |
|---|---|---|
| 3.1.1 App con todas sus pantallas y flujos, sin errores de navegación | 15% | Daniel (+ Javier) |
| 3.1.2 Microservicios Spring Boot con BD y endpoints funcionales | 15% | Cristobal |
| 3.1.3 App integrada con los microservicios (CRUD completo) | 15% | Cristobal (+ Javier) |
| 3.1.4 API externa integrada al flujo visual | 15% | Cristobal |
| 3.2.1 Pruebas unitarias, cobertura > 90% | 15% | Vicente |
| 3.3.1 APK firmada (build.gradle y .jks) | 10% | Javier |
| 3.3.2 GitHub y Trello, commits de todos | 15% | Todos (coordina Vicente y Javier) |

---

## Reparto por persona (equilibrado, ~10-11 unidades c/u)

### Cristobal Gonzalez · Backend: backend y conexión

- [ ] **Definir el contrato de la API en los primeros 2 días** (endpoints + JSON) y compartirlo al equipo
- [ ] Proyecto Spring Boot con base de datos (muestras, fotos/metadatos, estados de revisión)
- [ ] Endpoints REST CRUD de muestras, fotos/metadatos y estados de revisión
- [ ] Permisos por rol (operador, supervisor, analista)
- [ ] Documentación Swagger/OpenAPI (springdoc) accesible
- [ ] Retrofit en la app: consumir el backend (crear, leer, actualizar, eliminar)
- [ ] Open-Meteo Marine con Retrofit: oleaje y temperatura del mar, mostrado en el flujo de la muestra
      (indicar fecha, ubicación aproximada y que es un dato estimado, no medición del centro)
- [ ] Pasar a Vicente `ExportadorCsv.kt` como base para el .xlsx/PDF

### Javier Cataldo · Team Lead: fotos, sincronización y entrega

- [ ] CameraX para capturar la foto (reemplaza TakePicture)
- [ ] Coil para cargar/cachear imágenes
- [ ] Telephoto para ampliar la foto (zoom)
- [ ] Fotos en almacenamiento privado, con la referencia guardada en Room
- [ ] WorkManager: enviar al backend lo pendiente al volver internet, sin duplicados (ID de muestra)
- [ ] Campo de estado de sincronización en Room (subir versión de la base de datos)
- [ ] APK release firmada y versionada (`.jks` fuera del repo) + configuración en `build.gradle.kts`
- [ ] Revisar PRs, resolver conflictos, mantener `main` estable
- [ ] Entrega final en AVA: código Android y backend, APK, README, evidencias, enlace GitHub

### Daniel Vargas · Frontend/UI: conteo y visualización

- [x] Conteo asistido: marcar individuos tocando la foto (Compose)
- [x] Deshacer marcas y ajustar el total manualmente; guardar junto con la corrección
- [x] Dashboard con gráficos: muestras por centro, estados pendiente/validado, evolución en el tiempo
- [x] Historial: comparar muestreos de una misma línea / entre períodos
- [x] Estados de vacío, carga, error y sin internet
- [x] Aviso visual "Pendiente de sincronización"
- [x] Animaciones discretas, iconos por sección, botones con buen contraste

### Vicente Barrera · QA: calidad, exportación y documentación

- [ ] Pruebas JUnit + MockK de ViewModel y Repository
- [ ] Al menos un Compose UI Test (formulario)
- [ ] Cobertura > 90% medida con JaCoCo o Kover + reporte de cobertura
- [ ] Exportación de registros validados a Excel (.xlsx) y/o PDF
      (ID, fecha, centro, tren, línea, longitud, conteo, observaciones, estado)
- [ ] Probar escenarios offline/online y las validaciones
- [ ] README final: instalación, diagramas, Swagger, API externa, capturas, pruebas, evidencias
- [ ] Capturas del flujo y evidencias de Swagger y de la API externa
- [ ] Trello al día (tarjetas asignadas y en la columna correcta)

---

## Orden de trabajo (2 semanas)

| Cuándo | Qué |
|---|---|
| Días 1-2 | Cristobal publica el contrato de la API. Todos trabajan con datos de ejemplo mientras tanto |
| Semana 1 | Backend + Retrofit (Cristobal) · CameraX y Coil (Javier) · Conteo y dashboard (Daniel) · Esqueleto de pruebas (Vicente) |
| Semana 2 | WorkManager, Telephoto, exportación, pruebas y cobertura, APK firmada, README |
| Últimos 2 días | Prueba completa en emulador/teléfono, sin cambios grandes. Ensayo de la defensa |

## Reglas de trabajo

- Una rama por tarea (`feature/...`), PR revisado por otro integrante antes de mergear. Sin force push.
- Commits de **todos** (es evidencia para el indicador 3.3.2).
- Cada tarea se mueve en Trello: Por hacer → En curso → Hecho.
- Datos y fotos **ficticios o autorizados** para fines académicos.
- Después de la fecha de entrega no se modifica el repositorio.

## Pendiente de confirmar

- [ ] La rúbrica indica entrega **en parejas**. Confirmar con el profesor si somos 4 en un solo equipo o dos parejas.
      Si son dos parejas, propuesta: Cristobal + Daniel (backend y el front que lo consume) y Javier + Vicente (fotos, sync, APK y pruebas).
