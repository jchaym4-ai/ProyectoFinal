package controladorFx;

import dao.AnulacionDAO;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.function.Consumer;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.util.StringConverter;
import modelo.Usuario;
import servicio.AnulacionService;

/**
 * Administrador: listado general, filtro por concierto y anulaciones.
 * El filtrado nunca modifica la base de datos. Las anulaciones usan
 * AnulacionService, que valida de nuevo y ejecuta commit/rollback en Neon.
 */
public class AnulacionesFXController {

    @FXML private ComboBox<ConciertoOpcion> cmbConciertos;
    @FXML private TextField txtBuscar;
    @FXML private TextArea txtMotivo;
    @FXML private Button btnActualizar;
    @FXML private Button btnLimpiar;
    @FXML private Button btnAnularBoleto;
    @FXML private Button btnAnularVenta;
    @FXML private Label lblResumen;
    @FXML private Label lblSeleccionado;
    @FXML private Label lblEstado;
    @FXML private TableView<AnulacionDAO.BoletoParaAnulacion> tablaBoletos;
    @FXML private TableColumn<AnulacionDAO.BoletoParaAnulacion, String> colBoleto;
    @FXML private TableColumn<AnulacionDAO.BoletoParaAnulacion, String> colVenta;
    @FXML private TableColumn<AnulacionDAO.BoletoParaAnulacion, String> colConcierto;
    @FXML private TableColumn<AnulacionDAO.BoletoParaAnulacion, String> colLocalidad;
    @FXML private TableColumn<AnulacionDAO.BoletoParaAnulacion, String> colAsiento;
    @FXML private TableColumn<AnulacionDAO.BoletoParaAnulacion, String> colEstado;
    @FXML private TableColumn<AnulacionDAO.BoletoParaAnulacion, String> colPrecio;

    private final AnulacionService servicio = new AnulacionService();
    private final ObservableList<AnulacionDAO.BoletoParaAnulacion> todos =
            FXCollections.observableArrayList();
    private final FilteredList<AnulacionDAO.BoletoParaAnulacion> filtrados =
            new FilteredList<>(todos, b -> true);

    private Usuario usuario;
    private boolean ocupado;
    private boolean cargando;

    private record ConciertoOpcion(int id, String nombre) {
        @Override public String toString() { return nombre; }
    }

    @FXML
    private void initialize() {
        colBoleto.setCellValueFactory(c -> valor("#" + c.getValue().idBoleto()));
        colVenta.setCellValueFactory(c -> valor("#" + c.getValue().idVenta()));
        colConcierto.setCellValueFactory(c -> valor(c.getValue().concierto()));
        colLocalidad.setCellValueFactory(c -> valor(c.getValue().localidad()));
        colAsiento.setCellValueFactory(c -> valor(textoAsiento(c.getValue())));
        colEstado.setCellValueFactory(c -> valor(estadoVisual(c.getValue())));
        colPrecio.setCellValueFactory(c -> valor(dinero(c.getValue().precio())));

        tablaBoletos.setItems(filtrados);
        tablaBoletos.getSelectionModel().selectedItemProperty().addListener(
                (obs, anterior, nuevo) -> {
                    mostrarSeleccion(nuevo);
                    actualizarBotones();
                });

        cmbConciertos.setConverter(new StringConverter<>() {
            @Override public String toString(ConciertoOpcion c) {
                return c == null ? "" : c.nombre();
            }
            @Override public ConciertoOpcion fromString(String texto) { return null; }
        });
        cmbConciertos.valueProperty().addListener((obs, anterior, nuevo) -> aplicarFiltros());
        txtBuscar.textProperty().addListener((obs, anterior, nuevo) -> aplicarFiltros());
        cmbConciertos.getItems().add(new ConciertoOpcion(0, "Todos los conciertos"));
        cmbConciertos.getSelectionModel().selectFirst();
        lblEstado.setText("Esperando una sesión de administrador...");
        mostrarSeleccion(null);
        actualizarBotones();
    }

    /** Lo llama MenuAdministradorFXController luego de cargar el FXML. */
    public void setUsuario(Usuario usuario) {
        if (usuario == null || usuario.getIdUsuario() <= 0) {
            throw new IllegalArgumentException("Se requiere una sesión de administrador.");
        }
        this.usuario = usuario;
        cargarBoletos(-1);
    }

