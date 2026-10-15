
package modelo;

import java.time.LocalDateTime;

public class Boleto {

    private int idBoleto;
    private int idDetalle;
    private Integer idAsiento;
    private String codigoBoleto;
    private LocalDateTime fechaEmision;
    private String estado;
    private String rutaPdf;

    public Boleto() {
    }

    public Boleto(
            int idBoleto,
            int idDetalle,
            Integer idAsiento,
            String codigoBoleto,
            LocalDateTime fechaEmision,
            String estado,
            String rutaPdf) {

        this.idBoleto = idBoleto;
        this.idDetalle = idDetalle;
        this.idAsiento = idAsiento;
        this.codigoBoleto = codigoBoleto;
        this.fechaEmision = fechaEmision;
        this.estado = estado;
        this.rutaPdf = rutaPdf;
    }

    public int getIdBoleto() {
        return idBoleto;
    }

    public void setIdBoleto(int idBoleto) {
        this.idBoleto = idBoleto;
    }

    public int getIdDetalle() {
        return idDetalle;
    }

    public void setIdDetalle(int idDetalle) {
        this.idDetalle = idDetalle;
    }

    public Integer getIdAsiento() {
        return idAsiento;
    }

    public void setIdAsiento(Integer idAsiento) {
        this.idAsiento = idAsiento;
    }

    public String getCodigoBoleto() {
        return codigoBoleto;
    }

    public void setCodigoBoleto(String codigoBoleto) {
        this.codigoBoleto = codigoBoleto;
    }

    public LocalDateTime getFechaEmision() {
        return fechaEmision;
    }

    public void setFechaEmision(LocalDateTime fechaEmision) {
        this.fechaEmision = fechaEmision;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public String getRutaPdf() {
        return rutaPdf;
    }

    public void setRutaPdf(String rutaPdf) {
        this.rutaPdf = rutaPdf;
    }
}
