package dao;

import coneccion.CreateConnection;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Persistencia transaccional para las anulaciones de SVB-GUA.
 *
 * No elimina ventas, detalles, boletos ni PDFs. Cada boleto anulado conserva
 * su fila histórica; se registra una devolución SIMULADA de su precio.
 *
 * IMPORTANTE: este DAO exige un idUsuario existente, pero el control por rol
 * ADMINISTRADOR deberá reforzarse al integrar la interfaz y confirmar
 * el esquema concreto de roles de la tabla usuarios.
 */
public final class AnulacionDAO {

    private final CreateConnection conexiones = new CreateConnection();

    private static final String SQL_DETALLE = """
            SELECT b.id_boleto, b.codigo_boleto, b.estado, b.id_asiento,
                   d.id_venta, d.id_inventario, d.precio_unitario,
                   c.id_concierto, c.titulo_evento, l.nombre AS localidad,
                   a.fila, a.numero AS numero_asiento,
                   (ca.id_acceso IS NOT NULL) AS ya_ingreso,
                   (ab.id_anulacion IS NOT NULL) AS ya_anulado
              FROM boletos b
              JOIN detalle_ventas d ON d.id_detalle = b.id_detalle
              JOIN inventario_boletos i ON i.id_inventario = d.id_inventario
              JOIN conciertos c ON c.id_concierto = i.id_concierto
              JOIN localidades l ON l.id_localidad = i.id_localidad
              LEFT JOIN asientos a ON a.id_asiento = b.id_asiento
              LEFT JOIN control_accesos ca ON ca.id_boleto = b.id_boleto
              LEFT JOIN anulaciones_boletos ab ON ab.id_boleto = b.id_boleto
            """;