    @FXML
    private void actualizarTabla() {
        if (!ocupado && !cargando && usuario != null) {
            AnulacionDAO.BoletoParaAnulacion seleccionado =
                    tablaBoletos.getSelectionModel().getSelectedItem();
            cargarBoletos(seleccionado == null ? -1 : seleccionado.idBoleto());
        }
    }

    @FXML
    private void limpiarFiltros() {
        txtBuscar.clear();
        cmbConciertos.getSelectionModel().selectFirst();
        aplicarFiltros();
    }

    private void cargarBoletos(int idConservar) {
        if (ocupado || cargando || usuario == null) return;
        cargando = true;
        actualizarBotones();
        lblEstado.setText("Cargando boletos desde Neon...");
        Task<List<AnulacionDAO.BoletoParaAnulacion>> tarea = new Task<>() {
            @Override protected List<AnulacionDAO.BoletoParaAnulacion> call() throws SQLException {
                return servicio.listarTodos();
            }
        };
        tarea.setOnSucceeded(e -> {
            cargando = false;
            int anteriorConcierto = cmbConciertos.getValue() == null
                    ? 0 : cmbConciertos.getValue().id();
            todos.setAll(tarea.getValue());
            reconstruirConciertos(anteriorConcierto);
            aplicarFiltros();
            if (idConservar > 0) {
                for (AnulacionDAO.BoletoParaAnulacion boleto : filtrados) {
                    if (boleto.idBoleto() == idConservar) {
                        tablaBoletos.getSelectionModel().select(boleto);
                        tablaBoletos.scrollTo(boleto);
                        break;
                    }
                }
            }
            lblEstado.setText("Listado actualizado desde Neon. Selecciona un boleto válido para anularlo.");
            actualizarBotones();
        });
        tarea.setOnFailed(e -> {
            cargando = false;
            lblEstado.setText("Error al consultar Neon: " + descripcionError(tarea.getException()));
            actualizarBotones();
            avisar(Alert.AlertType.ERROR, "Error de consulta", descripcionError(tarea.getException()));
        });
        ejecutar(tarea, "svb-gua-listar-anulaciones");
    }

    private void reconstruirConciertos(int seleccionAnterior) {
        Map<Integer, String> opciones = new TreeMap<>();
        for (AnulacionDAO.BoletoParaAnulacion b : todos) {
            opciones.putIfAbsent(b.idConcierto(), b.concierto());
        }
        List<ConciertoOpcion> lista = new ArrayList<>();
        lista.add(new ConciertoOpcion(0, "Todos los conciertos"));
        opciones.forEach((id, nombre) -> lista.add(new ConciertoOpcion(id, nombre)));
        cmbConciertos.getItems().setAll(lista);
        ConciertoOpcion seleccion = lista.stream()
                .filter(o -> o.id() == seleccionAnterior)
                .findFirst().orElse(lista.get(0));
        cmbConciertos.setValue(seleccion);
    }

    private void aplicarFiltros() {
        ConciertoOpcion seleccion = cmbConciertos.getValue();
        int idConcierto = seleccion == null ? 0 : seleccion.id();
        String busqueda = normalizar(txtBuscar.getText());
        filtrados.setPredicate(b -> (idConcierto == 0 || b.idConcierto() == idConcierto)
                && (busqueda.isBlank()
                    || contiene("" + b.idBoleto(), busqueda)
                    || contiene("" + b.idVenta(), busqueda)
                    || contiene(b.codigo(), busqueda)
                    || contiene(b.concierto(), busqueda)
                    || contiene(b.localidad(), busqueda)
                    || contiene(textoAsiento(b), busqueda)
                    || contiene(estadoVisual(b), busqueda)));
        long anulables = filtrados.stream().filter(this::puedeAnular).count();
        lblResumen.setText("Mostrando " + filtrados.size() + " de " + todos.size()
                + " boletos · " + anulables + " anulables");
        actualizarBotones();
    }

    private static String normalizar(String texto) {
        if (texto == null) return "";
        return Normalizer.normalize(texto, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "").toLowerCase(Locale.ROOT).trim();
    }

    private static boolean contiene(String texto, String busqueda) {
        return normalizar(texto).contains(busqueda);
    }

