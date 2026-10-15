
package servicio;

import java.nio.file.Path;

public class PruebaBoletoCompleto {

    public static void main(String[] args) {

        try {

            // Boleto existente en Neon para realizar la prueba
            int idBoleto = 24;

            CloudflareBoletoService cloudflare =
                    new CloudflareBoletoService();

            BoletoPDFService pdfService =
                    new BoletoPDFService();

            // 1. Generar identificador individual
            String identificador =
                    cloudflare.generarIdentificador();

            // 2. Preparar URL definitiva
            String url =
                    cloudflare.construirUrl(identificador);

            // 3. Generar PDF con esa URL dentro del QR
            Path archivoPDF =
                    pdfService.generarPDF(idBoleto, url);

            System.out.println(
                    "PDF generado: " + archivoPDF
            );

            // 4. Subir PDF a Cloudflare
            String urlPublicada =
                    cloudflare.subirPDF(
                            archivoPDF,
                            identificador
                    );

            System.out.println(
                    "PDF publicado correctamente."
            );

            System.out.println(
                    "URL: " + urlPublicada
            );

            System.out.println(
                    "Escanea el QR del PDF para probarlo."
            );

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
