
package servicio;

import dao.BoletoDAO;
import modelo.DetalleBoleto;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.text.Normalizer;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;

import com.google.zxing.WriterException;

public class BoletoPDFService {

    private final BoletoDAO boletoDAO = new BoletoDAO();

    private final BoletoQRService boletoQRService =
            new BoletoQRService();

    private static final PDFont NORMAL =
            new PDType1Font(
                    Standard14Fonts.FontName.HELVETICA
            );

    private static final PDFont NEGRITA =
            new PDType1Font(
                    Standard14Fonts.FontName.HELVETICA_BOLD
            );

    private static final Color AZUL =
            new Color(15, 23, 42);

    private static final Color GRIS =
            new Color(100, 116, 139);

    private static final Color AZUL_CLARO =
            new Color(37, 99, 235);

    private static final DateTimeFormatter FORMATO_FECHA =
            DateTimeFormatter.ofPattern(
                    "dd/MM/yyyy HH:mm"
            );

    // Compatibilidad con las llamadas anteriores.
    // Si no se indica una URL, el QR usa el codigo del boleto.
    public Path generarPDF(int idBoleto)
            throws SQLException, IOException {

        DetalleBoleto boleto =
                boletoDAO.buscarDetallePorId(idBoleto);

        if (boleto == null) {
            throw new SQLException(
                    "No existe el boleto indicado."
            );
        }

        return generarPDF(
                idBoleto,
                boleto.getCodigoBoleto()
        );
    }

    // Nuevo metodo: permite insertar la URL individual en el QR.
    public Path generarPDF(int idBoleto, String enlaceQR)
            throws SQLException, IOException {

        if (enlaceQR == null || enlaceQR.isBlank()) {
            throw new IllegalArgumentException(
                    "El contenido del QR no puede estar vacio."
            );
        }

        DetalleBoleto boleto =
                boletoDAO.buscarDetallePorId(idBoleto);

        if (boleto == null) {
            throw new SQLException(
                    "No existe el boleto indicado."
            );
        }

        if (boleto.getCodigoBoleto() == null
                || boleto.getCodigoBoleto().isBlank()) {

            throw new SQLException(
                    "El boleto no tiene codigo."
            );
        }

        Path carpeta = Path.of(
                System.getProperty("user.home"),
                "SVB-GUA",
                "boletos"
        );

        Files.createDirectories(carpeta);

        String archivo = "boleto-"
                + boleto.getIdBoleto()
                + ".pdf";

        Path destino = carpeta.resolve(archivo);

        BufferedImage imagenQR;

        try {

            imagenQR = boletoQRService.generarQR(
                    enlaceQR,
                    500
            );

        } catch (WriterException e) {

            throw new IOException(
                    "No se pudo generar el codigo QR.",
                    e
            );
        }

        try (PDDocument documento = new PDDocument()) {

            PDPage pagina = new PDPage(
                    PDRectangle.A4
            );

            documento.addPage(pagina);

            try (PDPageContentStream contenido =
                    new PDPageContentStream(
                            documento,
                            pagina
                    )) {

                dibujarBoleto(
                        contenido,
                        boleto
                );

                PDImageXObject imagenPDF =
                        LosslessFactory.createFromImage(
                                documento,
                                imagenQR
                        );

                contenido.drawImage(
                        imagenPDF,
                        420,
                        110,
                        125,
                        125
                );
            }

            documento.save(destino.toFile());
        }

        return destino;
    }

