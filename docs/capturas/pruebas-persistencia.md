# Pruebas: persistencia con Room (tarea 1.6)

Pruebas y capturas solicitadas por la pauta de la Evaluación Parcial 2 (DSY1105) como evidencia de:

- **IE 2.3.1, arquitectura MVVM y persistencia Room:** una muestra registrada en la app se conserva al cerrar y volver a abrir la aplicación.

- **Responsable:** Vicente Barrera (QA / validaciones)
- **Fecha:** 26-10-2026
- **Rama:** `feature/fecha-hora`
- **Dispositivo:** emulador Android, Medium Phone API 37
- **Usuario:** `operador1` (datos ficticios)

## Resultados

| # | Prueba | Resultado esperado | Resultado |
|---|---|---|---|
| 1 | Registrar una muestra (Centro Ficticio Norte, Tren 1, Línea 2, 2026-10-20 15:00, tramo 0,2 m, 250 individuos) y enviarla a revisión | La muestra aparece en el Historial como Muestra #6, estado Pendiente | ✅ Pasó |
| 2 | Cerrar la app por completo (forzar detención, sin proceso en memoria) y volver a abrirla | La app inicia desde cero en el Login | ✅ Pasó |
| 3 | Iniciar sesión con `operador1` y abrir el Historial | La Muestra #6 sigue en la lista con los mismos datos (4 muestras · 1147 individuos) | ✅ Pasó |

## Errores encontrados

Ninguno.

## Capturas

- [Historial antes de cerrar la app](persistencia-antes.png)
- [Historial después de volver a abrir la app](persistencia-despues.png)
