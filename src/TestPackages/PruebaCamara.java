
package TestPackages;

import com.github.sarxos.webcam.Webcam;
import java.util.List;

public class PruebaCamara {

    public static void main(String[] args) {

        System.out.println("SVB-GUA - DETECCION DE CAMARAS");
        System.out.println("-----------------------------");

        try {
            List<Webcam> camaras = Webcam.getWebcams();

            if (camaras.isEmpty()) {
                System.out.println(
                    "No se encontraron camaras disponibles."
                );
                return;
            }

            System.out.println(
                "Camaras detectadas: " + camaras.size()
            );

            for (int i = 0; i < camaras.size(); i++) {

                Webcam camara = camaras.get(i);

                System.out.println(
                    (i + 1) + ". " + camara.getName()
                );
            }

            System.out.println(
                "\nDeteccion finalizada correctamente."
            );

        } catch (Exception e) {

            System.err.println(
                "Error al detectar las camaras."
            );

            e.printStackTrace();
        }
    }
}
