package servicio;

import dao.ReportesDAO;
import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.Normalizer;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;

/** Exporta el reporte administrativo, sin modificar información en Neon. */
public final class ReportesPDFService {
    private static final PDType1Font REGULAR =
            new PDType1Font(Standard14Fonts.FontName.HELVETICA);
    private static final PDType1Font NEGRITA =
            new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
    private static final float MARGEN = 39;
    private static final float ANCHO = PDRectangle.A4.getWidth() - MARGEN * 2;
    private static final DecimalFormat MONEDA = formatoMoneda();

    private static DecimalFormat formatoMoneda() {
        DecimalFormatSymbols simbolos = DecimalFormatSymbols.getInstance(Locale.US);
        return new DecimalFormat("#,##0.00", simbolos);
    }

    public void exportar(File archivo, String filtro, ReportesDAO.Resumen resumen,
            List<ReportesDAO.Concierto> conciertos,
            List<ReportesDAO.Localidad> localidades) throws IOException {
        if (archivo == null || resumen == null) {
            throw new IllegalArgumentException("Faltan datos para exportar.");
        }
        try (PDDocument documento = new PDDocument()) {
            Escritor p = new Escritor(documento);
            try {
                p.titulo("SVB-GUA  /  REPORTE ADMINISTRATIVO");
                String fecha = LocalDateTime.now(ZoneId.of("America/Guatemala"))
                        .format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
                p.texto("Generado: " + fecha + " (Guatemala)");
                p.texto("Concierto: " + filtro);
                p.espacio(12);
                p.subtitulo("RESUMEN GENERAL");
                p.par("Ventas con boletos", Long.toString(resumen.ventas()));
                p.par("Conciertos", Long.toString(resumen.conciertos()));
                p.par("Boletos emitidos (historico)", Long.toString(resumen.emitidos()));
                p.par("Ingresos autorizados mediante QR", Long.toString(resumen.ingresos()));
                p.par("Boletos sin utilizar", Long.toString(resumen.sinUtilizar()));
                p.par("Boletos cancelados", Long.toString(resumen.cancelados()));
                p.par("Aforo disponible", Long.toString(resumen.disponibles()));
                p.par("Ingresos historicos", q(resumen.historicos()));
                p.par("Devoluciones simuladas", q(resumen.devoluciones()));
                p.par("Ingresos netos estimados", q(resumen.netos()));
                p.espacio(14);
                p.subtitulo("RESULTADO POR CONCIERTO");
                if (conciertos.isEmpty()) p.texto("No se encontraron conciertos.");
                for (ReportesDAO.Concierto c : conciertos) {
                    p.linea();
                    p.textoNegrita(c.nombre());
                    p.texto("Boleto(s): " + c.emitidos()
                            + "   Accesos QR: " + c.ingresos()
                            + "   Cancelados: " + c.cancelados()
                            + "   Disponibles: " + c.disponibles());
                    p.texto("Historicos: " + q(c.historicos())
                            + "   Devoluciones: " + q(c.devoluciones())
                            + "   Netos: " + q(c.netos()));
                }
                p.espacio(14);
                p.subtitulo("DETALLE POR LOCALIDAD");
                if (localidades.isEmpty()) p.texto("No hay localidades para el filtro.");
                for (ReportesDAO.Localidad l : localidades) {
                    p.linea();
                    p.textoNegrita(l.concierto() + "  /  " + l.nombre());
                    p.texto("Capacidad: " + l.capacidad()
                            + "   Disponibles: " + l.disponibles()
                            + "   Emitidos: " + l.emitidos()
                            + "   Ingresaron: " + l.ingresos()
                            + "   Cancelados: " + l.cancelados());
                    p.texto("Ingreso neto estimado: " + q(l.netos()));
                }
                p.espacio(18);
                p.texto("Reporte academico SVB-GUA. Los reintegros son simulados.");
                p.texto("No es factura SAT ni comprobante de devolucion bancaria.");
            } finally {
                p.cerrar();
            }
            documento.save(archivo);
        }
    }

    private static String q(BigDecimal valor) {
        return "Q " + MONEDA.format(valor == null ? BigDecimal.ZERO : valor);
    }

    private static String limpiar(String texto) {
        if (texto == null) return "";
        String simple = Normalizer.normalize(texto, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "");
        return simple.replaceAll("[^\\x20-\\x7E]", "?");
    }

    private static final class Escritor {
        private final PDDocument documento;
        private PDPageContentStream contenido;
        private float y;
        private int pagina;

        Escritor(PDDocument documento) throws IOException {
            this.documento = documento;
            nuevaPagina();
        }

        private void nuevaPagina() throws IOException {
            if (contenido != null) contenido.close();
            PDPage hoja = new PDPage(PDRectangle.A4);
            documento.addPage(hoja);
            contenido = new PDPageContentStream(documento, hoja);
            y = PDRectangle.A4.getHeight() - MARGEN;
            pagina++;
            contenido.setStrokingColor(new Color(218, 225, 235));
            contenido.moveTo(MARGEN, 37);
            contenido.lineTo(MARGEN + ANCHO, 37);
            contenido.stroke();
            contenido.beginText();
            contenido.setFont(REGULAR, 8);
            contenido.newLineAtOffset(MARGEN, 25);
            contenido.showText("SVB-GUA | Reporte de gestion | Pagina " + pagina);
            contenido.endText();
        }

        private void verificar(float necesario) throws IOException {
            if (y - necesario < 56) nuevaPagina();
        }

        private void escribir(String texto, float tamano,
                boolean negrita, float alturaY) throws IOException {
            String limpio = limpiar(texto);
            PDType1Font fuente = negrita ? NEGRITA : REGULAR;
            // Cada renglon se ajusta al ancho de la hoja para evitar recortes.
            String restante = limpio;
            if (restante.isEmpty()) restante = " ";
            while (!restante.isEmpty()) {
                verificar(alturaY);
                int limite = restante.length();
                while (limite > 1
                        && fuente.getStringWidth(restante.substring(0, limite))
                        / 1000f * tamano > ANCHO) {
                    limite--;
                }
                String linea = restante.substring(0, limite);
                if (limite < restante.length()) {
                    int espacio = linea.lastIndexOf(' ');
                    if (espacio > 2) limite = espacio + 1;
                    linea = restante.substring(0, limite).trim();
                }
                contenido.beginText();
                contenido.setFont(fuente, tamano);
                contenido.newLineAtOffset(MARGEN, y);
                contenido.showText(linea.isEmpty() ? " " : linea);
                contenido.endText();
                y -= alturaY;
                restante = restante.substring(limite).stripLeading();
            }
        }

        void titulo(String texto) throws IOException { escribir(texto, 17, true, 26); }
        void subtitulo(String texto) throws IOException { escribir(texto, 11, true, 19); }
        void texto(String texto) throws IOException { escribir(texto, 9, false, 13); }
        void textoNegrita(String texto) throws IOException { escribir(texto, 9.5f, true, 15); }
        void par(String etiqueta, String valor) throws IOException {
            escribir(etiqueta + ":  " + valor, 9.5f, false, 16);
        }
        void espacio(float altura) throws IOException { verificar(altura); y -= altura; }
        void linea() throws IOException {
            verificar(12);
            y -= 4;
            contenido.moveTo(MARGEN, y);
            contenido.lineTo(MARGEN + ANCHO, y);
            contenido.stroke();
            y -= 8;
        }
        void cerrar() throws IOException { if (contenido != null) contenido.close(); }
    }
}
