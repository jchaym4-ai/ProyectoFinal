
package servicio;

import dao.BoletoDAO;
import modelo.Boleto;

import java.nio.file.Path;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class PublicacionBoletosService {

    private final BoletoDAO boletoDAO;
    private final BoletoPDFService pdfService;
    private final CloudflareBoletoService cloudflare;

    public PublicacionBoletosService() throws Exception {

        boletoDAO = new BoletoDAO();

        pdfService = new BoletoPDFService();

        cloudflare = new CloudflareBoletoService();
    }

    public List<String> publicarBoletosVenta(int idVenta)
            throws Exception {

        if (idVenta <= 0) {
            throw new IllegalArgumentException(
                    "El ID de venta no es valido."
            );
        }

        List<Boleto> boletos =
                boletoDAO.listarPorVenta(idVenta);

        if (boletos.isEmpty()) {
            throw new SQLException(
                    "No se encontraron boletos para la venta."
            );
        }

        List<String> enlaces = new ArrayList<>();

        for (Boleto boleto : boletos) {

            // Evitar generar un enlace nuevo
            // para boletos ya publicados.
            if (boleto.getRutaPdf() != null
                    && !boleto.getRutaPdf().isBlank()) {

                enlaces.add(boleto.getRutaPdf());
                continue;
            }

            // 1. Generar identificador individual.
            String identificador =
                    cloudflare.generarIdentificador();

            // 2. Preparar enlace HTTPS.
            String url =
                    cloudflare.construirUrl(identificador);

            // 3. Generar PDF con el enlace en el QR.
            Path archivoPDF =
                    pdfService.generarPDF(
                            boleto.getIdBoleto(),
                            url
                    );

            // 4. Publicar archivo en Cloudflare.
            String urlPublicada =
                    cloudflare.subirPDF(
                            archivoPDF,
                            identificador
                    );

            // 5. Guardar enlace en Neon.
            boletoDAO.actualizarRutaPdf(
                    boleto.getIdBoleto(),
                    urlPublicada
            );

            enlaces.add(urlPublicada);

            System.out.println(
                    "Boleto #" + boleto.getIdBoleto()
                    + " publicado: " + urlPublicada
            );
        }

        return enlaces;
    }
}
