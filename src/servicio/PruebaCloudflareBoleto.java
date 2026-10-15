
package servicio;

import java.nio.file.Files;
import java.nio.file.Path;

public class PruebaCloudflareBoleto {

    public static void main(String[] args) {

        try {
            CloudflareBoletoService servicio =
                    new CloudflareBoletoService();

            Path archivo = Path.of(
                    System.getProperty("user.home"),
                    "SVB-GUA",
                    "boletos",
                    "prueba.pdf"
            );

            if (!Files.isRegularFile(archivo)) {
                throw new IllegalStateException(
                        "No existe el PDF: " + archivo
                );
            }

            String identificador =
                    servicio.generarIdentificador();

            String url = servicio.subirPDF(
                    archivo,
                    identificador
            );

            System.out.println(
                    "PDF publicado correctamente."
            );

            System.out.println(
                    "URL: " + url
            );

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
