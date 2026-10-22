package controladorFx;

import com.github.sarxos.webcam.Webcam;
import dao.ConciertoDAO;
import modelo.Concierto;
import modelo.Usuario;
import modelo.ResultadoValidacionAcceso;
import servicio.ValidadorBoletoService;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.layout.VBox;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.NotFoundException;
import com.google.zxing.RGBLuminanceSource;
import com.google.zxing.Result;
import com.google.zxing.common.HybridBinarizer;
import java.awt.Graphics2D;
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
import javafx.scene.control.TextArea;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.util.StringConverter;

/**
 * Paso 72.4: cámara en vivo y lectura de QR sin acceder a Neon.
 * El QR detectado se muestra como texto, NO autoriza ingresos.
 */
public class ControlAccesoFXController {

    @FXML private ComboBox<Webcam> cmbCamaras;
    @FXML private ComboBox<Concierto> cmbConciertos;
    @FXML private Button btnActualizarConciertos;
    @FXML private Button btnValidar;
    @FXML private Label lblResultadoValidacion;
    @FXML private Label lblDetalleValidacion;
    @FXML private VBox panelValidacion;
    @FXML private StackPane panelVideo;
    @FXML private ImageView imgCamara;
    @FXML private Label lblVistaPrevia;
    @FXML private Label lblEstado;
    @FXML private Label lblLectura;
    @FXML private TextArea txtResultadoQR;
    @FXML private Button btnIniciar;
    @FXML private Button btnDetener;
    @FXML private Button btnActualizar;
    @FXML private Button btnRotar;
    @FXML private Button btnRearmar;

    private volatile boolean activo;
    private volatile boolean destruido;
    private volatile boolean qrDetectado;
    private volatile int gradosRotacion = 0;
    private Thread hiloCaptura;
    private Usuario usuario;
    private volatile boolean validando;
    private String ultimoQr;
    private final ValidadorBoletoService validador = new ValidadorBoletoService();
    private static final DateTimeFormatter FORMATO_FECHA =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @FXML
    private void initialize() {
        btnIniciar.setDisable(true);
        btnDetener.setDisable(true);
        btnRearmar.setDisable(true);
        btnValidar.setDisable(true);
        lblResultadoValidacion.setText("Esperando lectura de QR");
        lblDetalleValidacion.setText("Selecciona el concierto y escanea un boleto.");
        cmbConciertos.setConverter(new StringConverter<>() {
            @Override public String toString(Concierto concierto) {
                if (concierto == null) return "";
                String fecha = concierto.getFechaConcierto() == null
                        ? "Sin fecha" : concierto.getFechaConcierto().format(FORMATO_FECHA);
                return concierto.getTituloEvento() + "  |  " + fecha;
            }
            @Override public Concierto fromString(String texto) { return null; }
        });
        cmbConciertos.valueProperty().addListener((o, a, b) -> actualizarBotonValidar());

        cmbCamaras.setConverter(new StringConverter<>() {
            @Override public String toString(Webcam camara) {
                return camara == null ? "" : camara.getName();
            }
            @Override public Webcam fromString(String texto) {
                return null;
            }
        });

        // Impedir que la imagen sobresalga del StackPane.
        javafx.scene.shape.Rectangle clip = new javafx.scene.shape.Rectangle();
        clip.widthProperty().bind(panelVideo.widthProperty());
        clip.heightProperty().bind(panelVideo.heightProperty());
        clip.setArcWidth(28);
        clip.setArcHeight(28);
        panelVideo.setClip(clip);

        // Ajuste automático, también cuando la ventana cambia de tamaño.
        panelVideo.widthProperty().addListener((o, a, b) -> ajustarImagen());
        panelVideo.heightProperty().addListener((o, a, b) -> ajustarImagen());
        actualizarCamaras();
        actualizarConciertos();
    }

