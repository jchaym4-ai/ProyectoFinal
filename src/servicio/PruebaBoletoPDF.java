
package servicio;

import java.nio.file.Path;

public class PruebaBoletoPDF {

    public static void main(String[] args) {

        try {

            BoletoPDFService servicio =
                    new BoletoPDFService();

            Path archivo = servicio.generarPDF(24);

            System.out.println(
                    "PDF generado correctamente:"
            );

            System.out.println(
                    archivo.toAbsolutePath()
            );

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
