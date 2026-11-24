package com.aquasample.backend.controller;

import com.aquasample.backend.dto.MuestraResponse;
import com.aquasample.backend.dto.ResumenResponse;
import com.aquasample.backend.service.MuestraService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Indicadores y datos para reportes. Los puede consultar cualquier rol. */
@RestController
@RequestMapping("/api/reportes")
@Tag(name = "Reportes", description = "Indicadores del dashboard y registros para exportar")
public class ReporteController {

    private final MuestraService servicio;

    public ReporteController(MuestraService servicio) {
        this.servicio = servicio;
    }

    @GetMapping("/resumen")
    @Operation(summary = "Indicadores: total de muestras, individuos y conteo por estado y por centro")
    public ResumenResponse resumen() {
        return servicio.resumen();
    }

    @GetMapping("/validadas")
    @Operation(summary = "Muestras validadas, para exportar a Excel o PDF")
    public List<MuestraResponse> validadas() {
        return servicio.validadas().stream().map(MuestraResponse::desde).toList();
    }
}