    /** Recibe la sesión autenticada del MenuAdministradorFXController. */
    public void setUsuario(Usuario usuario) {
        if (usuario == null || usuario.getIdUsuario() <= 0) {
            throw new IllegalArgumentException("El Control de acceso requiere usuario autenticado.");
        }
        this.usuario = usuario;
        actualizarBotonValidar();
    }

    @FXML
    private void actualizarConciertos() {
        if (destruido || validando) return;
        btnActualizarConciertos.setDisable(true);
        cmbConciertos.setDisable(true);
        lblEstado.setText("Cargando conciertos desde Neon...");
        Task<List<Concierto>> tarea = new Task<>() {
            @Override protected List<Concierto> call() {
                return new ConciertoDAO().listarTodos();
            }
        };
        tarea.setOnSucceeded(event -> {
            if (destruido) return;
            Concierto actual = cmbConciertos.getValue();
            int idAnterior = actual == null ? -1 : actual.getIdConcierto();
            List<Concierto> conciertos = tarea.getValue();
            cmbConciertos.getItems().setAll(conciertos);
            Concierto conservar = conciertos.stream()
                    .filter(c -> c.getIdConcierto() == idAnterior)
                    .findFirst().orElse(null);
            cmbConciertos.setValue(conservar);
            cmbConciertos.setDisable(false);
            btnActualizarConciertos.setDisable(false);
            actualizarBotonValidar();
            lblEstado.setText(conciertos.isEmpty()
                    ? "No hay conciertos registrados." : "Selecciona el concierto a controlar.");
        });
        tarea.setOnFailed(event -> {
            if (destruido) return;
            cmbConciertos.setDisable(false);
            btnActualizarConciertos.setDisable(false);
            lblEstado.setText("No se pudieron cargar conciertos: "
                    + tarea.getException().getMessage());
        });
        Thread hilo = new Thread(tarea, "svb-cargar-conciertos-acceso");
        hilo.setDaemon(true);
        hilo.start();
    }

    private void actualizarBotonValidar() {
        if (btnValidar != null) {
            btnValidar.setDisable(destruido || validando
                    || usuario == null || cmbConciertos.getValue() == null
                    || ultimoQr == null || ultimoQr.isBlank());
        }
    }

