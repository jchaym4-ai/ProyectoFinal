package servicio;

import coneccion.CreateConnection;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;



public class FacturaPDFService {

    private static final PDType1Font NORMAL =
            new PDType1Font(Standard14Fonts.FontName.HELVETICA);
    private static final PDType1Font NEGRITA =
            new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
    // IVA incluido en el precio final: no se suma al total de Neon.
    private static final BigDecimal FACTOR_IVA = new BigDecimal("1.12");
    private static final float IZQUIERDA = 46;
    private static final float DERECHA = 566;
    private static final DateTimeFormatter FECHA =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public void generarFactura(int idVenta, Path archivoDestino)
            throws SQLException, IOException {

        if (idVenta <= 0) {
            throw new IllegalArgumentException("El identificador de venta no es válido.");
        }
        if (archivoDestino == null) {
            throw new IllegalArgumentException("Debe indicar el archivo PDF de destino.");
        }

        Factura factura = leerFactura(idVenta);
        Path destino = archivoDestino.toAbsolutePath().normalize();
        if (destino.getParent() != null) {
            Files.createDirectories(destino.getParent());
        }

        try (PDDocument documento = new PDDocument()) {
            Pagina pagina = crearPagina(documento, factura, true);

            for (Linea linea : factura.lineas) {
                if (pagina.y < 132) {
                    cerrarPagina(pagina);
                    pagina = crearPagina(documento, factura, false);
                }
                dibujarLinea(pagina, linea);
            }

            // Reservar espacio para subtotal, IVA y total antes del pie.
            if (pagina.y < 174) {
                cerrarPagina(pagina);
                pagina = crearPagina(documento, factura, false);
            }

            BigDecimal total = factura.total.setScale(2, RoundingMode.HALF_UP);
            BigDecimal subtotalSinIva = total.divide(
                    FACTOR_IVA, 2, RoundingMode.HALF_UP);
            BigDecimal ivaIncluido = total.subtract(subtotalSinIva);

            pagina.y -= 13;
            lineaHorizontal(pagina.stream, pagina.y);
            pagina.y -= 22;
            escribir(pagina.stream, "SUBTOTAL SIN IVA", IZQUIERDA,
                    pagina.y, 10, NORMAL);
            escribirDerecha(pagina.stream, moneda(subtotalSinIva), DERECHA,
                    pagina.y, 10, NORMAL);
            pagina.y -= 21;
            escribir(pagina.stream, "IVA INCLUIDO (12%)", IZQUIERDA,
                    pagina.y, 10, NORMAL);
            escribirDerecha(pagina.stream, moneda(ivaIncluido), DERECHA,
                    pagina.y, 10, NORMAL);
            pagina.y -= 25;
            lineaHorizontal(pagina.stream, pagina.y + 10);
            escribir(pagina.stream, "TOTAL PAGADO", IZQUIERDA,
                    pagina.y - 5, 13, NEGRITA);
            escribirDerecha(pagina.stream, moneda(total), DERECHA,
                    pagina.y - 5, 15, NEGRITA);
            cerrarPagina(pagina);
            documento.save(destino.toFile());
        }
    }

