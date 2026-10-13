
package servicio;

import coneccion.CreateConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class GeneradorAsientosService {

    private final CreateConnection conexionFactory;

    public GeneradorAsientosService() {
        this.conexionFactory = new CreateConnection();
    }

    public int generarAsientos(int idInventario, int asientosPorFila)
            throws SQLException {

        if (idInventario <= 0) {
            throw new IllegalArgumentException(
                    "Debe seleccionar un inventario válido."
            );
        }

        if (asientosPorFila <= 0) {
            throw new IllegalArgumentException(
                    "Los asientos por fila deben ser mayores que cero."
            );
        }

        try (Connection conexion = conexionFactory.getConection()) {

            if (conexion == null) {
                throw new SQLException(
                        "No se pudo establecer conexión con PostgreSQL."
                );
            }

            conexion.setAutoCommit(false);

            try {

                String sqlInventario = """
                        SELECT
                            cantidad_total,
                            cantidad_disponible,
                            tipo_venta
                        FROM inventario_boletos
                        WHERE id_inventario = ?
                        FOR UPDATE
                        """;

                int cantidadTotal;
                int cantidadDisponible;
                String tipoVenta;

                try (PreparedStatement ps =
                        conexion.prepareStatement(sqlInventario)) {

                    ps.setInt(1, idInventario);

                    try (ResultSet rs = ps.executeQuery()) {

                        if (!rs.next()) {
                            throw new SQLException(
                                    "El inventario seleccionado no existe."
                            );
                        }

                        cantidadTotal = rs.getInt("cantidad_total");
                        cantidadDisponible =
                                rs.getInt("cantidad_disponible");
                        tipoVenta = rs.getString("tipo_venta");
                    }
                }

                if (!"numerado".equalsIgnoreCase(tipoVenta)) {
                    throw new SQLException(
                            "Solo se pueden generar asientos "
                            + "para localidades de tipo numerado."
                    );
                }

                if (cantidadTotal <= 0) {
                    throw new SQLException(
                            "La capacidad del inventario debe "
                            + "ser mayor que cero."
                    );
                }

                if (cantidadDisponible != cantidadTotal) {
                    throw new SQLException(
                            "No se pueden generar asientos porque "
                            + "el inventario ya tiene disponibilidad reducida."
                    );
                }

                String sqlVerificar = """
                        SELECT COUNT(*)
                        FROM asientos
                        WHERE id_inventario = ?
                        """;

                try (PreparedStatement ps =
                        conexion.prepareStatement(sqlVerificar)) {

                    ps.setInt(1, idInventario);

                    try (ResultSet rs = ps.executeQuery()) {

                        rs.next();

                        if (rs.getInt(1) > 0) {
                            throw new SQLException(
                                    "Este inventario ya tiene asientos "
                                    + "generados. No se crearán duplicados."
                            );
                        }
                    }
                }

                String sqlInsertar = """
                        INSERT INTO asientos
                        (id_inventario, fila, numero, estado)
                        VALUES (?, ?, ?, 'disponible')
                        """;

                int generados = 0;

                try (PreparedStatement ps =
                        conexion.prepareStatement(sqlInsertar)) {

                    while (generados < cantidadTotal) {

                        int indiceFila = generados / asientosPorFila;
                        int numeroAsiento =
                                (generados % asientosPorFila) + 1;

                        String fila =
                                convertirIndiceAFila(indiceFila);

                        ps.setInt(1, idInventario);
                        ps.setString(2, fila);
                        ps.setInt(3, numeroAsiento);

                        ps.addBatch();
                        generados++;

                        if (generados % 500 == 0) {
                            ps.executeBatch();
                        }
                    }

                    ps.executeBatch();
                }

                conexion.commit();

                return generados;

            } catch (SQLException | RuntimeException e) {

                try {
                    conexion.rollback();
                } catch (SQLException rollbackError) {
                    e.addSuppressed(rollbackError);
                }

                throw e;
            }
        }
    }

    private String convertirIndiceAFila(int indice) {

        StringBuilder fila = new StringBuilder();

        int numero = indice + 1;

        while (numero > 0) {

            numero--;

            fila.insert(
                    0,
                    (char) ('A' + (numero % 26))
            );

            numero /= 26;
        }

        return fila.toString();
    }
}
