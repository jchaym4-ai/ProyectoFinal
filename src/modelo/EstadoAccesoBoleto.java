package modelo;

import java.time.LocalDateTime;

/** Datos de lectura para la consulta de acceso de un boleto. */
public class EstadoAccesoBoleto {
    private final int idBoleto;
    private final String codigoBoleto;
    private final int idVenta;
    private final int idConcierto;
    private final String concierto;
    private final String localidad;
    private final String fila;
    private final String numeroAsiento;
    private final String estadoBoleto;
    private final String estadoAcceso;
    private final LocalDateTime fechaIngresoGT;

    public EstadoAccesoBoleto(int idBoleto, String codigoBoleto,
            int idVenta, int idConcierto, String concierto, String localidad,
            String fila, String numeroAsiento, String estadoBoleto,
            String estadoAcceso, LocalDateTime fechaIngresoGT) {
        this.idBoleto = idBoleto;
        this.codigoBoleto = codigoBoleto;
        this.idVenta = idVenta;
        this.idConcierto = idConcierto;
        this.concierto = concierto;
        this.localidad = localidad;
        this.fila = fila;
        this.numeroAsiento = numeroAsiento;
        this.estadoBoleto = estadoBoleto;
        this.estadoAcceso = estadoAcceso;
        this.fechaIngresoGT = fechaIngresoGT;
    }

    public int getIdBoleto() { return idBoleto; }
    public String getCodigoBoleto() { return codigoBoleto; }
    public int getIdVenta() { return idVenta; }
    public int getIdConcierto() { return idConcierto; }
    public String getConcierto() { return concierto; }
    public String getLocalidad() { return localidad; }
    public String getFila() { return fila; }
    public String getNumeroAsiento() { return numeroAsiento; }
    public String getEstadoBoleto() { return estadoBoleto; }
    public String getEstadoAcceso() { return estadoAcceso; }
    public LocalDateTime getFechaIngresoGT() { return fechaIngresoGT; }
}
