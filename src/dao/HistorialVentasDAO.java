
package dao;

import coneccion.CreateConnection;
import modelo.HistorialVenta;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import modelo.DetalleHistorialVenta;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class HistorialVentasDAO {

    private final CreateConnection conexion;

    public HistorialVentasDAO() {
        conexion = new CreateConnection();
    }

    public List<HistorialVenta> buscarVentas(
            int idVendedor,
            String busqueda,
            LocalDate fechaDesde,
            LocalDate fechaHasta) throws SQLException {

        if (idVendedor <= 0) {
            throw new IllegalArgumentException(
                    "El vendedor no es válido."
            );
        }

        if (fechaDesde != null && fechaHasta != null
                && fechaDesde.isAfter(fechaHasta)) {
            throw new IllegalArgumentException(
                    "La fecha inicial no puede ser mayor "
                    + "que la fecha final."
            );
        }

        List<HistorialVenta> historial =
                new ArrayList<>();

        StringBuilder sql = new StringBuilder(
                "SELECT v.id_venta, "
                + "v.fecha_venta, "
                + "u.nombre_completo, "
                + "v.total_venta, "
                + "COALESCE(("
                + "SELECT SUM(d.cantidad) "
                + "FROM detalle_ventas d "
                + "WHERE d.id_venta = v.id_venta"
                + "), 0) AS cantidad_boletos "
                + "FROM ventas v "
                + "INNER JOIN usuarios u "
                + "ON v.id_vendedor = u.id_usuario "
                + "WHERE v.id_vendedor = ? "
        );

        String texto = busqueda == null
                ? "" : busqueda.trim();

        boolean buscarTexto = !texto.isEmpty();

        if (buscarTexto) {
            sql.append(
                    "AND ("
                    + "CAST(v.id_venta AS TEXT) ILIKE ? "
                    + "OR u.nombre_completo ILIKE ? "
                    + "OR EXISTS ("
                    + "SELECT 1 FROM detalle_ventas d2 "
                    + "INNER JOIN inventario_boletos i "
                    + "ON d2.id_inventario = i.id_inventario "
                    + "INNER JOIN conciertos c "
                    + "ON i.id_concierto = c.id_concierto "
                    + "INNER JOIN artistas a "
                    + "ON c.id_artista = a.id_artista "
                    + "WHERE d2.id_venta = v.id_venta "
                    + "AND (c.titulo_evento ILIKE ? "
                    + "OR a.nombre_artistico ILIKE ?)"
                    + ")) "
            );
        }

        if (fechaDesde != null) {
            sql.append("AND v.fecha_venta >= ? ");
        }

        if (fechaHasta != null) {
            sql.append("AND v.fecha_venta < ? ");
        }

        sql.append(
                "ORDER BY v.fecha_venta DESC NULLS LAST, "
                + "v.id_venta DESC"
        );

        Connection conn = conexion.getConection();

        if (conn == null) {
            throw new SQLException(
                    "No se pudo conectar a PostgreSQL."
            );
        }

        try (conn;
             PreparedStatement ps =
                     conn.prepareStatement(sql.toString())) {

            int indice = 1;

            ps.setInt(indice++, idVendedor);

            if (buscarTexto) {
                String patron = "%" + texto + "%";

                ps.setString(indice++, patron);
                ps.setString(indice++, patron);
                ps.setString(indice++, patron);
                ps.setString(indice++, patron);
            }

            if (fechaDesde != null) {
                ps.setTimestamp(
                        indice++,
                        Timestamp.valueOf(
                                fechaDesde.atStartOfDay()
                        )
                );
            }

            if (fechaHasta != null) {
                ps.setTimestamp(
                        indice++,
                        Timestamp.valueOf(
                                fechaHasta.plusDays(1).atStartOfDay()
                        )
                );
            }

            try (ResultSet rs = ps.executeQuery()) {

                while (rs.next()) {

                    Timestamp fecha =
                            rs.getTimestamp("fecha_venta");

                    HistorialVenta venta =
                            new HistorialVenta(
                                    rs.getInt("id_venta"),

                                    fecha == null
                                        ? null
                                        : fecha.toLocalDateTime(),

                                    rs.getString("nombre_completo"),

                                    rs.getInt("cantidad_boletos"),

                                    rs.getBigDecimal("total_venta")
                            );

                    historial.add(venta);
                }
            }
        }

        return historial;
    }
    
public List<DetalleHistorialVenta> obtenerDetallesVenta(
        int idVenta,
        int idVendedor) throws SQLException {

    if (idVenta <= 0 || idVendedor <= 0) {
        throw new IllegalArgumentException(
                "La venta o el vendedor no son válidos."
        );
    }

    List<DetalleHistorialVenta> detalles =
            new ArrayList<>();

    String sql =
            "SELECT d.id_detalle, "
            + "c.titulo_evento AS concierto, "
            + "l.nombre AS localidad, "
            + "d.cantidad, "
            + "d.precio_unitario, "
            + "d.subtotal "
            + "FROM detalle_ventas d "
            + "INNER JOIN ventas v "
            + "ON d.id_venta = v.id_venta "
            + "INNER JOIN inventario_boletos i "
            + "ON d.id_inventario = i.id_inventario "
            + "INNER JOIN conciertos c "
            + "ON i.id_concierto = c.id_concierto "
            + "INNER JOIN localidades l "
            + "ON i.id_localidad = l.id_localidad "
            + "WHERE d.id_venta = ? "
            + "AND v.id_vendedor = ? "
            + "ORDER BY d.id_detalle";

    Connection conn = conexion.getConection();

    if (conn == null) {
        throw new SQLException(
                "No se pudo conectar a PostgreSQL."
        );
    }

    try (conn;
         PreparedStatement ps = conn.prepareStatement(sql)) {

        ps.setInt(1, idVenta);
        ps.setInt(2, idVendedor);

        try (ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {

                DetalleHistorialVenta detalle =
                        new DetalleHistorialVenta(
                                rs.getInt("id_detalle"),
                                rs.getString("concierto"),
                                rs.getString("localidad"),
                                rs.getInt("cantidad"),
                                rs.getBigDecimal("precio_unitario"),
                                rs.getBigDecimal("subtotal")
                        );

                detalles.add(detalle);
            }
        }
    }

    return detalles;
}

}
