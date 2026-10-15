
package dao;

import coneccion.CreateConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import modelo.Boleto;
import modelo.DetalleBoleto;

public class BoletoDAO {

    private final CreateConnection connFactory;

    public BoletoDAO() {
        this.connFactory = new CreateConnection();
    }

    public List<Boleto> listarPorVenta(int idVenta)
            throws SQLException {

        List<Boleto> boletos = new ArrayList<>();

        String sql = """
                SELECT
                    b.id_boleto,
                    b.id_detalle,
                    b.id_asiento,
                    b.codigo_boleto,
                    b.fecha_emision,
                    b.estado,
                    b.ruta_pdf
                FROM boletos b
                INNER JOIN detalle_ventas d
                    ON b.id_detalle = d.id_detalle
                WHERE d.id_venta = ?
                ORDER BY b.id_boleto
                """;

        try (Connection conn = connFactory.getConection()) {

            if (conn == null) {
                throw new SQLException(
                        "No se pudo conectar a Neon PostgreSQL."
                );
            }

            try (PreparedStatement ps =
                    conn.prepareStatement(sql)) {

                ps.setInt(1, idVenta);

                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        boletos.add(mapearBoleto(rs));
                    }
                }
            }
        }

        return boletos;
    }

    public Boleto buscarPorCodigo(String codigo)
            throws SQLException {

        if (codigo == null || codigo.isBlank()) {
            throw new IllegalArgumentException(
                    "Debe ingresar un código de boleto."
            );
        }

        String sql = """
                SELECT
                    id_boleto,
                    id_detalle,
                    id_asiento,
                    codigo_boleto,
                    fecha_emision,
                    estado,
                    ruta_pdf
                FROM boletos
                WHERE codigo_boleto = ?
                """;

        try (Connection conn = connFactory.getConection()) {

            if (conn == null) {
                throw new SQLException(
                        "No se pudo conectar a Neon PostgreSQL."
                );
            }

            try (PreparedStatement ps =
                    conn.prepareStatement(sql)) {

                ps.setString(1, codigo.trim());

                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return mapearBoleto(rs);
                    }
                }
            }
        }

        return null;
    }

    public void actualizarRutaPdf(
            int idBoleto,
            String rutaPdf) throws SQLException {

        if (idBoleto <= 0 || rutaPdf == null
                || rutaPdf.isBlank()) {

            throw new IllegalArgumentException(
                    "El boleto y la ruta PDF deben ser válidos."
            );
        }

        String sql = """
                UPDATE boletos
                SET ruta_pdf = ?
                WHERE id_boleto = ?
                """;

        try (Connection conn = connFactory.getConection()) {

            if (conn == null) {
                throw new SQLException(
                        "No se pudo conectar a Neon PostgreSQL."
                );
            }

            try (PreparedStatement ps =
                    conn.prepareStatement(sql)) {

                ps.setString(1, rutaPdf);
                ps.setInt(2, idBoleto);

                if (ps.executeUpdate() != 1) {
                    throw new SQLException(
                            "No existe el boleto indicado."
                    );
                }
            }
        }
    }

    private Boleto mapearBoleto(ResultSet rs)
            throws SQLException {

        Boleto boleto = new Boleto();

        boleto.setIdBoleto(rs.getInt("id_boleto"));
        boleto.setIdDetalle(rs.getInt("id_detalle"));

        int idAsiento = rs.getInt("id_asiento");

        boleto.setIdAsiento(
                rs.wasNull() ? null : idAsiento
        );

        boleto.setCodigoBoleto(
                rs.getString("codigo_boleto")
        );

        Timestamp fecha = rs.getTimestamp("fecha_emision");

        boleto.setFechaEmision(
                fecha == null ? null : fecha.toLocalDateTime()
        );

        boleto.setEstado(rs.getString("estado"));
        boleto.setRutaPdf(rs.getString("ruta_pdf"));

        return boleto;
    }

    public DetalleBoleto buscarDetallePorId(int idBoleto)
            throws SQLException {

        if (idBoleto <= 0) {
            throw new IllegalArgumentException(
                    "El ID del boleto debe ser válido."
            );
        }

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
                    a.numero AS numero_asiento
                FROM boletos b
                INNER JOIN detalle_ventas d
                    ON b.id_detalle = d.id_detalle
                INNER JOIN ventas v
                    ON d.id_venta = v.id_venta
                INNER JOIN inventario_boletos i
                    ON d.id_inventario = i.id_inventario
                INNER JOIN conciertos c
                    ON i.id_concierto = c.id_concierto
                INNER JOIN artistas ar
                    ON c.id_artista = ar.id_artista
                INNER JOIN localidades l
                    ON i.id_localidad = l.id_localidad
                LEFT JOIN asientos a
                    ON b.id_asiento = a.id_asiento
                WHERE b.id_boleto = ?
                """;

        try (Connection conn = connFactory.getConection()) {

            if (conn == null) {
                throw new SQLException(
                        "No se pudo conectar a Neon PostgreSQL."
                );
            }

            try (PreparedStatement ps =
                    conn.prepareStatement(sql)) {

                ps.setInt(1, idBoleto);

                try (ResultSet rs = ps.executeQuery()) {

                    if (!rs.next()) {
                        return null;
                    }

                    DetalleBoleto detalle = new DetalleBoleto();

                    detalle.setIdBoleto(
                            rs.getInt("id_boleto")
                    );

                    detalle.setIdVenta(
                            rs.getInt("id_venta")
                    );

                    detalle.setCodigoBoleto(
                            rs.getString("codigo_boleto")
                    );

                    detalle.setEstado(
                            rs.getString("estado")
                    );

                    Timestamp fechaEmision =
                            rs.getTimestamp("fecha_emision");

                    if (fechaEmision != null) {
                        detalle.setFechaEmision(
                                fechaEmision.toLocalDateTime()
                        );
                    }

                    detalle.setRutaPdf(
                            rs.getString("ruta_pdf")
                    );

                    detalle.setTituloEvento(
                            rs.getString("titulo_evento")
                    );

                    Timestamp fechaConcierto =
                            rs.getTimestamp("fecha_concierto");

                    if (fechaConcierto != null) {
                        detalle.setFechaConcierto(
                                fechaConcierto.toLocalDateTime()
                        );
                    }

                    detalle.setRecinto(
                            rs.getString("recinto")
                    );

                    detalle.setNombreArtista(
                            rs.getString("nombre_artista")
                    );

                    detalle.setLocalidad(
                            rs.getString("localidad")
                    );

                    detalle.setTipoVenta(
                            rs.getString("tipo_venta")
                    );

                    detalle.setPrecio(
                            rs.getBigDecimal("precio_unitario")
                    );

                    detalle.setFila(
                            rs.getString("fila")
                    );

                    int numeroAsiento =
                            rs.getInt("numero_asiento");

                    detalle.setNumeroAsiento(
                            rs.wasNull() ? null : numeroAsiento
                    );

                    return detalle;
                }
            }
        }
    }
    
}