    @FXML
    private void validarIngreso() {
        if (destruido || validando) return;
        Concierto concierto = cmbConciertos.getValue();
        if (usuario == null || concierto == null || ultimoQr == null || ultimoQr.isBlank()) {
            lblDetalleValidacion.setText("Selecciona un concierto y escanea un QR primero.");
            return;
        }
        String qr = ultimoQr;
        int idConcierto = concierto.getIdConcierto();
        int idUsuario = usuario.getIdUsuario();
        Alert confirmacion = new Alert(Alert.AlertType.CONFIRMATION);
        confirmacion.setTitle("SVB-GUA | Control de acceso");
        confirmacion.setHeaderText("¿Registrar el ingreso del boleto?");
        confirmacion.setContentText("Concierto: " + concierto.getTituloEvento()
                + "\n\nSi el boleto es válido, quedará marcado como utilizado "
                + "y no podrá ingresar otra vez.");
        if (btnValidar.getScene() != null) {
            confirmacion.initOwner(btnValidar.getScene().getWindow());
        }
        if (confirmacion.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) return;

        validando = true;
        btnValidar.setDisable(true);
        btnRearmar.setDisable(true);
        cmbConciertos.setDisable(true);
        btnActualizarConciertos.setDisable(true);
        lblResultadoValidacion.setText("Verificando en Neon...");
        lblDetalleValidacion.setText("No cierres el módulo mientras se procesa la operación.");

        Task<ResultadoValidacionAcceso> tarea = new Task<>() {
            @Override protected ResultadoValidacionAcceso call() throws SQLException {
                return validador.validarIngreso(qr, idConcierto, idUsuario);
            }
        };
        tarea.setOnSucceeded(event -> {
            if (destruido) return;
            validando = false;
            ResultadoValidacionAcceso resultado = tarea.getValue();
            lblResultadoValidacion.setText(resultado.autorizado()
                    ? "ACCESO AUTORIZADO" : "ACCESO DENEGADO: " + resultado.codigo());
            lblResultadoValidacion.setStyle(resultado.autorizado()
                    ? "-fx-font-size: 17px; -fx-font-weight: bold; -fx-text-fill: #15803D;"
                    : "-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #B91C1C;");
            String detalle = resultado.mensaje();
            if (resultado.idBoleto() > 0) {
                detalle += "\nBoleto #" + resultado.idBoleto()
                        + "  |  Venta #" + resultado.idVenta()
                        + "\nEvento: " + resultado.concierto()
                        + "\nLocalidad: " + resultado.localidad()
                        + "\nAsiento: " + resultado.asiento();
            }
            if (resultado.fechaIngreso() != null && !resultado.fechaIngreso().isBlank()) {
                detalle += "\nFecha ingreso: " + resultado.fechaIngreso();
            }
            lblDetalleValidacion.setText(detalle);
            btnRearmar.setDisable(false);
            cmbConciertos.setDisable(false);
            btnActualizarConciertos.setDisable(false);
            // Para repetir se exige pulsar Escanear otro QR, incluso si denegó.
            ultimoQr = null;
            actualizarBotonValidar();
        });
        tarea.setOnFailed(event -> {
            if (destruido) return;
            validando = false;
            lblResultadoValidacion.setText("ERROR DE VALIDACIÓN");
            lblResultadoValidacion.setStyle(
                    "-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #B91C1C;");
            lblDetalleValidacion.setText("No se pudo consultar Neon: "
                    + tarea.getException().getMessage()
                    + "\nNo repitas la venta. Puedes reintentar esta validación.");
            btnRearmar.setDisable(false);
            cmbConciertos.setDisable(false);
            btnActualizarConciertos.setDisable(false);
            actualizarBotonValidar();
        });
        Thread hilo = new Thread(tarea, "svb-validar-ingreso-neon");
        hilo.setDaemon(true);
        hilo.start();
    }

    private void ajustarImagen() {
        // Al rotar 90/270 grados, se intercambian los límites para no desbordar.
        boolean vertical = gradosRotacion == 90 || gradosRotacion == 270;
        double ancho = Math.max(1, panelVideo.getWidth() - 16);
        double alto = Math.max(1, panelVideo.getHeight() - 16);
        imgCamara.setFitWidth(vertical ? alto : ancho);
        imgCamara.setFitHeight(vertical ? ancho : alto);
        imgCamara.setRotate(gradosRotacion);
    }

    @FXML
    private void rotarCamara() {
        gradosRotacion = (gradosRotacion + 90) % 360;
        ajustarImagen();
    }

