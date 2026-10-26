# Pruebas: selector de fecha y hora (tarea 3.6)

Pruebas y capturas solicitadas por la pauta de la Evaluación Parcial 2 (DSY1105) como evidencia de:

- **IE 2.4.1, integración de recursos del dispositivo:** uso de la fecha y hora del teléfono en el registro de la muestra.
- **IE 2.1.2, formularios y validación visual:** reglas y mensajes de error de fecha y hora futuras.

- **Responsable:** Vicente Barrera (QA / validaciones)
- **Fecha:** 25-10-2026
- **Rama:** `feature/fecha-hora`
- **Dispositivo:** emulador Android, Medium Phone API 37 (zona horaria GMT)
- **Usuario:** `operador1` (datos ficticios)

## Resultados

| # | Prueba | Resultado esperado | Resultado |
|---|---|---|---|
| 1 | Abrir Nueva muestra | Fecha y Hora parten con la fecha y hora actuales del teléfono | ✅ Pasó |
| 2 | Tocar el campo Fecha | Se abre el calendario con el día del formulario seleccionado y hoy marcado | ✅ Pasó |
| 3 | Intentar elegir un día posterior a hoy | Los días futuros aparecen deshabilitados | ✅ Pasó |
| 4 | Elegir un día anterior y Aceptar | El campo muestra la fecha en formato `AAAA-MM-DD` | ✅ Pasó |
| 5 | Elegir otro día y Cancelar | La fecha no cambia | ✅ Pasó |
| 6 | Tocar el campo Hora | Se abre el reloj de 24 horas con la hora del formulario | ✅ Pasó |
| 7 | Fecha de hoy con hora futura y Siguiente | Error "La hora no puede ser posterior a la actual" bajo Hora | ✅ Pasó |
| 8 | Cambiar a una fecha pasada con la misma hora y Siguiente | Sin error, avanza a Conteo | ✅ Pasó |
| 9 | Girar la pantalla con el calendario abierto | El calendario sigue abierto y conserva el día elegido | ⚠️ Pasó con observación (ver E1) |
| 10 | Completar el registro y ver el Resumen | El Resumen muestra la fecha y la hora elegidas | ✅ Pasó |

## Errores encontrados

| ID | Pantalla | Descripción | Severidad | Estado |
|---|---|---|---|---|
| E1 | Nueva muestra (calendario) | Con el teléfono en horizontal, el calendario no cabe en la pantalla: los botones Cancelar y Aceptar quedan encima de la última fila de días. Se puede usar igual, pero se ve cortado. | Baja (visual) | Abierto |
| E2 | Resumen | La etiqueta "Calibre promedio" es más larga que su columna y queda pegada al valor ("No registrado"). | Baja (visual) | Abierto, informar al responsable de la pantalla |

## Capturas

- [Calendario con días futuros deshabilitados](fecha-calendario.png)
- [Reloj de 24 horas](fecha-reloj.png)
- [Error de hora futura](fecha-error-hora.png)
