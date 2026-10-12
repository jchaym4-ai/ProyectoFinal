
package modelo;

import java.math.BigDecimal;

public class EstadisticasVendedor {

    private int ventasHoy;
    private int boletosVendidosHoy;
    private BigDecimal totalRecaudadoHoy;
    private int misVentasHoy;

    public EstadisticasVendedor() {
        this.totalRecaudadoHoy = BigDecimal.ZERO;
    }

    public int getVentasHoy() {
        return ventasHoy;
    }

    public void setVentasHoy(int ventasHoy) {
        this.ventasHoy = ventasHoy;
    }

    public int getBoletosVendidosHoy() {
        return boletosVendidosHoy;
    }

    public void setBoletosVendidosHoy(int boletosVendidosHoy) {
        this.boletosVendidosHoy = boletosVendidosHoy;
    }

    public BigDecimal getTotalRecaudadoHoy() {
        return totalRecaudadoHoy;
    }

    public void setTotalRecaudadoHoy(
            BigDecimal totalRecaudadoHoy) {
        this.totalRecaudadoHoy = totalRecaudadoHoy;
    }

    public int getMisVentasHoy() {
        return misVentasHoy;
    }

    public void setMisVentasHoy(int misVentasHoy) {
        this.misVentasHoy = misVentasHoy;
    }
}