    @FXML
    private void actualizarCamaras() {
        if (destruido) return;
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
        qrDetectado = false;
        ultimoQr = null;
        actualizarBotonValidar();
        lblResultadoValidacion.setText("Esperando lectura de QR");
        lblDetalleValidacion.setText("Apunta el iPhone al boleto.");
        btnIniciar.setDisable(true);
        btnDetener.setDisable(false);
        btnRearmar.setDisable(true);
        btnActualizar.setDisable(true);
        cmbCamaras.setDisable(true);
        lblVistaPrevia.setVisible(true);
        lblVistaPrevia.setText("Conectando cámara...");
        lblLectura.setText("Apunta el iPhone al QR del boleto.");
        txtResultadoQR.setText("Aún no se ha leído ningún QR.");
        lblEstado.setText("Abriendo " + seleccionada.getName() + "...");

        hiloCaptura = new Thread(() -> {
            String error = null;
            long ultimoIntentoQR = 0;
            long ultimoFotograma = 0;
            try {
                seleccionada.open();
                while (activo && !Thread.currentThread().isInterrupted()) {
                    BufferedImage imagen = seleccionada.getImage();
                    if (imagen != null) {
                        long ahora = System.currentTimeMillis();
                        // Se limita el envío de fotogramas a JavaFX.
                        if (ahora - ultimoFotograma >= 130) {
                            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                            ImageIO.write(imagen, "jpg", bytes);
                            Image imagenFX = new Image(new ByteArrayInputStream(bytes.toByteArray()));
                            Platform.runLater(() -> {
                                if (activo && !destruido) {
                                    imgCamara.setImage(imagenFX);
                                    lblVistaPrevia.setVisible(false);
                                }
                            });
                            ultimoFotograma = ahora;
                        }
                        if (!qrDetectado && ahora - ultimoIntentoQR >= 350) {
                            ultimoIntentoQR = ahora;
                            String texto = leerQR(imagen);
                            if (texto == null && (gradosRotacion == 90 || gradosRotacion == 270)) {
                                // Algunas cámaras físicas proporcionan imagen de lado.
                                texto = leerQR(rotarImagen(imagen, gradosRotacion));
                            }
                            if (texto != null) {
                                qrDetectado = true;
                                final String resultado = texto;
                                Platform.runLater(() -> {
                                    if (!destruido && activo) {
                                        ultimoQr = resultado;
                                        txtResultadoQR.setText(resultado);
                                        actualizarBotonValidar();
                                        lblLectura.setText("QR leído correctamente. Presiona 'Escanear otro QR' para continuar.");
                                        btnRearmar.setDisable(false);
                                    }
                                });
                            }
                        }
                    }
                    Thread.sleep(45);
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
        }, "svb-lector-qr-camara");
        hiloCaptura.setDaemon(true);
        hiloCaptura.start();
    }

    // Solo utiliza ZXing core: no requiere javafx.swing ni la biblioteca javase.
    private static String leerQR(BufferedImage imagen) {
        try {
            int ancho = imagen.getWidth();
            int alto = imagen.getHeight();
            int[] pixeles = imagen.getRGB(0, 0, ancho, alto, null, 0, ancho);
            RGBLuminanceSource fuente = new RGBLuminanceSource(ancho, alto, pixeles);
            BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(fuente));
            Result resultado = new MultiFormatReader().decode(bitmap);
            return resultado.getText();
        } catch (NotFoundException ex) {
            // Un fotograma sin QR es normal; no imprimir miles de errores.
            return null;
        }
    }

    private static BufferedImage rotarImagen(BufferedImage imagen, int grados) {
        int ancho = imagen.getWidth();
        int alto = imagen.getHeight();
        BufferedImage salida = new BufferedImage(alto, ancho, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = salida.createGraphics();
        try {
            g.translate(alto / 2.0, ancho / 2.0);
            g.rotate(Math.toRadians(grados));
            g.drawImage(imagen, -ancho / 2, -alto / 2, null);
        } finally {
            g.dispose();
        }
        return salida;
    }

    @FXML
    private void escanearOtro() {
        if (validando) return;
        ultimoQr = null;
        actualizarBotonValidar();
        lblResultadoValidacion.setText("Esperando lectura de QR");
        lblResultadoValidacion.setStyle(
                "-fx-font-size: 17px; -fx-font-weight: bold; -fx-text-fill: #0F172A;");
        lblDetalleValidacion.setText("Apunta al siguiente boleto.");
        txtResultadoQR.setText("Aún no se ha leído ningún QR.");
        lblLectura.setText("Listo para otro QR. Aparta primero el boleto anterior de la cámara.");
        btnRearmar.setDisable(true);
        qrDetectado = false;
    }

    @FXML
    public void detenerCamara() {
        activo = false;
        if (hiloCaptura != null) hiloCaptura.interrupt();
        if (lblEstado != null) lblEstado.setText("Deteniendo cámara...");
    }

    /** El menú Administrador debe llamar a cerrar() al salir de la vista. */
    public void cerrar() {
        destruido = true;
        ultimoQr = null;
        detenerCamara();
    }
}
