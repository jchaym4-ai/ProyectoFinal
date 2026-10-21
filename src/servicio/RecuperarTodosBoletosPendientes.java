
package servicio;

import coneccion.CreateConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class RecuperarTodosBoletosPendientes {

    public static void main(String[] args) {

        List<Integer> ventas = new ArrayList<>();

        String sql = """
                SELECT DISTINCT d.id_venta
                FROM boletos b
                INNER JOIN detalle_ventas d
                    ON b.id_detalle = d.id_detalle
                WHERE b.ruta_pdf IS NULL
                   OR TRIM(b.ruta_pdf) = ''
                ORDER BY d.id_venta
                """;

        try {

            CreateConnection conexion =
                    new CreateConnection();

            try (Connection conn =
                    conexion.getConection()) {

                if (conn == null) {
                    throw new IllegalStateException(
                            "No se pudo conectar a Neon."
                    );
                }

                try (PreparedStatement ps =
                        conn.prepareStatement(sql);
                     ResultSet rs = ps.executeQuery()) {

                    while (rs.next()) {
                        ventas.add(
                                rs.getInt("id_venta")
                        );
                    }
                }
            }

            if (ventas.isEmpty()) {
                System.out.println(
                        "No existen boletos pendientes."
                );
                return;
            }

            System.out.println(
                    "Ventas pendientes: " + ventas.size()
            );

            PublicacionBoletosService publicacion =
                    new PublicacionBoletosService();

            int correctas = 0;
            int errores = 0;

            for (Integer idVenta : ventas) {

                try {

                    System.out.println(
                            "\nProcesando venta #" + idVenta
                    );

                    publicacion.publicarBoletosVenta(
                            idVenta
                    );

                    correctas++;

                    System.out.println(
                            "Venta #" + idVenta
                            + " procesada correctamente."
                    );

                } catch (Exception e) {

                    errores++;

                    System.err.println(
                            "Error en venta #" + idVenta
                            + ": " + e.getMessage()
                    );
                }
            }

            System.out.println(
                    "\nRECUPERACION FINALIZADA"
            );

            System.out.println(
                    "Ventas procesadas: " + correctas
            );

            System.out.println(
                    "Ventas con errores: " + errores
            );

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