    private void dibujarBoleto(
            PDPageContentStream contenido,
            DetalleBoleto boleto) throws IOException {

        contenido.setNonStrokingColor(AZUL);
        contenido.addRect(0, 690, 595, 152);
        contenido.fill();

        escribir(
                contenido,
                "SVB-GUA",
                42, 785,
                29,
                NEGRITA,
                Color.WHITE,
                500
        );

        escribir(
                contenido,
                "SISTEMA DE VENTA DE BOLETOS",
                43, 756,
                11,
                NORMAL,
                new Color(203, 213, 225),
                500
        );

        escribir(
                contenido,
                "BOLETO ELECTRONICO",
                43, 714,
                13,
                NEGRITA,
                Color.WHITE,
                500
        );

        escribir(
                contenido,
                "EVENTO",
                43, 655,
                10,
                NEGRITA,
                GRIS,
                500
        );

        escribir(
                contenido,
                boleto.getTituloEvento(),
                43, 624,
                21,
                NEGRITA,
                AZUL,
                505
        );

        escribir(
                contenido,
                "ARTISTA",
                43, 589,
                10,
                NEGRITA,
                GRIS,
                500
        );

        escribir(
                contenido,
                boleto.getNombreArtista(),
                43, 568,
                14,
                NORMAL,
                AZUL,
                500
        );

        escribirCampo(
                contenido,
                "FECHA DEL CONCIERTO",
                fechaTexto(boleto),
                43, 520
        );

        escribirCampo(
                contenido,
                "RECINTO",
                boleto.getRecinto(),
                310, 520
        );

        escribirCampo(
                contenido,
                "LOCALIDAD",
                boleto.getLocalidad(),
                43, 447
        );

        escribirCampo(
                contenido,
                "PRECIO",
                precioTexto(boleto.getPrecio()),
                310, 447
        );

        boolean numerado =
                "numerado".equalsIgnoreCase(
                        boleto.getTipoVenta()
                );

        if (numerado) {

            escribirCampo(
                    contenido,
                    "FILA",
                    boleto.getFila(),
                    43, 374
            );

            String asiento =
                    boleto.getNumeroAsiento() == null
                    ? "No asignado"
                    : String.valueOf(
                            boleto.getNumeroAsiento()
                    );

            escribirCampo(
                    contenido,
                    "ASIENTO",
                    asiento,
                    310, 374
            );

        } else {

            escribirCampo(
                    contenido,
                    "TIPO DE VENTA",
                    "AFORO GENERAL",
                    43, 374
            );
        }

        contenido.setStrokingColor(
                new Color(203, 213, 225)
        );

        contenido.moveTo(43, 318);
        contenido.lineTo(552, 318);
        contenido.stroke();

        escribir(
                contenido,
                "CODIGO UNICO DEL BOLETO",
                43, 289,
                10,
                NEGRITA,
                GRIS,
                500
        );

        escribir(
                contenido,
                boleto.getCodigoBoleto(),
                43, 263,
                11,
                NEGRITA,
                AZUL,
                505
        );

        escribirCampo(
                contenido,
                "ESTADO",
                boleto.getEstado(),
                43, 218
        );

        escribirCampo(
                contenido,
                "VENTA",
                "#" + boleto.getIdVenta(),
                310, 218
        );

        contenido.setNonStrokingColor(AZUL_CLARO);
        contenido.addRect(0, 0, 595, 75);
        contenido.fill();

        escribir(
                contenido,
                "SVB-GUA | BOLETO INDIVIDUAL",
                43, 43,
                12,
                NEGRITA,
                Color.WHITE,
                500
        );

        escribir(
                contenido,
                "Conserve este documento para su consulta.",
                43, 24,
                9,
                NORMAL,
                Color.WHITE,
                500
        );
    }

    private void escribirCampo(
            PDPageContentStream contenido,
            String etiqueta,
            String valor,
            float x,
            float y) throws IOException {

        escribir(
                contenido,
                etiqueta,
                x, y,
                10,
                NEGRITA,
                GRIS,
                240
        );

        escribir(
                contenido,
                valor,
                x, y - 23,
                13,
                NEGRITA,
                AZUL,
                240
        );
    }

    private void escribir(
            PDPageContentStream contenido,
            String texto,
            float x,
            float y,
            int tamano,
            PDFont fuente,
            Color color,
            float anchoMaximo) throws IOException {

        String seguro = textoSeguro(texto);

        while (!seguro.isEmpty()
                && fuente.getStringWidth(seguro)
                * tamano / 1000f > anchoMaximo) {

            seguro = seguro.substring(
                    0,
                    seguro.length() - 1
            );
        }

        contenido.beginText();
        contenido.setFont(fuente, tamano);
        contenido.setNonStrokingColor(color);
        contenido.newLineAtOffset(x, y);
        contenido.showText(seguro);
        contenido.endText();
    }

    private String textoSeguro(String texto) {

        if (texto == null || texto.isBlank()) {
            return "No disponible";
        }

        String normalizado = Normalizer.normalize(
                texto,
                Normalizer.Form.NFD
        );

        normalizado = normalizado.replaceAll(
                "\\p{M}+",
                ""
        );

        return normalizado.replaceAll(
                "[^\\x20-\\x7E]",
                " "
        );
    }

    private String fechaTexto(DetalleBoleto boleto) {

        if (boleto.getFechaConcierto() == null) {
            return "Fecha no disponible";
        }

        return boleto.getFechaConcierto()
                .format(FORMATO_FECHA);
    }

    private String precioTexto(BigDecimal precio) {

        if (precio == null) {
            return "Q 0.00";
        }

        return String.format(
                Locale.US,
                "Q %,.2f",
                precio
        );
    }
}