    private void mostrarSeleccion(AnulacionDAO.BoletoParaAnulacion boleto) {
        if (boleto == null) {
            lblSeleccionado.setText("Selecciona una fila para ver el estado y habilitar la anulación.");
            return;
        }
        boolean ventaAnulable = ventaEnteraAnulable(boleto.idVenta());
        lblSeleccionado.setText("Boleto #" + boleto.idBoleto()
                + " · Venta #" + boleto.idVenta()
                + " · " + boleto.concierto()
                + " · " + boleto.localidad() + " · " + textoAsiento(boleto)
                + " · " + estadoVisual(boleto)
                + "\nCódigo: " + boleto.codigo()
                + "\nAnulación individual: " + (puedeAnular(boleto) ? "Permitida" : "No permitida")
                + " · Venta completa: " + (ventaAnulable ? "Permitida" : "No permitida"));
    }

    @FXML
    private void anularBoleto() {
        if (ocupado || cargando || usuario == null) return;
        AnulacionDAO.BoletoParaAnulacion b = tablaBoletos.getSelectionModel().getSelectedItem();
        if (!puedeAnular(b)) {
            avisar(Alert.AlertType.WARNING, "No se puede anular",
                    "Selecciona una entrada válida que no haya ingresado.");
            return;
        }
        String motivo = validarMotivo();
        if (motivo == null) return;
        if (!confirmar("Se anulará SOLO el boleto #" + b.idBoleto()
                + " de la venta #" + b.idVenta()
                + ".\nImporte de devolución simulada: " + dinero(b.precio())
                + "\nSe devolverá una entrada al inventario."
                + "\nLa venta y la factura originales se conservarán."
                + "\n\n¿Confirmas esta anulación?")) return;

        final int idBoleto = b.idBoleto();
        final int idAdmin = usuario.getIdUsuario();
        iniciarOperacion("Anulando boleto #" + idBoleto + "...", new Task<AnulacionDAO.ResultadoAnulacion>() {
            @Override protected AnulacionDAO.ResultadoAnulacion call() throws SQLException {
                return servicio.anularBoleto(idBoleto, idAdmin, motivo);
            }
        }, resultado -> {
            txtMotivo.clear();
            avisar(Alert.AlertType.INFORMATION, "Boleto anulado",
                    "Boleto #" + idBoleto + " cancelado correctamente.\n"
                    + "Devolución simulada: " + dinero(resultado.importeAnulado()));
            cargarBoletos(idBoleto);
        });
    }

    @FXML
    private void anularVenta() {
        if (ocupado || cargando || usuario == null) return;
        AnulacionDAO.BoletoParaAnulacion b = tablaBoletos.getSelectionModel().getSelectedItem();
        if (b == null || !ventaEnteraAnulable(b.idVenta())) {
            avisar(Alert.AlertType.WARNING, "Venta no anulable",
                    "Para anular una venta completa, todos sus boletos deben estar válidos y sin ingreso.");
            return;
        }
        String motivo = validarMotivo();
        if (motivo == null) return;
        long cantidad = todos.stream().filter(x -> x.idVenta() == b.idVenta()).count();
        BigDecimal importe = todos.stream().filter(x -> x.idVenta() == b.idVenta())
                .map(AnulacionDAO.BoletoParaAnulacion::precio)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (!confirmar("Se anularán TODOS los " + cantidad + " boletos de la venta #"
                + b.idVenta() + ".\nDevolución simulada: " + dinero(importe)
                + "\nEsto afectará el inventario de las localidades correspondientes."
                + "\nSi algún boleto ya ingresó, Neon deshará la operación."
                + "\n\n¿Confirmas la anulación de la venta completa?")) return;

        final int idVenta = b.idVenta();
        final int idAdmin = usuario.getIdUsuario();
        final int idConservar = b.idBoleto();
        iniciarOperacion("Anulando venta #" + idVenta + "...", new Task<AnulacionDAO.ResultadoAnulacion>() {
            @Override protected AnulacionDAO.ResultadoAnulacion call() throws SQLException {
                return servicio.anularVentaCompleta(idVenta, idAdmin, motivo);
            }
        }, resultado -> {
            txtMotivo.clear();
            avisar(Alert.AlertType.INFORMATION, "Venta anulada",
                    resultado.boletosAnulados().size() + " boletos anulados.\n"
                    + "Devolución simulada: " + dinero(resultado.importeAnulado()));
            cargarBoletos(idConservar);
        });
    }

    private boolean puedeAnular(AnulacionDAO.BoletoParaAnulacion b) {
        return b != null && "valido".equalsIgnoreCase(b.estado())
                && !b.yaIngreso() && !b.yaAnulado();
    }

