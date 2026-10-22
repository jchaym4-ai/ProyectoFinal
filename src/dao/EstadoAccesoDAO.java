package dao;

import coneccion.CreateConnection;
import modelo.EstadoAccesoBoleto;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Consultas de solo lectura para el estado de acceso de cada boleto. */
public class EstadoAccesoDAO {

    private static final String COLUMNAS = """
            SELECT id_boleto, codigo_boleto, id_venta, id_concierto,
                   concierto, localidad, fila, numero_asiento,
                   estado_boleto, estado_acceso, fecha_ingreso_gt
            FROM vw_estado_acceso_boletos
            """;

    private final CreateConnection conexion = new CreateConnection();

    public Optional<EstadoAccesoBoleto> buscarPorId(int idBoleto) throws SQLException {
        if (idBoleto <= 0) {
            throw new IllegalArgumentException("ID de boleto inválido.");
        }
        try (Connection conn = abrir();
             PreparedStatement ps = conn.prepareStatement(
                     COLUMNAS + " WHERE id_boleto = ?")) {
            ps.setInt(1, idBoleto);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapear(rs)) : Optional.empty();
            }
        }
    }

    public List<EstadoAccesoBoleto> listarPorVenta(int idVenta) throws SQLException {
        if (idVenta <= 0) {
            throw new IllegalArgumentException("ID de venta inválido.");
        }
        List<EstadoAccesoBoleto> resultado = new ArrayList<>();
        try (Connection conn = abrir();
             PreparedStatement ps = conn.prepareStatement(
                     COLUMNAS + " WHERE id_venta = ? ORDER BY id_boleto")) {
            ps.setInt(1, idVenta);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    resultado.add(mapear(rs));
                }
            }
        }
        return resultado;
    }

    private Connection abrir() throws SQLException {
        Connection conn = conexion.getConection();
        if (conn == null) {
            throw new SQLException("No se pudo conectar a Neon PostgreSQL.");
        }
        return conn;
    }

    private static EstadoAccesoBoleto mapear(ResultSet rs) throws SQLException {
        Timestamp ingreso = rs.getTimestamp("fecha_ingreso_gt");
        return new EstadoAccesoBoleto(
                rs.getInt("id_boleto"),
                rs.getString("codigo_boleto"),
                rs.getInt("id_venta"),
                rs.getInt("id_concierto"),
                rs.getString("concierto"),
                rs.getString("localidad"),
                rs.getString("fila"),
                rs.getString("numero_asiento"),
                rs.getString("estado_boleto"),
                rs.getString("estado_acceso"),
                ingreso == null ? null : ingreso.toLocalDateTime()
        );
    }
}
