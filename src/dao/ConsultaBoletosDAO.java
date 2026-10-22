package dao;

import coneccion.CreateConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.ArrayList;
import java.util.List;
import modelo.DetalleBoleto;

/** Consulta todos los boletos de Neon, con sus datos para la tabla. */
public class ConsultaBoletosDAO {

    private final CreateConnection connFactory = new CreateConnection();

    /**
     * Conserva el método anterior para cualquier otro módulo que ya lo utilice.
     */
    public List<DetalleBoleto> listarTodos() throws SQLException {
        return listarTodosConAcceso().boletos();
    }

    /** El estado de ingreso se obtiene en la MISMA consulta que los boletos. */
    public ResultadoConsulta listarTodosConAcceso() throws SQLException {
        String sql = """
                SELECT
                    b.id_boleto,
                    v.id_venta,
                    b.codigo_boleto,
                    b.estado,
                    b.fecha_emision,
                    b.ruta_pdf,
                    c.titulo_evento,
                    c.fecha_concierto,
                    c.recinto,
                    ar.nombre_artistico AS nombre_artista,
                    l.nombre AS localidad,
                    i.tipo_venta,
                    d.precio_unitario,
                    a.fila,
                    a.numero AS numero_asiento,
                    acceso.estado_acceso,
                    acceso.fecha_ingreso_gt
                FROM boletos b
                INNER JOIN detalle_ventas d ON b.id_detalle = d.id_detalle
                INNER JOIN ventas v ON d.id_venta = v.id_venta
                INNER JOIN inventario_boletos i ON d.id_inventario = i.id_inventario
                INNER JOIN conciertos c ON i.id_concierto = c.id_concierto
                INNER JOIN artistas ar ON c.id_artista = ar.id_artista
                INNER JOIN localidades l ON i.id_localidad = l.id_localidad
                LEFT JOIN asientos a ON b.id_asiento = a.id_asiento
                LEFT JOIN vw_estado_acceso_boletos acceso
                    ON acceso.id_boleto = b.id_boleto
                ORDER BY b.id_boleto DESC
                """;

        List<DetalleBoleto> boletos = new ArrayList<>();
        Map<Integer, EstadoAcceso> accesos = new LinkedHashMap<>();

        try (Connection conn = connFactory.getConection()) {
            if (conn == null) {
                throw new SQLException("No se pudo conectar a Neon PostgreSQL.");
            }

            try (PreparedStatement ps = conn.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {

                while (rs.next()) {
                    DetalleBoleto detalle = new DetalleBoleto();
                    detalle.setIdBoleto(rs.getInt("id_boleto"));
                    detalle.setIdVenta(rs.getInt("id_venta"));
                    detalle.setCodigoBoleto(rs.getString("codigo_boleto"));
                    detalle.setEstado(rs.getString("estado"));
                    detalle.setRutaPdf(rs.getString("ruta_pdf"));
                    detalle.setTituloEvento(rs.getString("titulo_evento"));
                    detalle.setRecinto(rs.getString("recinto"));
                    detalle.setNombreArtista(rs.getString("nombre_artista"));
                    detalle.setLocalidad(rs.getString("localidad"));
                    detalle.setTipoVenta(rs.getString("tipo_venta"));
                    detalle.setPrecio(rs.getBigDecimal("precio_unitario"));
                    detalle.setFila(rs.getString("fila"));

                    Timestamp emision = rs.getTimestamp("fecha_emision");
                    if (emision != null) {
                        detalle.setFechaEmision(emision.toLocalDateTime());
                    }

                    Timestamp concierto = rs.getTimestamp("fecha_concierto");
                    if (concierto != null) {
                        detalle.setFechaConcierto(concierto.toLocalDateTime());
                    }

                    int asiento = rs.getInt("numero_asiento");
                    detalle.setNumeroAsiento(rs.wasNull() ? null : asiento);
                    boletos.add(detalle);

                    // Vista creada en el Paso 73: consulta de solo lectura.
                    Timestamp ingreso = rs.getTimestamp("fecha_ingreso_gt");
                    accesos.put(detalle.getIdBoleto(), new EstadoAcceso(
                            rs.getString("estado_acceso"),
                            ingreso == null ? null : ingreso.toLocalDateTime()
                    ));
                }
            }
        }

        return new ResultadoConsulta(List.copyOf(boletos), Map.copyOf(accesos));
    }

    /** Información de ingreso asociada con un boleto. */
    public record EstadoAcceso(String descripcion, LocalDateTime fechaIngreso) { }

    /** Resultado único para evitar consultas adicionales por cada fila. */
    public record ResultadoConsulta(
            List<DetalleBoleto> boletos,
            Map<Integer, EstadoAcceso> accesos) { }
}
