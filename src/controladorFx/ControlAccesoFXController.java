package controladorFx;

import com.github.sarxos.webcam.Webcam;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.List;
import javax.imageio.ImageIO;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

/**
 * Primera etapa del validador: solo previsualizacion de camara.
 * No lee QR, no consulta Neon y no modifica boletos.
 */
public class ControlAccesoFXController {

    @FXML private ComboBox<Webcam> cmbCamaras;
    @FXML private ImageView imgCamara;
    @FXML private Label lblVistaPrevia;
    @FXML private Label lblEstado;
    @FXML private Button btnIniciar;
    @FXML private Button btnDetener;
    @FXML private Button btnActualizar;

    private volatile boolean activo;
    private volatile boolean destruido;
    private Thread hiloCaptura;

    @FXML
    private void initialize() {
        btnIniciar.setDisable(true);
        btnDetener.setDisable(true);
        cmbCamaras.setConverter(new javafx.util.StringConverter<>() {
            @Override public String toString(Webcam camara) {
                return camara == null ? "" : camara.getName();
            }
            @Override public Webcam fromString(String texto) {
                return null;
            }
        });
        actualizarCamaras();
    }

    @FXML
    private void actualizarCamaras() {
        if (activo || (hiloCaptura != null && hiloCaptura.isAlive())) {
            lblEstado.setText("Detén la cámara antes de actualizar la lista.");
            return;
        }
        btnActualizar.setDisable(true);
        btnIniciar.setDisable(true);
        lblEstado.setText("Buscando cámaras en Windows...");

        Task<List<Webcam>> tarea = new Task<>() {
            @Override protected List<Webcam> call() {
                return Webcam.getWebcams();
            }
        };
        tarea.setOnSucceeded(event -> {
            if (destruido) return;
            List<Webcam> camaras = tarea.getValue();
            cmbCamaras.getItems().setAll(camaras);
            Webcam preferida = camaras.stream()
                    .filter(c -> c.getName().toLowerCase().contains("droidcam"))
                    .findFirst().orElse(camaras.isEmpty() ? null : camaras.get(0));
            cmbCamaras.setValue(preferida);
            btnIniciar.setDisable(preferida == null);
            btnActualizar.setDisable(false);
            lblEstado.setText(camaras.isEmpty()
                    ? "No se detectaron cámaras. Revisa DroidCam y vuelve a actualizar."
                    : "Cámaras detectadas: " + camaras.size() + ". Selecciona una e inicia.");
        });
        tarea.setOnFailed(event -> {
            if (destruido) return;
            btnActualizar.setDisable(false);
            lblEstado.setText("No se pudieron detectar cámaras: " + tarea.getException().getMessage());
        });
        Thread hilo = new Thread(tarea, "svb-listar-camaras");
        hilo.setDaemon(true);
        hilo.start();
    }

    @FXML
    private void iniciarCamara() {
        if (destruido || activo || (hiloCaptura != null && hiloCaptura.isAlive())) return;
        Webcam seleccionada = cmbCamaras.getValue();
        if (seleccionada == null) {
            lblEstado.setText("Primero selecciona una cámara.");
            return;
        }

        activo = true;
        btnIniciar.setDisable(true);
        btnDetener.setDisable(false);
        btnActualizar.setDisable(true);
        cmbCamaras.setDisable(true);
        lblVistaPrevia.setVisible(true);
        lblVistaPrevia.setText("Conectando cámara...");
        lblEstado.setText("Abriendo " + seleccionada.getName() + "...");

        hiloCaptura = new Thread(() -> {
            String error = null;
            try {
                seleccionada.open();
                while (activo && !Thread.currentThread().isInterrupted()) {
                    BufferedImage imagen = seleccionada.getImage();
                    if (imagen != null) {
                        // Conversión sin javafx.swing: solo usa clases estándar.
                        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                        ImageIO.write(imagen, "jpg", bytes);
                        Image imagenFX = new Image(
                                new ByteArrayInputStream(bytes.toByteArray()));
                        Platform.runLater(() -> {
                            if (activo && !destruido) {
                                imgCamara.setImage(imagenFX);
                                lblVistaPrevia.setVisible(false);
                                lblEstado.setText("Cámara activa: " + seleccionada.getName());
                            }
                        });
                    }
                    Thread.sleep(125); // Aproximadamente 8 fps para vista previa.
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                error = e.getMessage();
                e.printStackTrace();
            } finally {
                try {
                    if (seleccionada.isOpen()) seleccionada.close();
                } catch (Exception cierre) {
                    cierre.printStackTrace();
                }
                final String mensajeError = error;
                Platform.runLater(() -> {
                    if (destruido) return;
                    activo = false;
                    imgCamara.setImage(null);
                    lblVistaPrevia.setVisible(true);
                    lblVistaPrevia.setText("La cámara está detenida");
                    btnDetener.setDisable(true);
                    btnIniciar.setDisable(cmbCamaras.getValue() == null);
                    btnActualizar.setDisable(false);
                    cmbCamaras.setDisable(false);
                    lblEstado.setText(mensajeError == null
                            ? "Cámara detenida." : "Error al abrir/capturar: " + mensajeError);
                });
            }
        }, "svb-vista-previa-camara");
        hiloCaptura.setDaemon(true);
        hiloCaptura.start();
    }

    @FXML
    public void detenerCamara() {
        activo = false;
        if (hiloCaptura != null) hiloCaptura.interrupt();
        // La cámara se cierra en el finally del hilo; evita cerrar desde JavaFX.
        if (lblEstado != null) lblEstado.setText("Deteniendo cámara...");
    }

    /** Se invoca al salir de la vista para liberar el dispositivo. */
    public void cerrar() {
        destruido = true;
        detenerCamara();
    }
}
