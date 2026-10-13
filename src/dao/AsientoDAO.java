
package dao;

import coneccion.CreateConnection;
import modelo.Asiento;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class AsientoDAO {

    private final CreateConnection connFactory;

    public AsientoDAO() {
        this.connFactory = new CreateConnection();
    }

    public List<Asiento> listarPorInventario(
            int idInventario) throws SQLException {

        List<Asiento> lista = new ArrayList<>();

        String sql = """
                SELECT
                    id_asiento,
                    id_inventario,
                    fila,
                    numero,
                    estado
                FROM asientos
                WHERE id_inventario = ?
                ORDER BY fila, numero
                """;

        try (Connection conn = connFactory.getConection()) {

            if (conn == null) {
                throw new SQLException(
                        "No se pudo conectar a PostgreSQL."
                );
            }

            try (PreparedStatement ps =
                    conn.prepareStatement(sql)) {

                ps.setInt(1, idInventario);

                try (ResultSet rs = ps.executeQuery()) {

                    while (rs.next()) {

                        Asiento asiento = new Asiento();

                        asiento.setIdAsiento(
                                rs.getInt("id_asiento")
                        );

                        asiento.setIdInventario(
                                rs.getInt("id_inventario")
                        );

                        asiento.setFila(
                                rs.getString("fila")
                        );

                        asiento.setNumero(
                                rs.getInt("numero")
                        );

                        asiento.setEstado(
                                rs.getString("estado")
                        );

                        lista.add(asiento);
                    }
                }
            }
        }

        return lista;
    }

    public List<Asiento> listarPorConcierto(
            int idConcierto) throws SQLException {

        List<Asiento> lista = new ArrayList<>();

        String sql = """
                SELECT
                    a.id_asiento,
                    a.id_inventario,
                    a.fila,
                    a.numero,
                    a.estado
                FROM asientos a
                INNER JOIN inventario_boletos i
                    ON a.id_inventario = i.id_inventario
                WHERE i.id_concierto = ?
                AND i.tipo_venta = 'numerado'
                ORDER BY a.id_inventario, a.fila, a.numero
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

                    while (rs.next()) {

                        Asiento asiento = new Asiento();

                        asiento.setIdAsiento(
                                rs.getInt("id_asiento")
                        );

                        asiento.setIdInventario(
                                rs.getInt("id_inventario")
                        );

                        asiento.setFila(
                                rs.getString("fila")
                        );

                        asiento.setNumero(
                                rs.getInt("numero")
                        );

                        asiento.setEstado(
                                rs.getString("estado")
                        );

                        lista.add(asiento);
                    }
                }
            }
        }

        return lista;
    }
}
