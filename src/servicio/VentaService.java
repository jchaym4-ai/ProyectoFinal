
package servicio;

import coneccion.CreateConnection;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class VentaService {

    private final CreateConnection connFactory;

    public VentaService() {
        this.connFactory = new CreateConnection();
    }

    public int registrarVenta(
            int idVendedor,
            int idInventario,
            int cantidad,
            List<Integer> idsAsientos) throws SQLException {

        if (idVendedor <= 0 || idInventario <= 0) {
            throw new IllegalArgumentException(
                    "El vendedor y el inventario deben ser válidos."
            );
        }

        if (cantidad <= 0) {
            throw new IllegalArgumentException(
                    "La cantidad debe ser mayor que cero."
            );
        }

        List<Integer> asientos = idsAsientos == null
                ? new ArrayList<>()
                : new ArrayList<>(idsAsientos);

        try (Connection conn = connFactory.getConection()) {

            if (conn == null) {
                throw new SQLException(
                        "No se pudo conectar a PostgreSQL."
                );
            }

            conn.setAutoCommit(false);

            try {

                String sqlInventario = """
                        SELECT
                            precio,
                            cantidad_disponible,
                            tipo_venta
                        FROM inventario_boletos
                        WHERE id_inventario = ?
                        FOR UPDATE
                        """;

                BigDecimal precio;
                int disponibles;
                String tipoVenta;

                try (PreparedStatement ps =
                        conn.prepareStatement(sqlInventario)) {

                    ps.setInt(1, idInventario);

                    try (ResultSet rs = ps.executeQuery()) {

                        if (!rs.next()) {
                            throw new SQLException(
                                    "El inventario no existe."
                            );
                        }

                        precio = rs.getBigDecimal("precio");
                        disponibles = rs.getInt(
                                "cantidad_disponible"
                        );
                        tipoVenta = rs.getString("tipo_venta");
                    }
                }

                if (disponibles < cantidad) {
                    throw new SQLException(
                            "No hay suficientes boletos disponibles."
                    );
                }

                if ("numerado".equalsIgnoreCase(tipoVenta)) {

                    if (asientos.size() != cantidad) {
                        throw new SQLException(
                                "La cantidad no coincide con "
                                + "los asientos seleccionados."
                        );
                    }

                    Set<Integer> unicos = new HashSet<>(asientos);

                    if (unicos.size() != asientos.size()
                            || unicos.contains(null)) {

                        throw new SQLException(
                                "La selección contiene asientos "
                                + "duplicados o inválidos."
                        );
                    }

                    for (Integer idAsiento : asientos) {

                        if (idAsiento <= 0) {
                            throw new SQLException(
                                    "Existe un asiento inválido."
                            );
                        }
                    }

                    verificarAsientos(
                            conn,
                            idInventario,
                            asientos
                    );

                } else if ("aforo".equalsIgnoreCase(tipoVenta)) {

                    if (!asientos.isEmpty()) {
                        throw new SQLException(
                                "Las localidades de aforo "
                                + "no admiten asientos numerados."
                        );
                    }

                } else {

                    throw new SQLException(
                            "Tipo de venta no reconocido."
                    );
                }

                BigDecimal total = precio.multiply(
                        BigDecimal.valueOf(cantidad)
                );

                int idVenta = insertarVenta(
                        conn,
                        idVendedor,
                        total
                );

                int idDetalle = insertarDetalle(
                        conn,
                        idVenta,
                        idInventario,
                        cantidad,
                        precio,
                        total
                );

                if ("numerado".equalsIgnoreCase(tipoVenta)) {

                    for (Integer idAsiento : asientos) {

                        marcarAsientoVendido(
                                conn,
                                idInventario,
                                idAsiento
                        );

                        insertarBoleto(
                                conn,
                                idDetalle,
                                idAsiento
                        );
                    }

                } else {

                    for (int i = 0; i < cantidad; i++) {

                        insertarBoleto(
                                conn,
                                idDetalle,
                                null
                        );
                    }
                }

                String sqlDescontar = """
                        UPDATE inventario_boletos
                        SET cantidad_disponible =
                            cantidad_disponible - ?
                        WHERE id_inventario = ?
                          AND cantidad_disponible >= ?
                        """;

                try (PreparedStatement ps =
                        conn.prepareStatement(sqlDescontar)) {

                    ps.setInt(1, cantidad);
                    ps.setInt(2, idInventario);
                    ps.setInt(3, cantidad);

                    if (ps.executeUpdate() != 1) {
                        throw new SQLException(
                                "No se pudo descontar el inventario."
                        );
                    }
                }

                conn.commit();

                return idVenta;

            } catch (SQLException | RuntimeException e) {

                try {
                    conn.rollback();
                } catch (SQLException rollbackError) {
                    e.addSuppressed(rollbackError);
                }

                throw e;
            }
        }
    }

    private void verificarAsientos(
            Connection conn,
            int idInventario,
            List<Integer> idsAsientos) throws SQLException {

        String sql = """
                SELECT estado
                FROM asientos
                WHERE id_asiento = ?
                  AND id_inventario = ?
                FOR UPDATE
                """;

        List<Integer> ordenados = new ArrayList<>(idsAsientos);
        ordenados.sort(Integer::compareTo);

        try (PreparedStatement ps =
                conn.prepareStatement(sql)) {

            for (Integer idAsiento : ordenados) {

                ps.setInt(1, idAsiento);
                ps.setInt(2, idInventario);

                try (ResultSet rs = ps.executeQuery()) {

                    if (!rs.next()) {
                        throw new SQLException(
                                "El asiento " + idAsiento
                                + " no pertenece al inventario."
                        );
                    }

                    if (!"disponible".equalsIgnoreCase(
                            rs.getString("estado"))) {

                        throw new SQLException(
                                "El asiento " + idAsiento
                                + " ya no está disponible."
                        );
                    }
                }
            }
        }
    }

    private int insertarVenta(
            Connection conn,
            int idVendedor,
            BigDecimal total) throws SQLException {

        String sql = """
                INSERT INTO ventas
                    (id_vendedor, total_venta)
                VALUES (?, ?)
                RETURNING id_venta
                """;

        try (PreparedStatement ps =
                conn.prepareStatement(sql)) {

            ps.setInt(1, idVendedor);
            ps.setBigDecimal(2, total);

            try (ResultSet rs = ps.executeQuery()) {

                if (!rs.next()) {
                    throw new SQLException(
                            "No se pudo registrar la venta."
                    );
                }

                return rs.getInt("id_venta");
            }
        }
    }

    private int insertarDetalle(
            Connection conn,
            int idVenta,
            int idInventario,
            int cantidad,
            BigDecimal precio,
            BigDecimal subtotal) throws SQLException {

        String sql = """
                INSERT INTO detalle_ventas
                    (id_venta,
                     id_inventario,
                     cantidad,
                     precio_unitario,
                     subtotal)
                VALUES (?, ?, ?, ?, ?)
                RETURNING id_detalle
                """;

        try (PreparedStatement ps =
                conn.prepareStatement(sql)) {

            ps.setInt(1, idVenta);
            ps.setInt(2, idInventario);
            ps.setInt(3, cantidad);
            ps.setBigDecimal(4, precio);
            ps.setBigDecimal(5, subtotal);

            try (ResultSet rs = ps.executeQuery()) {

                if (!rs.next()) {
                    throw new SQLException(
                            "No se pudo registrar el detalle."
                    );
                }

                return rs.getInt("id_detalle");
            }
        }
    }

    private void marcarAsientoVendido(
            Connection conn,
            int idInventario,
            int idAsiento) throws SQLException {

        String sql = """
                UPDATE asientos
                SET estado = 'vendido'
                WHERE id_asiento = ?
                  AND id_inventario = ?
                  AND estado = 'disponible'
                """;

        try (PreparedStatement ps =
                conn.prepareStatement(sql)) {

            ps.setInt(1, idAsiento);
            ps.setInt(2, idInventario);

            if (ps.executeUpdate() != 1) {
                throw new SQLException(
                        "No se pudo vender el asiento "
                        + idAsiento
                );
            }
        }
    }

    private void insertarBoleto(
            Connection conn,
            int idDetalle,
            Integer idAsiento) throws SQLException {

        String sql = """
                INSERT INTO boletos
                    (id_detalle,
                     id_asiento,
                     codigo_boleto,
                     estado)
                VALUES (?, ?, ?, 'valido')
                """;

        try (PreparedStatement ps =
                conn.prepareStatement(sql)) {

            ps.setInt(1, idDetalle);

            if (idAsiento == null) {
                ps.setNull(2, java.sql.Types.INTEGER);
            } else {
                ps.setInt(2, idAsiento);
            }

            ps.setString(
                    3,
                    "SVB-" + UUID.randomUUID()
            );

            ps.executeUpdate();
        }
    }
}
