package controladorFx;

import dao.ConsultaBoletosDAO;
import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.Normalizer;
import java.time.Duration;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.stage.FileChooser;
import modelo.DetalleBoleto;

/** Lista todos los boletos y recupera sus PDF almacenados en Cloudflare. */
public class ConsultarBoletosFXController {

    private static final String HOST_PDF = "svb-gua-boletos.jchaym4.workers.dev";
    private static final DateTimeFormatter FECHA =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @FXML private TextField txtFiltro;
    @FXML private Button btnActualizar;
    @FXML private Label lblResumen;
    @FXML private Label lblMensaje;
    @FXML private Label lblDetalleTitulo;
    @FXML private Label lblDetalleDatos;
    @FXML private TableView<DetalleBoleto> tablaBoletos;
    @FXML private TableColumn<DetalleBoleto, String> colBoleto;
    @FXML private TableColumn<DetalleBoleto, String> colVenta;
    @FXML private TableColumn<DetalleBoleto, String> colEvento;
    @FXML private TableColumn<DetalleBoleto, String> colLocalidad;
    @FXML private TableColumn<DetalleBoleto, String> colAsiento;
    @FXML private TableColumn<DetalleBoleto, String> colEstado;
    @FXML private TableColumn<DetalleBoleto, String> colPDF;
    @FXML private TableColumn<DetalleBoleto, DetalleBoleto> colAcciones;

    private final ObservableList<DetalleBoleto> datos =
            FXCollections.observableArrayList();
    private final FilteredList<DetalleBoleto> filtrados =
            new FilteredList<>(datos, boleto -> true);

    @FXML
    private void initialize() {
        configurarTabla();
        tablaBoletos.setItems(filtrados);
        tablaBoletos.getSelectionModel().selectedItemProperty()
                .addListener((obs, anterior, nuevo) -> mostrarDetalle(nuevo));

        txtFiltro.textProperty().addListener((obs, anterior, nuevo) -> aplicarFiltro());
        mostrarDetalle(null);
        cargarBoletos(); // Al abrir, muestra TODOS los boletos sin buscar uno por uno.
    }

