
package servicio;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import javax.imageio.ImageIO;

public class BoletoQRService {

    public BufferedImage generarQR(String contenido, int tamano)
            throws WriterException {

        if (contenido == null || contenido.isBlank()) {
            throw new IllegalArgumentException(
                    "El contenido del QR no puede estar vacío."
            );
        }

        if (tamano < 100) {
            throw new IllegalArgumentException(
                    "El tamaño mínimo del QR es de 100 píxeles."
            );
        }

        QRCodeWriter escritor = new QRCodeWriter();

        BitMatrix matriz = escritor.encode(
                contenido,
                BarcodeFormat.QR_CODE,
                tamano,
                tamano
        );

        BufferedImage imagen = new BufferedImage(
                tamano,
                tamano,
                BufferedImage.TYPE_INT_RGB
        );

        for (int x = 0; x < tamano; x++) {
            for (int y = 0; y < tamano; y++) {
                imagen.setRGB(
                        x,
                        y,
                        matriz.get(x, y)
                                ? 0x000000
                                : 0xFFFFFF
                );
            }
        }

        return imagen;
    }

    public void guardarQR(
            String contenido,
            int tamano,
            Path destino) throws WriterException, IOException {

        BufferedImage imagen = generarQR(contenido, tamano);

        if (!ImageIO.write(imagen, "PNG", destino.toFile())) {
            throw new IOException(
                    "No se pudo guardar la imagen QR."
            );
        }
    }
}
