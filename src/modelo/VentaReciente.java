
package modelo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class VentaReciente {

    private int idVenta;
    private LocalDateTime fechaVenta;
    private String nombreVendedor;
    private BigDecimal totalVenta;

    public VentaReciente(
            int idVenta,
            LocalDateTime fechaVenta,
            String nombreVendedor,
            BigDecimal totalVenta) {

        this.idVenta = idVenta;
        this.fechaVenta = fechaVenta;
        this.nombreVendedor = nombreVendedor;
        this.totalVenta = totalVenta;
    }

    public int getIdVenta() {
        return idVenta;
    }

    public LocalDateTime getFechaVenta() {
        return fechaVenta;
    }

    public String getNombreVendedor() {
        return nombreVendedor;
    }

    public BigDecimal getTotalVenta() {
        return totalVenta;
    }
}
