# Pruebas: escenarios de registro y revisión (tarea 5.2)

Pruebas y capturas solicitadas por la pauta de la Evaluación Parcial 2 (DSY1105) como evidencia de:

- **IE 2.1.2, formularios y validación visual:** campos obligatorios y mensajes por cada error.
- **IE 2.2.2, listas, búsqueda, filtros y detalle:** filtro del Historial por estado de revisión.

- **Responsable:** Vicente Barrera (QA / validaciones)
- **Fecha:** 26-10-2026
- **Rama:** `feature/fecha-hora`
- **Dispositivo:** emulador Android, Medium Phone API 37
- **Usuario:** `operador1` (datos ficticios)

## Resumen por escenario

| Escenario | Estado |
|---|---|
| A. Muestra completa | ✅ Pasó (probado en [pruebas-fecha-hora.md](pruebas-fecha-hora.md), prueba 10, y en [pruebas-persistencia.md](pruebas-persistencia.md)) |
| B. Campos faltantes o inválidos | ✅ Pasó (4 de 4 pruebas) |
| C. Foto pendiente | ⛔ Bloqueado: la cámara y la galería aún no están integradas (tareas 3.1 y 3.2) |
| D. Muestra observada y validada | ⚠️ Parcial: se verificó cómo se muestran y filtran los estados en el Historial. El flujo del supervisor (aprobar o pedir corrección) no tiene pantalla todavía (tareas 4.1 a 4.3) |

## Resultados

| # | Prueba | Resultado esperado | Resultado |
|---|---|---|---|
| B1 | Nueva muestra sin centro, tren, línea ni tramo y tocar Siguiente | No avanza; errores "Selecciona un centro", "Selecciona un tren", "Selecciona una línea" e "Ingresa un largo de tramo mayor a 0" | ✅ Pasó |
| B2 | Completar centro, tren y línea con tramo `0` y tocar Siguiente | Solo queda el error del tramo; no avanza | ✅ Pasó |
| B3 | Tramo `0.2`, avanzar a Conteo y tocar Ver resumen con 0 individuos | No avanza; error "El conteo debe ser mayor a 0" | ✅ Pasó |
| B4 | Conteo `250` y calibre `0`, tocar Ver resumen | Desaparece el error del conteo; error "El calibre debe ser un número mayor a 0"; no avanza | ✅ Pasó |
| D1 | Historial, filtro de estado "Observado" | Solo la Muestra #2, con etiqueta Observado y la observación del supervisor | ✅ Pasó |
| D2 | Historial, filtro de estado "Validado" | Solo la Muestra #1, con etiqueta Validado | ✅ Pasó |

## Errores encontrados

Se continúa la numeración de [pruebas-fecha-hora.md](pruebas-fecha-hora.md) (E1 y E2).

| ID | Pantalla | Descripción | Severidad | Estado |
|---|---|---|---|---|
| E3 | Conteo | El tramo se muestra tal como se escribió ("0.2 m" o "0,2 m" según se use punto o coma), en vez de con un formato único. | Baja (visual) | Abierto |
| E4 | Historial | Con un solo resultado, el contador dice "1 muestras" en vez de "1 muestra". | Baja (texto) | Abierto |

## Capturas

- [B1: formulario vacío](escenario-campos-vacios.png)
- [B2: tramo igual a 0](escenario-tramo-cero.png)
- [B3: conteo en 0](escenario-conteo-cero.png)
- [B4: calibre igual a 0](escenario-calibre-cero.png)
- [D1: filtro Observado](escenario-observada.png)
- [D2: filtro Validado](escenario-validada.png)
