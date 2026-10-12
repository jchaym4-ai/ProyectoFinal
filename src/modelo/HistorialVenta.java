
package modelo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class HistorialVenta {

    private final int idVenta;
    private final LocalDateTime fechaVenta;
    private final String vendedor;
    private final int cantidadBoletos;
    private final BigDecimal totalVenta;

    public HistorialVenta(
            int idVenta,
            LocalDateTime fechaVenta,
            String vendedor,
            int cantidadBoletos,
            BigDecimal totalVenta) {

        this.idVenta = idVenta;
        this.fechaVenta = fechaVenta;
        this.vendedor = vendedor;
        this.cantidadBoletos = cantidadBoletos;
        this.totalVenta = totalVenta;
    }

    public int getIdVenta() {
        return idVenta;
    }

    public LocalDateTime getFechaVenta() {
        return fechaVenta;
    }

    public String getVendedor() {
        return vendedor;
    }

    public int getCantidadBoletos() {
        return cantidadBoletos;
    }

    public BigDecimal getTotalVenta() {
        return totalVenta;
    }
}