    /** Carga todos los boletos para el listado del Administrador.
     *  Se ordenan del más reciente al más antiguo. Sólo lectura.
     */
    public List<BoletoParaAnulacion> listarTodos() throws SQLException {
        try (Connection conn = conexiones.getConection()) {
            comprobarConexion(conn);
            List<BoletoParaAnulacion> boletos = new ArrayList<>();
            try (PreparedStatement ps = conn.prepareStatement(
                    SQL_DETALLE + " ORDER BY b.id_boleto DESC");
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    boletos.add(mapear(rs));
                }
            }
            return List.copyOf(boletos);
        }
    }

    /** Consulta de solo lectura para la pantalla de anulaciones. */
    public BoletoParaAnulacion consultarBoleto(int idBoleto) throws SQLException {
        if (idBoleto <= 0) throw new IllegalArgumentException("ID de boleto inválido.");
        try (Connection conn = conexiones.getConection()) {
            comprobarConexion(conn);
            try (PreparedStatement ps = conn.prepareStatement(
                    SQL_DETALLE + " WHERE b.id_boleto = ?")) {
                ps.setInt(1, idBoleto);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() ? mapear(rs) : null;
                }
            }
        }
    }

    /** Consulta todos los boletos de una venta sin modificarlos. */
    public List<BoletoParaAnulacion> consultarVenta(int idVenta) throws SQLException {
        if (idVenta <= 0) throw new IllegalArgumentException("ID de venta inválido.");
        try (Connection conn = conexiones.getConection()) {
            comprobarConexion(conn);
            List<BoletoParaAnulacion> resultado = new ArrayList<>();
            try (PreparedStatement ps = conn.prepareStatement(
                    SQL_DETALLE + " WHERE d.id_venta = ? ORDER BY b.id_boleto")) {
                ps.setInt(1, idVenta);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) resultado.add(mapear(rs));
                }
            }
            return List.copyOf(resultado);
        }
    }

    public ResultadoAnulacion anularBoleto(int idBoleto, int idAdministrador, String motivo)
            throws SQLException {
        return ejecutarAnulacion(false, idBoleto, idAdministrador, motivo);
    }

    public ResultadoAnulacion anularVentaCompleta(int idVenta, int idAdministrador,
            String motivo) throws SQLException {
        return ejecutarAnulacion(true, idVenta, idAdministrador, motivo);
    }

    /**
     * Orden de bloqueos: inventario(s) ASC -> boletos ASC -> asientos.
     * Se alinea con VentaService, que bloquea inventario antes de asientos.
     * El validador de accesos bloquea cada boleto; así se serializa la
     * validación simultánea con la anulación de la misma entrada.
     */
    private ResultadoAnulacion ejecutarAnulacion(boolean ventaCompleta, int identificador,
            int idAdministrador, String motivo) throws SQLException {

        try (Connection conn = conexiones.getConection()) {
            comprobarConexion(conn);
            conn.setAutoCommit(false);

            try {
                comprobarUsuarioExistente(conn, idAdministrador);

                // Descubrimiento inicial sin modificar datos.
                List<Integer> inventarios = listarInventarios(conn, ventaCompleta,
                        identificador);
                if (inventarios.isEmpty()) {
                    throw new SQLException(ventaCompleta
                            ? "La venta indicada no tiene boletos."
                            : "No se encontró el boleto indicado.");
                }

                // Bloquea las filas del inventario en orden determinista.
                for (int idInventario : inventarios) bloquearInventario(conn, idInventario);

                // Sólo después del inventario se bloquean los boletos.
                List<BoletoBloqueado> boletos = bloquearBoletos(conn, ventaCompleta,
                        identificador);
                if (boletos.isEmpty()) {
                    throw new SQLException("No se encontraron boletos para anular.");
                }
                if (!ventaCompleta && boletos.size() != 1) {
                    throw new SQLException("La búsqueda del boleto produjo un resultado ambiguo.");
                }

                // Primero valida TODOS, de modo que una venta completa no se
                // cancele parcialmente porque una entrada ya fue utilizada.
                for (BoletoBloqueado b : boletos) {
                    if (!"valido".equalsIgnoreCase(b.estado())) {
                        throw new SQLException("El boleto #" + b.idBoleto()
                                + " no se puede anular. Estado: " + b.estado() + ".");
                    }
                    if (tieneIngreso(conn, b.idBoleto())) {
                        throw new SQLException("El boleto #" + b.idBoleto()
                                + " ya ingresó al concierto. No puede anularse.");
                    }
                    if (yaEstaAnulado(conn, b.idBoleto())) {
                        throw new SQLException("El boleto #" + b.idBoleto()
                                + " ya tiene una anulación registrada.");
                    }
                }

                UUID idOperacion = UUID.randomUUID();
                String tipo = ventaCompleta ? "venta" : "boleto";
                BigDecimal monto = BigDecimal.ZERO;
                List<Integer> ids = new ArrayList<>();
                int idVenta = boletos.get(0).idVenta();

                for (BoletoBloqueado b : boletos) {
                    cambiarEstadoCancelado(conn, b.idBoleto());
                    if (b.idAsiento() != null) {
                        liberarAsiento(conn, b.idInventario(), b.idAsiento());
                    }
                    devolverInventario(conn, b.idInventario());
                    guardarHistorial(conn, b.idBoleto(), idAdministrador, idOperacion,
                            tipo, motivo, b.precio());
                    ids.add(b.idBoleto());
                    monto = monto.add(b.precio());
                }

                conn.commit();
                return new ResultadoAnulacion(idOperacion, idVenta, List.copyOf(ids),
                        monto, tipo);

            } catch (SQLException | RuntimeException ex) {
                try {
                    conn.rollback();
                } catch (SQLException errorRollback) {
                    ex.addSuppressed(errorRollback);
                }
                throw ex;
            }
        }
    }

    private List<Integer> listarInventarios(Connection conn, boolean ventaCompleta,
            int id) throws SQLException {
        String sql = ventaCompleta
                ? """
                    SELECT DISTINCT d.id_inventario
                      FROM detalle_ventas d
                      JOIN boletos b ON b.id_detalle = d.id_detalle
                     WHERE d.id_venta = ?
                     ORDER BY d.id_inventario
                    """
                : """
                    SELECT DISTINCT d.id_inventario
                      FROM boletos b
                      JOIN detalle_ventas d ON d.id_detalle = b.id_detalle
                     WHERE b.id_boleto = ?
                     ORDER BY d.id_inventario
                    """;
        List<Integer> ids = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) ids.add(rs.getInt(1));
            }
        }
        return ids;
    }

    private void bloquearInventario(Connection conn, int idInventario) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT id_inventario FROM inventario_boletos WHERE id_inventario = ? FOR UPDATE")) {
            ps.setInt(1, idInventario);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) throw new SQLException(
                        "No existe el inventario #" + idInventario + ".");
            }
        }
    }

    private List<BoletoBloqueado> bloquearBoletos(Connection conn,
            boolean ventaCompleta, int id) throws SQLException {
        String sql = """
                SELECT b.id_boleto, b.estado, b.id_asiento, d.id_venta,
                       d.id_inventario, d.precio_unitario
                  FROM boletos b
                  JOIN detalle_ventas d ON d.id_detalle = b.id_detalle
                """ + (ventaCompleta ? " WHERE d.id_venta = ? "
                                       : " WHERE b.id_boleto = ? ")
                + " ORDER BY b.id_boleto FOR UPDATE OF b";
        List<BoletoBloqueado> boletos = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int asiento = rs.getInt("id_asiento");
                    Integer idAsiento = rs.wasNull() ? null : asiento;
                    boletos.add(new BoletoBloqueado(
                            rs.getInt("id_boleto"), rs.getString("estado"), idAsiento,
                            rs.getInt("id_venta"), rs.getInt("id_inventario"),
                            rs.getBigDecimal("precio_unitario")));
                }
            }
        }
        return boletos;
    }

    private boolean tieneIngreso(Connection conn, int idBoleto) throws SQLException {
        return existe(conn, "SELECT 1 FROM control_accesos WHERE id_boleto = ?", idBoleto);
    }

    private boolean yaEstaAnulado(Connection conn, int idBoleto) throws SQLException {
        return existe(conn, "SELECT 1 FROM anulaciones_boletos WHERE id_boleto = ?", idBoleto);
    }

    private boolean existe(Connection conn, String sql, int id) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    private void cambiarEstadoCancelado(Connection conn, int idBoleto) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE boletos SET estado = 'cancelado' "
                + "WHERE id_boleto = ? AND estado = 'valido'")) {
            ps.setInt(1, idBoleto);
            if (ps.executeUpdate() != 1) {
                throw new SQLException("No se pudo cancelar el boleto #" + idBoleto + ".");
            }
        }
    }

    private void liberarAsiento(Connection conn, int idInventario, int idAsiento)
            throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("""
                UPDATE asientos
                   SET estado = 'disponible'
                 WHERE id_asiento = ? AND id_inventario = ? AND estado = 'vendido'
                """)) {
            ps.setInt(1, idAsiento);
            ps.setInt(2, idInventario);
            if (ps.executeUpdate() != 1) {
                throw new SQLException("El asiento #" + idAsiento
                        + " no está marcado como vendido. Se deshace la anulación.");
            }
        }
    }

    private void devolverInventario(Connection conn, int idInventario)
            throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("""
                UPDATE inventario_boletos
                   SET cantidad_disponible = cantidad_disponible + 1
                 WHERE id_inventario = ?
                   AND cantidad_disponible < cantidad_total
                """)) {
            ps.setInt(1, idInventario);
            if (ps.executeUpdate() != 1) {
                throw new SQLException("No se pudo devolver una entrada al inventario #"
                        + idInventario + ". Se deshace toda la operación.");
            }
        }
    }

    private void guardarHistorial(Connection conn, int idBoleto, int idAdministrador,
            UUID idOperacion, String tipo, String motivo, BigDecimal importe)
            throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("""
                INSERT INTO anulaciones_boletos
                  (id_boleto, id_usuario, id_operacion, tipo_operacion,
                   motivo, importe_anulado)
                VALUES (?, ?, ?, ?, ?, ?)
                """)) {
            ps.setInt(1, idBoleto);
            ps.setInt(2, idAdministrador);
            ps.setObject(3, idOperacion);
            ps.setString(4, tipo);
            ps.setString(5, motivo);
            ps.setBigDecimal(6, importe);
            if (ps.executeUpdate() != 1) {
                throw new SQLException("No se pudo guardar el historial del boleto #"
                        + idBoleto + ".");
            }
        }
    }

    private void comprobarUsuarioExistente(Connection conn, int idUsuario)
            throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT 1 FROM usuarios WHERE id_usuario = ?")) {
            ps.setInt(1, idUsuario);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    throw new SQLException("No existe el administrador #" + idUsuario + ".");
                }
            }
        }
    }

    private BoletoParaAnulacion mapear(ResultSet rs) throws SQLException {
        int asiento = rs.getInt("id_asiento");
        Integer idAsiento = rs.wasNull() ? null : asiento;
        int numero = rs.getInt("numero_asiento");
        Integer numeroAsiento = rs.wasNull() ? null : numero;
        return new BoletoParaAnulacion(
                rs.getInt("id_boleto"), rs.getInt("id_venta"),
                rs.getString("codigo_boleto"), rs.getString("titulo_evento"),
                rs.getString("localidad"), rs.getString("fila"), numeroAsiento,
                rs.getString("estado"), rs.getBoolean("ya_ingreso"),
                rs.getBoolean("ya_anulado"), idAsiento,
                rs.getBigDecimal("precio_unitario"),
                rs.getInt("id_concierto"));
    }

    private static void comprobarConexion(Connection conn) throws SQLException {
        if (conn == null) throw new SQLException("No se pudo conectar a Neon PostgreSQL.");
    }

    public record BoletoParaAnulacion(
            int idBoleto, int idVenta, String codigo, String concierto,
            String localidad, String fila, Integer numeroAsiento, String estado,
            boolean yaIngreso, boolean yaAnulado, Integer idAsiento,
            BigDecimal precio, int idConcierto) { }

    public record ResultadoAnulacion(
            UUID idOperacion, int idVenta, List<Integer> boletosAnulados,
            BigDecimal importeAnulado, String tipoOperacion) { }

    private record BoletoBloqueado(
            int idBoleto, String estado, Integer idAsiento,
            int idVenta, int idInventario, BigDecimal precio) { }
}
