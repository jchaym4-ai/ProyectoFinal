package modelo;

import java.math.BigDecimal;

public class InventarioBoleto {

    private int idInventario;
    private int idConcierto;
    private int idLocalidad;

    private BigDecimal precio;

    private int cantidadTotal;
    private int cantidadDisponible;

    private String tipoVenta;

    public InventarioBoleto() {
    }

    public InventarioBoleto(
            int idInventario,
            int idConcierto,
            int idLocalidad,
            BigDecimal precio,
            int cantidadTotal,
            int cantidadDisponible,
            String tipoVenta) {

        this.idInventario = idInventario;
        this.idConcierto = idConcierto;
        this.idLocalidad = idLocalidad;
        this.precio = precio;
        this.cantidadTotal = cantidadTotal;
        this.cantidadDisponible = cantidadDisponible;
        this.tipoVenta = tipoVenta;
    }

    public int getIdInventario() {
        return idInventario;
    }

    public void setIdInventario(int idInventario) {
        this.idInventario = idInventario;
    }

    public int getIdConcierto() {
        return idConcierto;
    }

    public void setIdConcierto(int idConcierto) {
        this.idConcierto = idConcierto;
    }

    public int getIdLocalidad() {
        return idLocalidad;
    }

    public void setIdLocalidad(int idLocalidad) {
        this.idLocalidad = idLocalidad;
    }

    public BigDecimal getPrecio() {
        return precio;
    }

    public void setPrecio(BigDecimal precio) {
        this.precio = precio;
    }

    public int getCantidadTotal() {
        return cantidadTotal;
    }

    public void setCantidadTotal(int cantidadTotal) {
        this.cantidadTotal = cantidadTotal;
    }

    public int getCantidadDisponible() {
        return cantidadDisponible;
    }

    public void setCantidadDisponible(int cantidadDisponible) {
        this.cantidadDisponible = cantidadDisponible;
    }

    public String getTipoVenta() {
        return tipoVenta;
    }

    public void setTipoVenta(String tipoVenta) {
        this.tipoVenta = tipoVenta;
    }
}