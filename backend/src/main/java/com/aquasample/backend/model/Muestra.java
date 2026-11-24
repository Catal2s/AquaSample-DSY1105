package com.aquasample.backend.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * Tabla "muestras" del backend.
 *
 * El id NO es autogenerado: lo trae la app (el mismo id de Room).
 * Asi, si WorkManager reintenta el envio, se actualiza la misma fila
 * en vez de crear un duplicado.
 */
@Entity
@Table(name = "muestras")
public class Muestra {

    @Id
    private Integer id;

    @Column(nullable = false)
    private String centroId;
    @Column(nullable = false)
    private String trenId;
    @Column(nullable = false)
    private String lineaId;

    /** Formato yyyy-MM-dd, igual que en la app. */
    @Column(nullable = false, length = 10)
    private String fecha;
    /** Formato HH:mm. */
    @Column(nullable = false, length = 5)
    private String hora;

    private double tramoMetros;

    @Column(nullable = false)
    private String operadorId;

    private int individuos;
    private Double calibrePromedioMm;
    private boolean asistidoPorFoto;
    private int marcasRegistradas;
    private int ajusteManual;

    @Column(length = 500)
    private String observaciones;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoRevision estado = EstadoRevision.PENDIENTE;

    @Column(length = 500)
    private String comentarioSupervisor;

    /** Metadatos de la foto guardada en el servidor (null si aun no se sube). */
    private String fotoNombre;
    private String fotoTipo;
    private Long fotoTamanoBytes;

    private LocalDateTime creadoEn;
    private LocalDateTime actualizadoEn;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getCentroId() { return centroId; }
    public void setCentroId(String centroId) { this.centroId = centroId; }

    public String getTrenId() { return trenId; }
    public void setTrenId(String trenId) { this.trenId = trenId; }

    public String getLineaId() { return lineaId; }
    public void setLineaId(String lineaId) { this.lineaId = lineaId; }

    public String getFecha() { return fecha; }
    public void setFecha(String fecha) { this.fecha = fecha; }

    public String getHora() { return hora; }
    public void setHora(String hora) { this.hora = hora; }

    public double getTramoMetros() { return tramoMetros; }
    public void setTramoMetros(double tramoMetros) { this.tramoMetros = tramoMetros; }

    public String getOperadorId() { return operadorId; }
    public void setOperadorId(String operadorId) { this.operadorId = operadorId; }

    public int getIndividuos() { return individuos; }
    public void setIndividuos(int individuos) { this.individuos = individuos; }

    public Double getCalibrePromedioMm() { return calibrePromedioMm; }
    public void setCalibrePromedioMm(Double calibrePromedioMm) { this.calibrePromedioMm = calibrePromedioMm; }

    public boolean isAsistidoPorFoto() { return asistidoPorFoto; }
    public void setAsistidoPorFoto(boolean asistidoPorFoto) { this.asistidoPorFoto = asistidoPorFoto; }

    public int getMarcasRegistradas() { return marcasRegistradas; }
    public void setMarcasRegistradas(int marcasRegistradas) { this.marcasRegistradas = marcasRegistradas; }

    public int getAjusteManual() { return ajusteManual; }
    public void setAjusteManual(int ajusteManual) { this.ajusteManual = ajusteManual; }

    public String getObservaciones() { return observaciones; }
    public void setObservaciones(String observaciones) { this.observaciones = observaciones; }

    public EstadoRevision getEstado() { return estado; }
    public void setEstado(EstadoRevision estado) { this.estado = estado; }

    public String getComentarioSupervisor() { return comentarioSupervisor; }
    public void setComentarioSupervisor(String comentarioSupervisor) { this.comentarioSupervisor = comentarioSupervisor; }

    public String getFotoNombre() { return fotoNombre; }
    public void setFotoNombre(String fotoNombre) { this.fotoNombre = fotoNombre; }

    public String getFotoTipo() { return fotoTipo; }
    public void setFotoTipo(String fotoTipo) { this.fotoTipo = fotoTipo; }

    public Long getFotoTamanoBytes() { return fotoTamanoBytes; }
    public void setFotoTamanoBytes(Long fotoTamanoBytes) { this.fotoTamanoBytes = fotoTamanoBytes; }

    public LocalDateTime getCreadoEn() { return creadoEn; }
    public void setCreadoEn(LocalDateTime creadoEn) { this.creadoEn = creadoEn; }

    public LocalDateTime getActualizadoEn() { return actualizadoEn; }
    public void setActualizadoEn(LocalDateTime actualizadoEn) { this.actualizadoEn = actualizadoEn; }
}
