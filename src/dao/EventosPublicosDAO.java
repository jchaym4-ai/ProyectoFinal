package dao;

import coneccion.CreateConnection;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** Consultas exclusivamente de lectura de la cartelera y su disponibilidad. */
public class EventosPublicosDAO {
    private final CreateConnection factory = new CreateConnection();

    public record Evento(int id, String titulo, String artista, LocalDateTime fecha,
                         String recinto, byte[] banner) { }

    public record Localidad(int idInventario, String nombre, String tipoVenta,
                           BigDecimal precio, int disponibles, int capacidad) { }

    public record Asiento(String fila, int numero, String estado) { }

    public List<Evento> listarEventos() throws SQLException {
        String sql = "SELECT c.id_concierto, c.titulo_evento, a.nombre_artistico AS artista, "
                + "c.fecha_concierto, c.recinto, cb.imagen_jpg "
                + "FROM conciertos c "
                + "LEFT JOIN artistas a ON a.id_artista = c.id_artista "
                + "LEFT JOIN concierto_banners cb ON cb.id_concierto = c.id_concierto "
                + "WHERE LOWER(TRIM(c.estado)) IN ('programado', 'activo') "
                + "ORDER BY c.fecha_concierto, c.id_concierto";
        List<Evento> resultado = new ArrayList<>();
        try (Connection cn = factory.getConection();
             PreparedStatement ps = cn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Timestamp fecha = rs.getTimestamp("fecha_concierto");
                resultado.add(new Evento(
                        rs.getInt("id_concierto"),
                        rs.getString("titulo_evento"),
                        rs.getString("artista"),
                        fecha == null ? null : fecha.toLocalDateTime(),
                        rs.getString("recinto"),
                        rs.getBytes("imagen_jpg")));
            }
        }
        return resultado;
    }

    public List<Localidad> listarLocalidades(int idConcierto) throws SQLException {
        String sql = "SELECT i.id_inventario, l.nombre, i.tipo_venta, i.precio, "
                + "i.cantidad_disponible, i.cantidad_total "
                + "FROM inventario_boletos i "
                + "JOIN localidades l ON l.id_localidad = i.id_localidad "
                + "WHERE i.id_concierto = ? "
                + "ORDER BY i.precio DESC, l.nombre";
        List<Localidad> resultado = new ArrayList<>();
        try (Connection cn = factory.getConection();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setInt(1, idConcierto);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    resultado.add(new Localidad(
                            rs.getInt("id_inventario"), rs.getString("nombre"),
                            rs.getString("tipo_venta"), rs.getBigDecimal("precio"),
                            rs.getInt("cantidad_disponible"), rs.getInt("cantidad_total")));
                }
            }
        }
        return resultado;
    }

    public List<Asiento> listarAsientos(int idInventario) throws SQLException {
        String sql = "SELECT a.fila, a.numero, a.estado "
                + "FROM asientos a "
                + "JOIN inventario_boletos i ON i.id_inventario = a.id_inventario "
                + "WHERE a.id_inventario = ? AND i.tipo_venta = 'numerado' "
                + "ORDER BY a.fila, a.numero";
        List<Asiento> resultado = new ArrayList<>();
        try (Connection cn = factory.getConection();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setInt(1, idInventario);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    resultado.add(new Asiento(rs.getString("fila"),
                            rs.getInt("numero"), rs.getString("estado")));
                }
            }
        }
        return resultado;
    }
}