    private void configurarTabla() {
        colBoleto.setCellValueFactory(c -> valor("#" + c.getValue().getIdBoleto()));
        colVenta.setCellValueFactory(c -> valor("#" + c.getValue().getIdVenta()));
        colEvento.setCellValueFactory(c -> valor(c.getValue().getTituloEvento()));
        colLocalidad.setCellValueFactory(c -> valor(c.getValue().getLocalidad()));
        colAsiento.setCellValueFactory(c -> valor(asientoTexto(c.getValue())));
        colEstado.setCellValueFactory(c -> valor(estadoTexto(c.getValue().getEstado())));
        colPDF.setCellValueFactory(c -> valor(tienePDF(c.getValue()) ? "Disponible" : "Pendiente"));

        colAcciones.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue()));
        colAcciones.setSortable(false);
        colAcciones.setCellFactory(columna -> new TableCell<>() {
            private final Button ver = new Button("Ver PDF");
            private final Button descargar = new Button("Descargar");
            private final HBox acciones = new HBox(6, ver, descargar);

            {
                acciones.setAlignment(Pos.CENTER_LEFT);
                ver.getStyleClass().add("table-view-button");
                descargar.getStyleClass().add("table-download-button");
                ver.setOnAction(e -> {
                    DetalleBoleto boleto = getItem();
                    if (boleto != null) verPDF(boleto);
                });
                descargar.setOnAction(e -> {
                    DetalleBoleto boleto = getItem();
                    if (boleto != null) descargarPDF(boleto);
                });
            }

            @Override
            protected void updateItem(DetalleBoleto boleto, boolean vacio) {
                super.updateItem(boleto, vacio);
                if (vacio || boleto == null) {
                    setGraphic(null);
                } else {
                    ver.setDisable(!tienePDF(boleto));
                    descargar.setDisable(!tienePDF(boleto));
                    setGraphic(acciones);
                }
            }
        });
    }

    private ReadOnlyStringWrapper valor(String texto) {
        return new ReadOnlyStringWrapper(texto == null ? "" : texto);
    }

    @FXML
    private void actualizarTabla() {
        cargarBoletos();
    }

    @FXML
    private void limpiarFiltro() {
        txtFiltro.clear();
        tablaBoletos.getSelectionModel().clearSelection();
        txtFiltro.requestFocus();
    }

    private void cargarBoletos() {
        btnActualizar.setDisable(true);
        lblMensaje.setText("Consultando los boletos registrados en Neon...");

        Task<List<DetalleBoleto>> tarea = new Task<>() {
            @Override
            protected List<DetalleBoleto> call() throws Exception {
                return new ConsultaBoletosDAO().listarTodos();
            }
        };

        tarea.setOnSucceeded(e -> {
            btnActualizar.setDisable(false);
            datos.setAll(tarea.getValue());
            tablaBoletos.getSelectionModel().clearSelection();
            aplicarFiltro();
            lblMensaje.setText(datos.isEmpty()
                    ? "No hay boletos registrados en Neon."
                    : "Lista actualizada. Selecciona un boleto para ver sus datos.");
        });

        tarea.setOnFailed(e -> {
            btnActualizar.setDisable(false);
            lblMensaje.setText("No se pudo cargar la tabla: "
                    + descripcionError(tarea.getException()));
        });

        iniciar(tarea, "svb-gua-listar-boletos");
    }

    private void aplicarFiltro() {
        String busqueda = normalizar(txtFiltro.getText());
        filtrados.setPredicate(b -> busqueda.isBlank()
                || contiene(b.getCodigoBoleto(), busqueda)
                || contiene(b.getTituloEvento(), busqueda)
                || contiene(b.getNombreArtista(), busqueda)
                || contiene(b.getLocalidad(), busqueda)
                || contiene(b.getEstado(), busqueda)
                || contiene("" + b.getIdBoleto(), busqueda)
                || contiene("" + b.getIdVenta(), busqueda));

        lblResumen.setText("Mostrando " + filtrados.size()
                + " de " + datos.size() + " boletos");
    }

    private boolean contiene(String texto, String busqueda) {
        return normalizar(texto).contains(busqueda);
    }

    private String normalizar(String texto) {
        if (texto == null) return "";
        return Normalizer.normalize(texto, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .toLowerCase(Locale.ROOT)
                .trim();
    }

    private void mostrarDetalle(DetalleBoleto boleto) {
        if (boleto == null) {
            lblDetalleTitulo.setText("Selecciona un boleto de la tabla");
            lblDetalleDatos.setText("Veras aqui el artista, fecha, recinto, precio y codigo completo.");
            return;
        }

        String fecha = boleto.getFechaConcierto() == null
                ? "No disponible" : boleto.getFechaConcierto().format(FECHA);
        String precio = boleto.getPrecio() == null
                ? "No disponible" : String.format(Locale.US, "Q %,.2f", boleto.getPrecio());
        lblDetalleTitulo.setText("Boleto #" + boleto.getIdBoleto()
                + "  |  " + texto(boleto.getTituloEvento()));
        lblDetalleDatos.setText(
                "Artista: " + texto(boleto.getNombreArtista())
                + "   |   Fecha: " + fecha
                + "   |   Precio: " + precio
                + "\nRecinto: " + texto(boleto.getRecinto())
                + "   |   Localidad: " + texto(boleto.getLocalidad())
                + "   |   Asiento: " + asientoTexto(boleto)
                + "\nCodigo: " + texto(boleto.getCodigoBoleto())
        );
    }

    private String asientoTexto(DetalleBoleto boleto) {
        if (!"numerado".equalsIgnoreCase(boleto.getTipoVenta())) {
            return "Aforo general";
        }
        return texto(boleto.getFila()) + " - "
                + (boleto.getNumeroAsiento() == null
                ? "Sin asignar" : boleto.getNumeroAsiento());
    }

    private String estadoTexto(String estado) {
        if (estado == null) return "Desconocido";
        return switch (estado.toLowerCase(Locale.ROOT)) {
            case "valido" -> "Valido";
            case "usado" -> "Usado";
            case "cancelado" -> "Cancelado";
            default -> estado;
        };
    }

    private boolean tienePDF(DetalleBoleto boleto) {
        return boleto != null && boleto.getRutaPdf() != null
                && !boleto.getRutaPdf().isBlank();
    }

    private String texto(String texto) {
        return texto == null || texto.isBlank() ? "No disponible" : texto;
    }

    private URI urlSegura(DetalleBoleto boleto) throws IOException {
        if (!tienePDF(boleto)) {
            throw new IOException("Este boleto todavia no tiene PDF publicado.");
        }

        URI url;
        try {
            url = URI.create(boleto.getRutaPdf().trim());
        } catch (IllegalArgumentException e) {
            throw new IOException("La URL guardada en Neon no es valida.", e);
        }

        if (!"https".equalsIgnoreCase(url.getScheme())
                || !HOST_PDF.equalsIgnoreCase(url.getHost())
                || (url.getPort() != -1 && url.getPort() != 443)
                || url.getRawQuery() != null || url.getRawFragment() != null
                || !url.getPath().matches("/b/[a-f0-9]{32}")) {
            throw new IOException("La URL no pertenece al servidor de boletos SVB-GUA.");
        }
        return url;
    }

    private void verPDF(DetalleBoleto boleto) {
        try {
            URI url = urlSegura(boleto);
            if (!Desktop.isDesktopSupported()
                    || !Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                throw new IOException("El sistema no permite abrir enlaces automaticamente.");
            }
            Desktop.getDesktop().browse(url);
        } catch (Exception ex) {
            mostrarAlerta(Alert.AlertType.ERROR,
                    "No se pudo abrir el PDF", descripcionError(ex));
        }
    }

    private void descargarPDF(DetalleBoleto boleto) {
        URI url;
        try {
            url = urlSegura(boleto);
        } catch (IOException ex) {
            mostrarAlerta(Alert.AlertType.ERROR, "Enlace incorrecto", ex.getMessage());
            return;
        }

        FileChooser selector = new FileChooser();
        selector.setTitle("Descargar boleto #" + boleto.getIdBoleto());
        selector.setInitialFileName("boleto-" + boleto.getIdBoleto() + ".pdf");
        selector.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Archivos PDF", "*.pdf"));
        File destino = selector.showSaveDialog(tablaBoletos.getScene().getWindow());
        if (destino == null) return;

        tablaBoletos.setDisable(true);
        lblMensaje.setText("Descargando boleto #" + boleto.getIdBoleto() + "...");

        Task<Path> tarea = new Task<>() {
            @Override
            protected Path call() throws Exception {
                HttpRequest solicitud = HttpRequest.newBuilder(url)
                        .timeout(Duration.ofSeconds(40)).GET().build();
                try (HttpClient cliente = HttpClient.newBuilder()
                        .connectTimeout(Duration.ofSeconds(15))
                        .followRedirects(HttpClient.Redirect.NEVER)
                        .build()) {
                    HttpResponse<byte[]> respuesta = cliente.send(
                            solicitud, HttpResponse.BodyHandlers.ofByteArray());
                    byte[] bytes = respuesta.body();
                    if (respuesta.statusCode() != 200 || bytes == null
                            || bytes.length < 5 || bytes[0] != '%'
                            || bytes[1] != 'P' || bytes[2] != 'D'
                            || bytes[3] != 'F' || bytes[4] != '-') {
                        throw new IOException("Cloudflare no devolvio un PDF valido (HTTP "
                                + respuesta.statusCode() + ").");
                    }
                    Files.write(destino.toPath(), bytes);
                    return destino.toPath();
                }
            }
        };

        tarea.setOnSucceeded(e -> {
            tablaBoletos.setDisable(false);
            lblMensaje.setText("PDF guardado: " + tarea.getValue());
            mostrarAlerta(Alert.AlertType.INFORMATION,
                    "Descarga completada", "El PDF del boleto #"
                            + boleto.getIdBoleto() + " se guardo correctamente.");
        });
        tarea.setOnFailed(e -> {
            tablaBoletos.setDisable(false);
            lblMensaje.setText("No se pudo descargar el boleto #" + boleto.getIdBoleto());
            mostrarAlerta(Alert.AlertType.ERROR,
                    "Error de descarga", descripcionError(tarea.getException()));
        });
        iniciar(tarea, "svb-gua-descargar-boleto");
    }

    private String descripcionError(Throwable error) {
        return error == null || error.getMessage() == null
                ? "Error desconocido." : error.getMessage();
    }

    private void mostrarAlerta(Alert.AlertType tipo, String titulo, String contenido) {
        Alert alerta = new Alert(tipo);
        alerta.setTitle("SVB-GUA - Consultar Boletos");
        alerta.setHeaderText(titulo);
        alerta.setContentText(contenido);
        alerta.showAndWait();
    }

    private void iniciar(Task<?> tarea, String nombre) {
        Thread hilo = new Thread(tarea, nombre);
        hilo.setDaemon(true);
        hilo.start();
    }
}
