
package dao;

import coneccion.CreateConnection;
import modelo.MapaLocalidad;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class MapaLocalidadDAO {

    private final CreateConnection connFactory;

    private static final String[] COLORES = {
        "#8B5CF6",
        "#F59E0B",
        "#3B82F6",
        "#10B981",
        "#EC4899",
        "#06B6D4",
        "#EF4444",
        "#84CC16"
    };

    public MapaLocalidadDAO() {
        this.connFactory = new CreateConnection();
    }

    public List<MapaLocalidad> listarPorConcierto(
            int idConcierto) throws SQLException {

        List<MapaLocalidad> lista = new ArrayList<>();

        String sql = """
                SELECT
                    m.id_mapa,
                    i.id_inventario,
                    m.pos_x,
                    m.pos_y,
                    m.ancho,
                    m.alto,
                    m.color,
                    l.nombre AS nombre_localidad,
                    i.precio,
                    i.cantidad_disponible,
                    i.tipo_venta
                FROM inventario_boletos i
                INNER JOIN localidades l
                    ON i.id_localidad = l.id_localidad
                LEFT JOIN mapa_localidades m
                    ON i.id_inventario = m.id_inventario
                WHERE i.id_concierto = ?
                ORDER BY l.nombre, i.id_inventario
                """;

        try (Connection conn = connFactory.getConection()) {

            if (conn == null) {
                throw new SQLException(
                        "No se pudo conectar a PostgreSQL."
                );
            }

            try (PreparedStatement ps =
                    conn.prepareStatement(sql)) {

                ps.setInt(1, idConcierto);

                try (ResultSet rs = ps.executeQuery()) {

                    int indice = 0;

                    while (rs.next()) {

                        MapaLocalidad mapa = new MapaLocalidad();

                        mapa.setIdMapa(
                                rs.getInt("id_mapa")
                        );

                        mapa.setIdInventario(
                                rs.getInt("id_inventario")
                        );

                        mapa.setPosX(
                                rs.getInt("pos_x")
                        );

                        mapa.setPosY(
                                rs.getInt("pos_y")
                        );

                        mapa.setAncho(
                                rs.getInt("ancho")
                        );

                        mapa.setAlto(
                                rs.getInt("alto")
                        );

                        String color = rs.getString("color");

                        if (color == null
                                || !color.matches(
                                        "#[0-9a-fA-F]{6}")) {

                            color = COLORES[
                                    indice % COLORES.length
                            ];
                        }

                        mapa.setColor(color);

                        mapa.setNombreLocalidad(
                                rs.getString("nombre_localidad")
                        );

                        mapa.setPrecio(
                                rs.getBigDecimal("precio")
                        );

                        mapa.setCantidadDisponible(
                                rs.getInt("cantidad_disponible")
                        );

                        mapa.setTipoVenta(
                                rs.getString("tipo_venta")
                        );

                        lista.add(mapa);
                        indice++;
                    }
                }
            }
        }

        return lista;
    }
}
