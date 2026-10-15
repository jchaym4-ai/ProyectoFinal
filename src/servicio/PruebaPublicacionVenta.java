
package servicio;

import dao.BoletoDAO;
import modelo.Boleto;
import java.util.List;

public class PruebaPublicacionVenta {

    public static void main(String[] args) {

        try {
            int idVenta = 5;

            BoletoDAO boletoDAO = new BoletoDAO();

            List<Boleto> boletos =
                    boletoDAO.listarPorVenta(idVenta);

            if (boletos.isEmpty()) {
                System.out.println(
                        "No existen boletos para la venta #" + idVenta
                );
                return;
            }

            System.out.println(
                    "Boletos encontrados: " + boletos.size()
            );

            PublicacionBoletosService publicacion =
                    new PublicacionBoletosService();

            List<String> enlaces =
                    publicacion.publicarBoletosVenta(idVenta);

            System.out.println();
            System.out.println("PUBLICACION FINALIZADA");

            for (String enlace : enlaces) {
                System.out.println(enlace);
            }

            System.out.println();
            System.out.println(
                    "Enlaces guardados en Neon."
            );

        } catch (Exception e) {
            System.err.println(
                    "Error durante la publicacion:"
            );
            e.printStackTrace();
        }
    }
}