    private Factura leerFactura(int idVenta) throws SQLException {
        String sqlVenta = """
                SELECT id_venta, id_vendedor, fecha_venta, total_venta,
                       nombre_comprador, nit_comprador, direccion_comprador
                FROM ventas WHERE id_venta = ?
                """;
        String sqlDetalle = """
                SELECT d.cantidad, d.precio_unitario, d.subtotal,
                       c.titulo_evento, l.nombre AS nombre_localidad
                FROM detalle_ventas d
                JOIN inventario_boletos i ON i.id_inventario = d.id_inventario
                JOIN conciertos c ON c.id_concierto = i.id_concierto
                JOIN localidades l ON l.id_localidad = i.id_localidad
                WHERE d.id_venta = ?
                ORDER BY d.id_detalle
                """;

        try (Connection conexion = new CreateConnection().getConection()) {
            if (conexion == null) {
                throw new SQLException("No se pudo conectar a Neon PostgreSQL.");
            }
            Factura factura;
            try (PreparedStatement ps = conexion.prepareStatement(sqlVenta)) {
                ps.setInt(1, idVenta);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        throw new SQLException("No existe la venta #" + idVenta);
                    }
                    Timestamp fecha = rs.getTimestamp("fecha_venta");
                    factura = new Factura(
                            rs.getInt("id_venta"), rs.getInt("id_vendedor"),
                            fecha == null ? "Sin fecha" : fecha.toLocalDateTime().format(FECHA),
                            rs.getBigDecimal("total_venta"),
                            valor(rs.getString("nombre_comprador"), "Consumidor Final"),
                            valor(rs.getString("nit_comprador"), "CF"),
                            valor(rs.getString("direccion_comprador"), "No indicada"));
                }
            }
            try (PreparedStatement ps = conexion.prepareStatement(sqlDetalle)) {
                ps.setInt(1, idVenta);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        factura.lineas.add(new Linea(
                                valor(rs.getString("titulo_evento"), "Concierto") +
                                " / " + valor(rs.getString("nombre_localidad"), "Localidad"),
                                rs.getInt("cantidad"),
                                rs.getBigDecimal("precio_unitario"),
                                rs.getBigDecimal("subtotal")));
                    }
                }
            }
            if (factura.lineas.isEmpty()) {
                throw new SQLException("La venta #" + idVenta + " no tiene detalle.");
            }
            return factura;
        }
    }

    private Pagina crearPagina(PDDocument documento, Factura f, boolean principal)
            throws IOException {
        PDPage hoja = new PDPage(PDRectangle.LETTER);
        documento.addPage(hoja);
        PDPageContentStream stream = new PDPageContentStream(documento, hoja);
        Pagina p = new Pagina(stream);
        escribir(stream, "SVB-GUA", IZQUIERDA, 740, 21, NEGRITA);
        escribir(stream, "SISTEMA DE VENTA DE BOLETOS", IZQUIERDA, 720, 9, NORMAL);
        escribirDerecha(stream, "FACTURA FICTICIA", DERECHA, 740, 13, NEGRITA);
        escribirDerecha(stream, "Uso académico - SIN VALIDEZ TRIBUTARIA", DERECHA,
                720, 8, NORMAL);
        lineaHorizontal(stream, 705);
        escribir(stream, "Venta No. " + String.format(Locale.ROOT, "%06d", f.id),
                IZQUIERDA, 682, 11, NEGRITA);
        escribirDerecha(stream, "Fecha: " + f.fecha, DERECHA, 682, 10, NORMAL);
        escribir(stream, "Comprador: " + recortar(f.nombre, 62), IZQUIERDA, 658, 10, NORMAL);
        escribir(stream, "NIT: " + recortar(f.nit, 35), IZQUIERDA, 640, 10, NORMAL);
        escribir(stream, "Dirección: " + recortar(f.direccion, 76), IZQUIERDA, 622, 10, NORMAL);
        escribir(stream, "Vendedor ID: " + f.vendedor, IZQUIERDA, 604, 10, NORMAL);
        lineaHorizontal(stream, 587);
        escribir(stream, "DESCRIPCIÓN", IZQUIERDA, 568, 9, NEGRITA);
        escribirDerecha(stream, "CANT.", 398, 568, 9, NEGRITA);
        escribirDerecha(stream, "PRECIO", 476, 568, 9, NEGRITA);
        escribirDerecha(stream, "SUBTOTAL", DERECHA, 568, 9, NEGRITA);
        lineaHorizontal(stream, 557);
        p.y = 540;
        return p;
    }

    private void dibujarLinea(Pagina p, Linea l) throws IOException {
        String descripcion = recortar(l.descripcion, 51);
        escribir(p.stream, descripcion, IZQUIERDA, p.y, 9, NORMAL);
        escribirDerecha(p.stream, String.valueOf(l.cantidad), 398, p.y, 9, NORMAL);
        escribirDerecha(p.stream, moneda(l.precio), 476, p.y, 9, NORMAL);
        escribirDerecha(p.stream, moneda(l.subtotal), DERECHA, p.y, 9, NORMAL);
        p.y -= 25;
    }

    private void cerrarPagina(Pagina p) throws IOException {
        lineaHorizontal(p.stream, 70);
        escribir(p.stream, "COMPROBANTE ACADÉMICO SIMULADO. NO ES FACTURA FEL.",
                IZQUIERDA, 54, 9, NEGRITA);
        escribir(p.stream, "Este documento no tiene validez ante la SAT de Guatemala.",
                IZQUIERDA, 39, 8, NORMAL);
        p.stream.close();
    }

    private static void lineaHorizontal(PDPageContentStream stream, float y)
            throws IOException {
        stream.setLineWidth(0.7f);
        stream.moveTo(IZQUIERDA, y);
        stream.lineTo(DERECHA, y);
        stream.stroke();
    }

    private static void escribir(PDPageContentStream stream, String texto,
            float x, float y, int tamano, PDType1Font fuente) throws IOException {
        stream.beginText();
        stream.setFont(fuente, tamano);
        stream.newLineAtOffset(x, y);
        stream.showText(limpiarParaPdf(texto));
        stream.endText();
    }

    private static void escribirDerecha(PDPageContentStream stream, String texto,
            float bordeDerecho, float y, int tamano, PDType1Font fuente)
            throws IOException {
        String seguro = limpiarParaPdf(texto);
        float ancho = fuente.getStringWidth(seguro) * tamano / 1000f;
        escribir(stream, seguro, bordeDerecho - ancho, y, tamano, fuente);
    }

    private static String limpiarParaPdf(String entrada) {
        if (entrada == null) return "";
        StringBuilder salida = new StringBuilder();
        for (char c : entrada.toCharArray()) {
            // Las fuentes Standard 14 usan WinAnsi (no admiten emojis ni saltos).
            if (c == '\n' || c == '\r' || c == '\t') salida.append(' ');
            else if (c >= 32 && c <= 255 && c != 127) salida.append(c);
            else salida.append('?');
        }
        return salida.toString();
    }

    private static String valor(String texto, String predeterminado) {
        return texto == null || texto.isBlank() ? predeterminado : texto.trim();
    }

    private static String recortar(String texto, int maximo) {
        String t = texto == null ? "" : texto.replace('\n', ' ').replace('\r', ' ');
        return t.length() <= maximo ? t : t.substring(0, maximo - 3) + "...";
    }

    private static String moneda(BigDecimal cantidad) {
        BigDecimal valor = cantidad == null ? BigDecimal.ZERO : cantidad;
        return String.format(Locale.US, "Q %,.2f", valor.setScale(2, RoundingMode.HALF_UP));
    }

    private static final class Factura {
        final int id;
        final int vendedor;
        final String fecha;
        final BigDecimal total;
        final String nombre;
        final String nit;
        final String direccion;
        final List<Linea> lineas = new ArrayList<>();

        Factura(int id, int vendedor, String fecha, BigDecimal total,
                String nombre, String nit, String direccion) {
            this.id = id;
            this.vendedor = vendedor;
            this.fecha = fecha;
            this.total = total;
            this.nombre = nombre;
            this.nit = nit;
            this.direccion = direccion;
        }
    }

    private static final class Linea {
        final String descripcion;
        final int cantidad;
        final BigDecimal precio;
        final BigDecimal subtotal;

        Linea(String descripcion, int cantidad, BigDecimal precio, BigDecimal subtotal) {
            this.descripcion = descripcion;
            this.cantidad = cantidad;
            this.precio = precio;
            this.subtotal = subtotal;
        }
    }

    private static final class Pagina {
        final PDPageContentStream stream;
        float y;
        Pagina(PDPageContentStream stream) { this.stream = stream; }
    }
}
