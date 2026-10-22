
package TestPackages;


import com.github.sarxos.webcam.Webcam;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

public class PruebaCapturaDroidCam {

    public static void main(String[] args) {

        Webcam camara = null;

        try {
            // Buscar la camara virtual del iPhone.
            for (Webcam dispositivo : Webcam.getWebcams()) {

                if (dispositivo.getName()
                        .toLowerCase()
                        .contains("droidcam")) {

                    camara = dispositivo;
                    break;
                }
            }

            if (camara == null) {
                System.out.println(
                    "No se encontro DroidCam."
                );
                return;
            }

            System.out.println(
                "Camara encontrada: " + camara.getName()
            );

            // Abrir la camara.
            camara.open();

            // Capturar una imagen.
            BufferedImage imagen = camara.getImage();

            if (imagen == null) {
                throw new Exception(
                    "No se pudo capturar la imagen."
                );
            }

            // Guardar en la carpeta del usuario.
            File archivo = new File(
                System.getProperty("user.home"),
                "SVB-GUA-PruebaCamara.png"
            );

            ImageIO.write(imagen, "PNG", archivo);

            System.out.println(
                "IMAGEN CAPTURADA CORRECTAMENTE"
            );

            System.out.println(
                "Guardada en: " + archivo.getAbsolutePath()
            );

        } catch (Exception e) {

            System.err.println(
                "Error al capturar desde DroidCam."
            );

            e.printStackTrace();

        } finally {

            if (camara != null && camara.isOpen()) {
                camara.close();
            }
        }
    }
}
