package com.aquasample.backend.controller;

import com.aquasample.backend.model.Muestra;
import com.aquasample.backend.service.FotoService;
import com.aquasample.backend.service.MuestraService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.time.LocalDateTime;

import static org.hamcrest.Matchers.matchesPattern;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Pruebas de la capa HTTP: validaciones y formato unico de error (servicios simulados). */
@WebMvcTest(MuestraController.class)
class MuestraControllerTest {

    private static final String JSON = """
            {"id":7,"centroId":"C1","trenId":"T1","lineaId":"L1","fecha":"2026-11-24","hora":"10:30",
             "tramoMetros":0.2,"operadorId":"U1","individuos":312,"calibrePromedioMm":38.5}""";

    @Autowired
    private MockMvc mvc;
    @MockitoBean
    private MuestraService servicio;
    @MockitoBean
    private FotoService fotoService;

    private void esError(ResultActions r, int codigo) throws Exception {
        r.andExpect(status().is(codigo))
                .andExpect(jsonPath("$.status").value(codigo))
                .andExpect(jsonPath("$.error").isNotEmpty())
                .andExpect(jsonPath("$.mensajes[0]").isNotEmpty())
                .andExpect(jsonPath("$.fecha").value(matchesPattern("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}")));
    }

    @ParameterizedTest
    @CsvSource({"38.5,-5", "38.5,0", "2026-11-24,2026/11/24", "0.2,0", "312,0"})
    void valorInvalido_da400(String original, String malo) throws Exception {
        esError(mvc.perform(put("/api/muestras/7").header("X-Rol", "OPERADOR")
                .contentType(MediaType.APPLICATION_JSON).content(JSON.replace(original, malo))), 400);
    }

    @Test
    void idNegativoEnElCuerpo_da400() throws Exception {
        esError(mvc.perform(post("/api/muestras").header("X-Rol", "OPERADOR")
                .contentType(MediaType.APPLICATION_JSON).content(JSON.replace("\"id\":7", "\"id\":-7"))), 400);
    }

    @ParameterizedTest
    @CsvSource({"C1", "T1", "L1", "U1"})
    void idDeCatalogoMuyLargo_da400(String valor) throws Exception {
        esError(mvc.perform(put("/api/muestras/7").header("X-Rol", "OPERADOR")
                .contentType(MediaType.APPLICATION_JSON).content(JSON.replace(valor, "x".repeat(51)))), 400);
    }

    @Test
    void erroresDeRutaMetodoYTipo_usanElMismoJson() throws Exception {
        esError(mvc.perform(get("/api/nada")), 404);
        esError(mvc.perform(delete("/api/muestras").header("X-Rol", "SUPERVISOR")), 405);
        esError(mvc.perform(post("/api/muestras").header("X-Rol", "OPERADOR")
                .contentType(MediaType.TEXT_PLAIN).content("hola")), 415);
        esError(mvc.perform(post("/api/muestras")
                .contentType(MediaType.APPLICATION_JSON).content(JSON)), 400);
    }

    @Test
    void fotoMuyGrandeOSinArchivo_usanElMismoJson() throws Exception {
        when(fotoService.guardar(anyInt(), any(), any())).thenThrow(new MaxUploadSizeExceededException(10));
        esError(mvc.perform(multipart("/api/muestras/1/foto").file("archivo", new byte[]{1})
                .header("X-Rol", "OPERADOR")), 413);
        esError(mvc.perform(multipart("/api/muestras/1/foto").header("X-Rol", "OPERADOR")), 400);
    }

    @Test
    void errorInesperado_da500SinDetalles() throws Exception {
        when(servicio.obtener(1)).thenThrow(new IllegalArgumentException("detalle interno"));
        ResultActions r = mvc.perform(get("/api/muestras/1"));
        esError(r, 500);
        r.andExpect(jsonPath("$.mensajes[0]").value("Error interno del servidor"));
    }

    @Test
    void fechasDeSalida_sinFracciones() throws Exception {
        Muestra m = new Muestra();
        m.setId(1);
        m.setCreadoEn(LocalDateTime.of(2026, 11, 24, 10, 31, 0, 123456789));
        when(servicio.obtener(1)).thenReturn(m);
        mvc.perform(get("/api/muestras/1"))
                .andExpect(jsonPath("$.creadoEn").value("2026-11-24T10:31:00"));
    }
}
