package dao;

import coneccion.CreateConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/** Banners compartidos en Neon: exclusivamente imagen JPG, <= 450 KB. */
public class ConciertoBannerDAO {
    private final CreateConnection conexiones = new CreateConnection();

    public void guardar(int idConcierto, byte[] imagenJpg) throws SQLException {
        if (idConcierto <= 0 || imagenJpg == null || imagenJpg.length == 0
                || imagenJpg.length > 450000) {
            throw new IllegalArgumentException("Banner no válido o demasiado grande.");
        }
        String sql = "INSERT INTO concierto_banners (id_concierto, imagen_jpg) "
                + "VALUES (?, ?) ON CONFLICT (id_concierto) DO UPDATE SET "
                + "imagen_jpg = EXCLUDED.imagen_jpg, fecha_actualizacion = CURRENT_TIMESTAMP";
        try (Connection conn = conexiones.getConection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idConcierto);
            ps.setBytes(2, imagenJpg);
            ps.executeUpdate();
        }
    }

    public byte[] obtener(int idConcierto) throws SQLException {
        String sql = "SELECT imagen_jpg FROM concierto_banners WHERE id_concierto = ?";
        try (Connection conn = conexiones.getConection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idConcierto);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getBytes("imagen_jpg") : null;
            }
        }
    }

    public void quitar(int idConcierto) throws SQLException {
        String sql = "DELETE FROM concierto_banners WHERE id_concierto = ?";
        try (Connection conn = conexiones.getConection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idConcierto);
            ps.executeUpdate();
        }
    }
}
