# Pruebas: escenarios de registro y revisión (tarea 5.2)

Pruebas y capturas solicitadas por la pauta de la Evaluación Parcial 2 (DSY1105) como evidencia de:

- **IE 2.1.2, formularios y validación visual:** campos obligatorios y mensajes por cada error, incluida la foto obligatoria y el comentario obligatorio al solicitar corrección.
- **IE 2.2.2, listas, búsqueda, filtros y detalle:** filtro del Historial por estado de revisión y detalle de la muestra.
- **IE 2.4.1, integración de recursos del dispositivo:** foto de la muestra tomada con la cámara.

- **Responsable:** Vicente Barrera (QA / validaciones)
- **Fechas:** 26-10-2026 (B y D1–D2) y 27-10-2026 (C y D3–D7)
- **Rama:** `feature/fecha-hora` (C y D3–D7 con la versión de `main` que incluye fotos y revisión del supervisor)
- **Dispositivo:** emulador Android, Medium Phone API 37
- **Usuarios:** `operador1` y `supervisor1` (datos ficticios)

## Resumen por escenario

| Escenario | Estado |
|---|---|
| A. Muestra completa | ✅ Pasó (probado en [pruebas-fecha-hora.md](pruebas-fecha-hora.md), prueba 10, y en [pruebas-persistencia.md](pruebas-persistencia.md)) |
| B. Campos faltantes o inválidos | ✅ Pasó (4 de 4 pruebas) |
| C. Foto pendiente | ✅ Pasó (2 de 2 pruebas) |
| D. Muestra observada, corregida y validada | ✅ Pasó (7 de 7 pruebas), con el ciclo completo Pendiente → Observado → Corregido → Validado |

## Resultados

| # | Prueba | Resultado esperado | Resultado |
|---|---|---|---|
| B1 | Nueva muestra sin centro, tren, línea ni tramo y tocar Siguiente | No avanza; errores "Selecciona un centro", "Selecciona un tren", "Selecciona una línea" e "Ingresa un largo de tramo mayor a 0" | ✅ Pasó |
| B2 | Completar centro, tren y línea con tramo `0` y tocar Siguiente | Solo queda el error del tramo; no avanza | ✅ Pasó |
| B3 | Tramo `0.2`, avanzar a Conteo y tocar Ver resumen con 0 individuos | No avanza; error "El conteo debe ser mayor a 0" | ✅ Pasó |
| B4 | Conteo `250` y calibre `0`, tocar Ver resumen | Desaparece el error del conteo; error "El calibre debe ser un número mayor a 0"; no avanza | ✅ Pasó |
| C1 | En Conteo, con 250 individuos y sin foto, tocar Ver resumen | No avanza; error "Adjunta o toma una foto de la muestra" | ✅ Pasó |
| C2 | Tomar una foto con la cámara y tocar Ver resumen | Desaparece el error; avanza al Resumen, que muestra la foto | ✅ Pasó |
| D1 | Historial, filtro de estado "Observado" | Solo la Muestra #2, con etiqueta Observado y la observación del supervisor | ✅ Pasó |
| D2 | Historial, filtro de estado "Validado" | Solo la Muestra #1, con etiqueta Validado | ✅ Pasó |
| D3 | Enviar la muestra a revisión e ingresar como `supervisor1` | La Muestra #7 aparece en la Bandeja de revisión como Pendiente | ✅ Pasó |
| D4 | En el detalle, tocar Solicitar corrección con el comentario vacío | No cambia el estado; error "Escribe qué hay que corregir para solicitar la corrección" | ✅ Pasó |
| D5 | Escribir un comentario y tocar Solicitar corrección | La Muestra #7 pasa a Observado y muestra el comentario en el Historial | ✅ Pasó |
| D6 | Ingresar como `operador1`, abrir la Muestra #7 y tocar Marcar como corregida | La Muestra #7 pasa a Corregido | ✅ Pasó |
| D7 | Ingresar como `supervisor1`, abrir la Muestra #7 desde la Bandeja y tocar Aprobar muestra | La Muestra #7 pasa a Validado | ✅ Pasó |

## Errores encontrados

Se continúa la numeración de [pruebas-fecha-hora.md](pruebas-fecha-hora.md) (E1 y E2). El error E2 ("Calibre promedio" pegado a su valor) también se observa en el Detalle de muestra.

| ID | Pantalla | Descripción | Severidad | Estado |
|---|---|---|---|---|
| E3 | Conteo | El tramo se muestra tal como se escribió ("0.2 m" o "0,2 m" según se use punto o coma), en vez de con un formato único. | Baja (visual) | Abierto |
| E4 | Historial | Con un solo resultado, el contador dice "1 muestras" en vez de "1 muestra". | Baja (texto) | Abierto |

## Capturas

- [B1: formulario vacío](escenario-campos-vacios.png)
- [B2: tramo igual a 0](escenario-tramo-cero.png)
- [B3: conteo en 0](escenario-conteo-cero.png)
- [B4: calibre igual a 0](escenario-calibre-cero.png)
- [C1: sin foto](escenario-sin-foto.png)
- [C2: resumen con foto](escenario-con-foto.png)
- [D1: filtro Observado](escenario-observada.png)
- [D2: filtro Validado](escenario-validada.png)
- [D3: bandeja del supervisor](escenario-bandeja-supervisor.png)
- [D4: corrección sin comentario](escenario-correccion-sin-comentario.png)
- [D5: corrección solicitada](escenario-correccion-solicitada.png)
- [D6: muestra corregida](escenario-corregida.png)
- [D7: muestra aprobada](escenario-aprobada.png)
