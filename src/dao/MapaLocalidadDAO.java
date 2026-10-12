
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

    public MapaLocalidadDAO() {
        this.connFactory = new CreateConnection();
    }

    public List<MapaLocalidad> listarPorConcierto(
            int idConcierto) throws SQLException {

        List<MapaLocalidad> lista = new ArrayList<>();

        String sql = """
                SELECT
                    m.id_mapa,
                    m.id_inventario,
                    m.pos_x,
                    m.pos_y,
                    m.ancho,
                    m.alto,
                    m.color,
                    l.nombre AS nombre_localidad,
                    i.precio,
                    i.cantidad_disponible,
                    i.tipo_venta
                FROM mapa_localidades m
                INNER JOIN inventario_boletos i
                    ON m.id_inventario = i.id_inventario
                INNER JOIN localidades l
                    ON i.id_localidad = l.id_localidad
                WHERE i.id_concierto = ?
                ORDER BY m.pos_y, m.pos_x
                """;

        try (
            Connection conn = connFactory.getConection()
        ) {

            if (conn == null) {
                throw new SQLException(
                        "No se pudo conectar a PostgreSQL."
                );
            }

            try (
                PreparedStatement ps =
                        conn.prepareStatement(sql)
            ) {

                ps.setInt(1, idConcierto);

                try (ResultSet rs = ps.executeQuery()) {

                    while (rs.next()) {

                        MapaLocalidad mapa =
                                new MapaLocalidad();

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

                        mapa.setColor(
                                rs.getString("color")
                        );

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
                    }
                }
            }
        }

        return lista;
    }
}
