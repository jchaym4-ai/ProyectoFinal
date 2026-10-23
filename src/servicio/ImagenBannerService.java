package servicio;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.MemoryCacheImageOutputStream;

/** Optimiza la foto para tarjetas de 16:9 y la convierte en JPEG ligero. */
public final class ImagenBannerService {
    private static final int MAX_BYTES = 450000;
    private static final long MAX_ARCHIVO = 15L * 1024 * 1024;
    private ImagenBannerService() {}

    public static byte[] preparar(File origen) throws IOException {
        if (origen == null || !origen.isFile())
            throw new IOException("Selecciona un archivo de imagen válido.");
        if (origen.length() > MAX_ARCHIVO)
            throw new IOException("La imagen original supera 15 MB. Elige una más pequeña.");
        BufferedImage entrada = ImageIO.read(origen);
        if (entrada == null || entrada.getWidth() < 320 || entrada.getHeight() < 180)
            throw new IOException("Formato no admitido o resolución menor a 320 x 180.");
        int[] anchos = {1200, 1000, 800};
        float[] calidades = {0.83f, 0.73f, 0.63f, 0.53f, 0.43f};
        for (int ancho : anchos) {
            BufferedImage salida = adaptar16a9(entrada, ancho, ancho * 9 / 16);
            for (float calidad : calidades) {
                byte[] jpg = generarJpeg(salida, calidad);
                if (jpg.length <= MAX_BYTES) return jpg;
            }
        }
        throw new IOException("No fue posible reducir la imagen a menos de 450 KB.");
    }

    private static BufferedImage adaptar16a9(BufferedImage entrada, int w, int h) {
        double relacion = (double) w / h;
        int anchoCorte = entrada.getWidth(), altoCorte = entrada.getHeight();
        if ((double) anchoCorte / altoCorte > relacion)
            anchoCorte = (int)Math.round(altoCorte * relacion);
        else altoCorte = (int)Math.round(anchoCorte / relacion);
        int x = (entrada.getWidth() - anchoCorte) / 2;
        int y = (entrada.getHeight() - altoCorte) / 2;
        BufferedImage salida = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = salida.createGraphics();
        try {
            g.setColor(Color.BLACK);
            g.fillRect(0, 0, w, h);
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g.drawImage(entrada, 0, 0, w, h,
                    x, y, x + anchoCorte, y + altoCorte, null);
        } finally { g.dispose(); }
        return salida;
    }

    private static byte[] generarJpeg(BufferedImage img, float calidad) throws IOException {
        ImageWriter writer = ImageIO.getImageWritersByFormatName("jpg").next();
        try (ByteArrayOutputStream out = new ByteArrayOutputStream();
             MemoryCacheImageOutputStream destino = new MemoryCacheImageOutputStream(out)) {
            writer.setOutput(destino);
            ImageWriteParam params = writer.getDefaultWriteParam();
            params.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            params.setCompressionQuality(calidad);
            writer.write(null, new IIOImage(img, null, null), params);
            destino.flush();
            return out.toByteArray();
        } finally { writer.dispose(); }
    }
}
