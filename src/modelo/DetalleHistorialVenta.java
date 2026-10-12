
package modelo;

import java.math.BigDecimal;

public class DetalleHistorialVenta {

    private final int idDetalle;
    private final String concierto;
    private final String localidad;
    private final int cantidad;
    private final BigDecimal precioUnitario;
    private final BigDecimal subtotal;

    public DetalleHistorialVenta(
            int idDetalle,
            String concierto,
            String localidad,
            int cantidad,
            BigDecimal precioUnitario,
            BigDecimal subtotal) {

        this.idDetalle = idDetalle;
        this.concierto = concierto;
        this.localidad = localidad;
        this.cantidad = cantidad;
        this.precioUnitario = precioUnitario;
        this.subtotal = subtotal;
    }

    public int getIdDetalle() {
        return idDetalle;
    }

    public String getConcierto() {
        return concierto;
    }

    public String getLocalidad() {
        return localidad;
    }

    public int getCantidad() {
        return cantidad;
    }

    public BigDecimal getPrecioUnitario() {
        return precioUnitario;
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }
}
