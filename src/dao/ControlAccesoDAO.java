package dao;

import coneccion.CreateConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.format.DateTimeFormatter;
import modelo.ResultadoValidacionAcceso;

/**
 * Controla el ingreso en una transaccion PostgreSQL.
 * No cambia las ventas, ni el inventario, ni el campo boletos.estado:
 * la tabla control_accesos representa que un boleto ya fue utilizado.
 */
public class ControlAccesoDAO {

    private static final DateTimeFormatter FECHA_FORMATO =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    private static final String SQL_BOLETO = """
            SELECT b.id_boleto, b.estado, d.id_venta,
                   i.id_concierto, c.titulo_evento,
                   l.nombre AS localidad, a.fila, a.numero
            FROM boletos b
            JOIN detalle_ventas d ON d.id_detalle = b.id_detalle
            JOIN inventario_boletos i ON i.id_inventario = d.id_inventario
            JOIN conciertos c ON c.id_concierto = i.id_concierto
            JOIN localidades l ON l.id_localidad = i.id_localidad
            LEFT JOIN asientos a ON a.id_asiento = b.id_asiento
            WHERE b.ruta_pdf = ?
            FOR UPDATE OF b
            """;

    private static final String SQL_INGRESO_EXISTENTE = """
            SELECT fecha_ingreso
            FROM control_accesos
            WHERE id_boleto = ?
            """;

    private static final String SQL_REGISTRAR_INGRESO = """
            INSERT INTO control_accesos (id_boleto, id_usuario)
            VALUES (?, ?)
            ON CONFLICT (id_boleto) DO NOTHING
            RETURNING fecha_ingreso
            """;

    /**
     * Requiere un usuario previamente autenticado con permisos para Control
     * de acceso. La autorizacion por rol se conectara en el controlador del
     * Administrador (su esquema de roles debe verificarse antes).
     */
    public ResultadoValidacionAcceso registrarIngreso(
            String rutaPdf, int idConcierto, int idUsuario)
            throws SQLException {

        try (Connection conexion = new CreateConnection().getConection()) {
            if (conexion == null) {
                throw new SQLException("No se pudo conectar a Neon PostgreSQL.");
            }

            conexion.setAutoCommit(false);
            try {
                if (!existeUsuario(conexion, idUsuario)) {
                    throw new SQLException("El usuario autenticado ya no existe.");
                }

                RegistroBoleto boleto = buscarYBloquearBoleto(conexion, rutaPdf);
                if (boleto == null) {
                    conexion.rollback();
                    return new ResultadoValidacionAcceso(
                            "NO_ENCONTRADO", "No existe un boleto con este QR.",
                            0, 0, 0, "", "", "", "");
                }

                if (boleto.idConcierto() != idConcierto) {
                    conexion.rollback();
                    return respuesta("OTRO_CONCIERTO",
                            "Este boleto pertenece a otro concierto.", boleto, "");
                }

                if ("cancelado".equalsIgnoreCase(boleto.estado())) {
                    conexion.rollback();
                    return respuesta("ANULADO",
                            "Acceso denegado: el boleto esta anulado.", boleto, "");
                }

                if (!"valido".equalsIgnoreCase(boleto.estado())) {
                    conexion.rollback();
                    return respuesta("ESTADO_NO_VALIDO",
                            "Acceso denegado: estado del boleto: " + boleto.estado(),
                            boleto, "");
                }

                String ingresoAnterior = consultarIngresoAnterior(
                        conexion, boleto.idBoleto());
                if (ingresoAnterior != null) {
                    conexion.rollback();
                    return respuesta("YA_UTILIZADO",
                            "Acceso denegado: este boleto ya ingreso.",
                            boleto, ingresoAnterior);
                }

                String fechaNueva = insertarIngreso(
                        conexion, boleto.idBoleto(), idUsuario);

                if (fechaNueva == null) {
                    // Defensa adicional: la restriccion UNIQUE evita duplicados
                    // incluso si otro proceso intenta registrar el mismo boleto.
                    conexion.rollback();
                    return respuesta("YA_UTILIZADO",
                            "Acceso denegado: este boleto ya fue registrado.",
                            boleto, "");
                }

                conexion.commit();
                return respuesta("AUTORIZADO",
                        "Ingreso autorizado y registrado correctamente.",
                        boleto, fechaNueva);

            } catch (SQLException | RuntimeException ex) {
                try {
                    conexion.rollback();
                } catch (SQLException errorRollback) {
                    ex.addSuppressed(errorRollback);
                }
                throw ex;
            }
        }
    }

    private boolean existeUsuario(Connection conexion, int idUsuario)
            throws SQLException {
        try (PreparedStatement ps = conexion.prepareStatement(
                "SELECT 1 FROM usuarios WHERE id_usuario = ?")) {
            ps.setInt(1, idUsuario);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    private RegistroBoleto buscarYBloquearBoleto(
            Connection conexion, String rutaPdf) throws SQLException {
        try (PreparedStatement ps = conexion.prepareStatement(SQL_BOLETO)) {
            ps.setString(1, rutaPdf);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }

                String fila = rs.getString("fila");
                int numero = rs.getInt("numero");
                boolean hayNumero = !rs.wasNull();
                String asiento = fila == null || !hayNumero
                        ? "Aforo (sin asiento numerado)"
                        : fila + "-" + numero;

                RegistroBoleto boleto = new RegistroBoleto(
                        rs.getInt("id_boleto"),
                        rs.getString("estado"),
                        rs.getInt("id_venta"),
                        rs.getInt("id_concierto"),
                        rs.getString("titulo_evento"),
                        rs.getString("localidad"),
                        asiento);

                if (rs.next()) {
                    throw new SQLException(
                            "Hay mas de un boleto asociado a la misma URL QR. "
                            + "No se autoriza el ingreso hasta corregir los datos.");
                }
                return boleto;
            }
        }
    }

    private String consultarIngresoAnterior(
            Connection conexion, int idBoleto) throws SQLException {
        try (PreparedStatement ps =
                     conexion.prepareStatement(SQL_INGRESO_EXISTENTE)) {
            ps.setInt(1, idBoleto);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? convertirFecha(rs.getTimestamp(1)) : null;
            }
        }
    }

    private String insertarIngreso(Connection conexion,
            int idBoleto, int idUsuario) throws SQLException {
        try (PreparedStatement ps =
                     conexion.prepareStatement(SQL_REGISTRAR_INGRESO)) {
            ps.setInt(1, idBoleto);
            ps.setInt(2, idUsuario);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? convertirFecha(rs.getTimestamp(1)) : null;
            }
        }
    }

    private String convertirFecha(Timestamp fecha) {
        return fecha == null ? "Sin fecha"
                : fecha.toLocalDateTime().format(FECHA_FORMATO);
    }

    private ResultadoValidacionAcceso respuesta(
            String codigo, String mensaje, RegistroBoleto boleto,
            String fechaIngreso) {
        return new ResultadoValidacionAcceso(
                codigo, mensaje, boleto.idBoleto(), boleto.idVenta(),
                boleto.idConcierto(), boleto.concierto(), boleto.localidad(),
                boleto.asiento(), fechaIngreso);
    }

    private record RegistroBoleto(
            int idBoleto, String estado, int idVenta, int idConcierto,
            String concierto, String localidad, String asiento) {
    }
}