    /** Usa la lista COMPLETA, nunca sólo los resultados filtrados. */
    private boolean ventaEnteraAnulable(int idVenta) {
        List<AnulacionDAO.BoletoParaAnulacion> boletos = todos.stream()
                .filter(b -> b.idVenta() == idVenta).toList();
        return !boletos.isEmpty() && boletos.stream().allMatch(this::puedeAnular);
    }

    private String validarMotivo() {
        String motivo = txtMotivo.getText() == null ? "" : txtMotivo.getText().trim();
        if (motivo.length() < 5 || motivo.length() > 500) {
            avisar(Alert.AlertType.WARNING, "Motivo obligatorio",
                    "Escribe un motivo entre 5 y 500 caracteres antes de anular.");
            txtMotivo.requestFocus();
            return null;
        }
        return motivo;
    }

    private void actualizarBotones() {
        boolean sesion = usuario != null && usuario.getIdUsuario() > 0;
        boolean activo = sesion && !ocupado && !cargando;
        btnActualizar.setDisable(!activo);
        btnLimpiar.setDisable(!activo);
        AnulacionDAO.BoletoParaAnulacion b = tablaBoletos.getSelectionModel().getSelectedItem();
        btnAnularBoleto.setDisable(!activo || !puedeAnular(b));
        btnAnularVenta.setDisable(!activo || b == null || !ventaEnteraAnulable(b.idVenta()));
    }

    private String estadoVisual(AnulacionDAO.BoletoParaAnulacion b) {
        if (b.yaIngreso() || "utilizado".equalsIgnoreCase(b.estado())) return "Ya ingresó";
        if (b.yaAnulado() || "cancelado".equalsIgnoreCase(b.estado())) return "Anulado";
        if ("valido".equalsIgnoreCase(b.estado())) return "Sin utilizar";
        return b.estado();
    }

    private String textoAsiento(AnulacionDAO.BoletoParaAnulacion b) {
        if (b.idAsiento() == null) return "Aforo general";
        return (b.fila() == null ? "?" : b.fila()) + "-"
                + (b.numeroAsiento() == null ? "?" : b.numeroAsiento());
    }

    private String dinero(BigDecimal importe) {
        if (importe == null) return "Q 0.00";
        return String.format(Locale.US, "Q %,.2f", importe);
    }

    private ReadOnlyStringWrapper valor(String dato) {
        return new ReadOnlyStringWrapper(dato == null ? "" : dato);
    }

    private String descripcionError(Throwable error) {
        return error == null || error.getMessage() == null ? "Error desconocido" : error.getMessage();
    }

    private <T> void iniciarOperacion(String mensaje, Task<T> tarea, Consumer<T> fin) {
        ocupado = true;
        actualizarBotones();
        lblEstado.setText(mensaje);
        tarea.setOnSucceeded(e -> {
            ocupado = false;
            actualizarBotones();
            fin.accept(tarea.getValue());
        });
        tarea.setOnFailed(e -> {
            ocupado = false;
            actualizarBotones();
            lblEstado.setText("Operación fallida: " + descripcionError(tarea.getException()));
            avisar(Alert.AlertType.ERROR, "Error al anular", descripcionError(tarea.getException()));
        });
        ejecutar(tarea, "svb-gua-anular-boletos");
    }

    private static void ejecutar(Task<?> tarea, String nombre) {
        Thread hilo = new Thread(tarea, nombre);
        hilo.setDaemon(true);
        hilo.start();
    }

    private boolean confirmar(String mensaje) {
        ButtonType confirmar = new ButtonType("Sí, anular", ButtonBar.ButtonData.OK_DONE);
        Alert alerta = new Alert(Alert.AlertType.CONFIRMATION);
        alerta.setTitle("SVB-GUA - Confirmar anulación");
        alerta.setHeaderText("Esta operación modificará Neon PostgreSQL");
        alerta.setContentText(mensaje);
        alerta.getButtonTypes().setAll(confirmar, ButtonType.CANCEL);
        Optional<ButtonType> respuesta = alerta.showAndWait();
        return respuesta.isPresent() && respuesta.get() == confirmar;
    }

    private void avisar(Alert.AlertType tipo, String titulo, String mensaje) {
        Alert alerta = new Alert(tipo);
        alerta.setTitle("SVB-GUA - Anulaciones");
        alerta.setHeaderText(titulo);
        alerta.setContentText(mensaje);
        alerta.showAndWait();
    }
}
