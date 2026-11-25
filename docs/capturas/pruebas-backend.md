# Pruebas: conexión de la app con el backend

Evidencia de que la app sincroniza las muestras con el backend propio (Spring Boot + H2) a través de Retrofit, y de que sigue funcionando sin conexión.

- **Responsable:** Vicente Barrera (QA / validaciones)
- **Fecha:** 24-11-2026
- **Rama:** `feature/evidencia-backend`
- **Dispositivo:** teléfono Android físico (Android 16, API 36) conectado por USB
- **Backend:** `backend/` ejecutado en el PC con `SERVER_PORT=8081`; el teléfono llega a él con `adb reverse tcp:8081 tcp:8081` (`backend.url=http://localhost:8081/` en `local.properties`)
- **Usuario:** `operador1` (Operador Demo 01, datos ficticios)

## Resultados

| # | Prueba | Resultado esperado | Resultado |
|---|---|---|---|
| 1 | Registrar una muestra con foto (Centro Ficticio Norte, Tren 1, Línea 1, tramo 1,4 m, 11 individuos) | El resumen muestra los datos y avisa que la muestra queda pendiente de sincronización | ✅ Pasó |
| 2 | Sincronizar con el backend encendido | La app informa que todo está sincronizado y `GET /api/muestras/6` devuelve la muestra con sus datos y la foto (`image/jpeg`) | ✅ Pasó |
| 3 | Apagar el backend y tocar **Sincronizar** | La app no falla: muestra "Sin conexión con el servidor. Los cambios quedan guardados en el teléfono." | ✅ Pasó |
| 4 | Registrar otra muestra con el backend apagado (Tren 2, Línea 1, 7 individuos) | La Muestra #7 aparece en el Historial como "Pendiente de sincronización"; la #6 sigue "Sincronizada" | ✅ Pasó |
| 5 | Encender el backend y tocar **Sincronizar** | La app informa "Enviadas: 1 · Recibidas del servidor: 0" | ✅ Pasó |
| 6 | Revisar el Historial y el backend | La Muestra #7 queda "Sincronizada" y `GET /api/muestras/7` la devuelve con su foto; el backend pasa de 6 a 7 muestras | ✅ Pasó |

## Errores encontrados

Ninguno.

## Evidencia

Capturas del teléfono:

- [Resumen de la muestra antes de sincronizar](backend-1-resumen.png)
- [Sincronización con el backend encendido](backend-2-sincronizar.png)
- [Sincronización con el backend apagado](backend-3-sin-conexion.png)
- [Muestra registrada sin conexión, pendiente de sincronización](backend-4-pendiente.png)
- [Sincronización al volver a encender el backend](backend-5-reconexion.png)
- [Historial con todas las muestras sincronizadas](backend-6-sincronizada.png)

Respuestas del backend (`GET /api/muestras/{id}`):

- [Muestra 6](backend-2-api-muestra-6.json)
- [Muestra 7](backend-6-api-muestra-7.json)
