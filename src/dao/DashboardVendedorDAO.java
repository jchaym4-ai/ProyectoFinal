
package dao;

import coneccion.CreateConnection;
import modelo.EstadisticasVendedor;

import modelo.VentaReciente;
import java.util.ArrayList;
import java.util.List;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class DashboardVendedorDAO {

    private final CreateConnection conexion;

    public DashboardVendedorDAO() {
        conexion = new CreateConnection();
    }

    public EstadisticasVendedor obtenerEstadisticas(
            int idVendedor) throws SQLException {

        if (idVendedor <= 0) {
            throw new IllegalArgumentException(
                    "El vendedor no es válido.");
        }

        EstadisticasVendedor estadisticas =
                new EstadisticasVendedor();

        String sqlVentas =
                "SELECT COUNT(*) AS ventas_hoy, "
                + "COALESCE(SUM(total_venta), 0) AS total_hoy, "
                + "COUNT(*) FILTER "
                + "(WHERE id_vendedor = ?) AS mis_ventas "
                + "FROM ventas "
                + "WHERE fecha_venta >= CURRENT_DATE "
                + "AND fecha_venta < CURRENT_DATE "
                + "+ INTERVAL '1 day'";

        String sqlBoletos =
                "SELECT COALESCE(SUM(d.cantidad), 0) "
                + "AS boletos_hoy "
                + "FROM detalle_ventas d "
                + "INNER JOIN ventas v "
                + "ON d.id_venta = v.id_venta "
                + "WHERE v.fecha_venta >= CURRENT_DATE "
                + "AND v.fecha_venta < CURRENT_DATE "
                + "+ INTERVAL '1 day'";

        Connection conn = conexion.getConection();

        if (conn == null) {
            throw new SQLException(
                    "No se pudo conectar a PostgreSQL.");
        }

        try (conn) {

            // Consultar ventas y recaudación
            try (PreparedStatement ps =
                    conn.prepareStatement(sqlVentas)) {

                ps.setInt(1, idVendedor);

                try (ResultSet rs = ps.executeQuery()) {

                    if (rs.next()) {

                        estadisticas.setVentasHoy(
                                rs.getInt("ventas_hoy"));

                        BigDecimal total =
                                rs.getBigDecimal("total_hoy");

                        estadisticas.setTotalRecaudadoHoy(
                                total);

                        estadisticas.setMisVentasHoy(
                                rs.getInt("mis_ventas"));
                    }
                }
            }

            // Consultar boletos vendidos
            try (PreparedStatement ps =
                    conn.prepareStatement(sqlBoletos);

                 ResultSet rs = ps.executeQuery()) {

                if (rs.next()) {

                    estadisticas.setBoletosVendidosHoy(
                            rs.getInt("boletos_hoy"));
                }
            }
        }

        return estadisticas;
    }
    
public List<VentaReciente> obtenerVentasRecientes(
        int idVendedor) throws SQLException {

    if (idVendedor <= 0) {
        throw new IllegalArgumentException(
                "El vendedor no es válido."
        );
    }

    List<VentaReciente> ventas = new ArrayList<>();

    String sql =
            "SELECT v.id_venta, "
            + "v.fecha_venta, "
            + "u.nombre_completo, "
            + "v.total_venta "
            + "FROM ventas v "
            + "INNER JOIN usuarios u "
            + "ON v.id_vendedor = u.id_usuario "
            + "WHERE v.id_vendedor = ? "
            + "ORDER BY v.fecha_venta DESC NULLS LAST, "
            + "v.id_venta DESC "
            + "LIMIT 10";

    Connection conn = conexion.getConection();

    if (conn == null) {
        throw new SQLException(
                "No se pudo conectar a PostgreSQL."
        );
    }

    try (conn;
         PreparedStatement ps = conn.prepareStatement(sql)) {

        ps.setInt(1, idVendedor);

        try (ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {

                java.sql.Timestamp fecha =
                        rs.getTimestamp("fecha_venta");

                VentaReciente venta = new VentaReciente(
                        rs.getInt("id_venta"),
                        fecha == null
                            ? null
                            : fecha.toLocalDateTime(),
                        rs.getString("nombre_completo"),
                        rs.getBigDecimal("total_venta")
                );

                ventas.add(venta);
            }
        }
    }

    return ventas;
}

}
