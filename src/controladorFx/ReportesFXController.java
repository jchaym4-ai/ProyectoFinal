package controladorFx;

import dao.ReportesDAO;
import dao.ReportesDAO.Concierto;
import dao.ReportesDAO.Datos;
import dao.ReportesDAO.Localidad;
import dao.ReportesDAO.Resumen;
import java.io.File;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import javafx.beans.property.ReadOnlyStringWrapper;
import servicio.ReportesPDFService;

/**
 * Paso 75.2: Dashboard de reportes en Administrador.
 * Todas las operaciones SQL son de consulta; la exportación no altera Neon.
 */
public final class ReportesFXController {

    @FXML private ComboBox<OpcionConcierto> cmbConciertos;
    @FXML private Button btnActualizar;
    @FXML private Button btnExportar;
    @FXML private Label lblEstado;
    @FXML private Label lblActualizacion;
    @FXML private Label lblFiltro;
    @FXML private Label lblNeto;
    @FXML private Label lblDevoluciones;
    @FXML private Label lblHistorico;
    @FXML private Label lblEmitidos;
    @FXML private Label lblAccesos;
    @FXML private Label lblCancelados;
    @FXML private Label lblSinUtilizar;
    @FXML private Label lblDisponibles;
    @FXML private Label lblVentas;

    @FXML private BarChart<String, Number> graficaIngresos;
    @FXML private PieChart graficaEstados;

    @FXML private TableView<Concierto> tablaConciertos;
    @FXML private TableColumn<Concierto, String> colConcierto;
    @FXML private TableColumn<Concierto, String> colFecha;
    @FXML private TableColumn<Concierto, String> colVentas;
    @FXML private TableColumn<Concierto, String> colEmitidos;
    @FXML private TableColumn<Concierto, String> colAsistencia;
    @FXML private TableColumn<Concierto, String> colCancelados;
    @FXML private TableColumn<Concierto, String> colNetos;

    @FXML private TableView<Localidad> tablaLocalidades;
    @FXML private TableColumn<Localidad, String> colLocalidadConcierto;
    @FXML private TableColumn<Localidad, String> colLocalidad;
    @FXML private TableColumn<Localidad, String> colTipoVenta;
    @FXML private TableColumn<Localidad, String> colCapacidad;
    @FXML private TableColumn<Localidad, String> colDisponibles;
    @FXML private TableColumn<Localidad, String> colLocalidadEmitidos;
    @FXML private TableColumn<Localidad, String> colLocalidadIngresos;
    @FXML private TableColumn<Localidad, String> colLocalidadAnulados;
    @FXML private TableColumn<Localidad, String> colLocalidadNetos;

    private record OpcionConcierto(int id, String nombre) {
        @Override public String toString() { return nombre; }
    }

    private final ReportesDAO dao = new ReportesDAO();
    private final ReportesPDFService pdf = new ReportesPDFService();
    private Datos datos;
    private boolean cargando;
    private boolean exportando;

    @FXML
    private void initialize() {
        // Lectura simple desde records: no requiere propiedades JavaFX en el DAO.
        colConcierto.setCellValueFactory(c -> str(c.getValue().nombre()));
        colFecha.setCellValueFactory(c -> str(fechaCorta(c.getValue().fecha())));
        colVentas.setCellValueFactory(c -> str(Long.toString(c.getValue().ventas())));
        colEmitidos.setCellValueFactory(c -> str(Long.toString(c.getValue().emitidos())));
        colAsistencia.setCellValueFactory(c -> str(Long.toString(c.getValue().ingresos())));
        colCancelados.setCellValueFactory(c -> str(Long.toString(c.getValue().cancelados())));
        colNetos.setCellValueFactory(c -> str(q(c.getValue().netos())));

        colLocalidadConcierto.setCellValueFactory(c -> str(c.getValue().concierto()));
        colLocalidad.setCellValueFactory(c -> str(c.getValue().nombre()));
        colTipoVenta.setCellValueFactory(c -> str(c.getValue().tipoVenta()));
        colCapacidad.setCellValueFactory(c -> str(Long.toString(c.getValue().capacidad())));
        colDisponibles.setCellValueFactory(c -> str(Long.toString(c.getValue().disponibles())));
        colLocalidadEmitidos.setCellValueFactory(c -> str(Long.toString(c.getValue().emitidos())));
        colLocalidadIngresos.setCellValueFactory(c -> str(Long.toString(c.getValue().ingresos())));
        colLocalidadAnulados.setCellValueFactory(c -> str(Long.toString(c.getValue().cancelados())));
        colLocalidadNetos.setCellValueFactory(c -> str(q(c.getValue().netos())));

        cmbConciertos.getItems().add(new OpcionConcierto(0, "Todos los conciertos"));
        cmbConciertos.getSelectionModel().selectFirst();
        cmbConciertos.valueProperty().addListener((obs, previo, actual) -> mostrar());
        graficaIngresos.setLegendVisible(false);
        graficaIngresos.setAnimated(false);
        graficaEstados.setAnimated(false);
        graficaEstados.setLabelsVisible(true);
        lblEstado.setText("Preparando reportes...");
        actualizarControles();
        // FXMLLoader llama initialize al abrir la vista, sin bloquear el hilo JavaFX.
        actualizar();
    }

    @FXML
    private void actualizar() {
        if (cargando || exportando) return;
        cargando = true;
        lblEstado.setText("Consultando estadísticas en Neon...");
        actualizarControles();
        Task<Datos> tarea = new Task<>() {
            @Override protected Datos call() throws SQLException { return dao.cargar(); }
        };
        tarea.setOnSucceeded(e -> {
            cargando = false;
            datos = tarea.getValue();
            int idAnterior = idSeleccionado();
            poblarCombo(idAnterior);
            mostrar();
            lblActualizacion.setText("Actualizado: " + LocalDateTime.now()
                    .format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
            lblEstado.setText("Datos cargados desde Neon. Listo para exportar.");
            actualizarControles();
        });
        tarea.setOnFailed(e -> {
            cargando = false;
            lblEstado.setText("No fue posible cargar las vistas: " + error(tarea.getException()));
            actualizarControles();
            alerta(Alert.AlertType.ERROR, "Error al consultar reportes",
                    "Comprueba que ejecutaste 01_vistas_reportes.sql en Neon.\n\n"
                    + error(tarea.getException()));
        });
        ejecutar(tarea, "svb-gua-reportes-consulta");
    }

    private void poblarCombo(int conservarId) {
        List<OpcionConcierto> opciones = new ArrayList<>();
        opciones.add(new OpcionConcierto(0, "Todos los conciertos"));
        for (Concierto c : datos.conciertos()) {
            opciones.add(new OpcionConcierto(c.idConcierto(), c.nombre()));
        }
        cmbConciertos.getItems().setAll(opciones);
        cmbConciertos.getSelectionModel().select(opciones.stream()
                .filter(o -> o.id() == conservarId).findFirst().orElse(opciones.get(0)));
    }

    private int idSeleccionado() {
        OpcionConcierto opcion = cmbConciertos.getValue();
        return opcion == null ? 0 : opcion.id();
    }

    private void mostrar() {
        if (datos == null) return;
        int id = idSeleccionado();
        Resumen r = ReportesDAO.resumir(datos, id);
        lblNeto.setText(q(r.netos()));
        lblDevoluciones.setText(q(r.devoluciones()));
        lblHistorico.setText(q(r.historicos()));
        lblEmitidos.setText(String.format("%,d", r.emitidos()));
        lblAccesos.setText(String.format("%,d", r.ingresos()));
        lblCancelados.setText(String.format("%,d", r.cancelados()));
        lblSinUtilizar.setText(String.format("%,d", r.sinUtilizar()));
        lblDisponibles.setText(String.format("%,d", r.disponibles()));
        lblVentas.setText(String.format("%,d", r.ventas()));
        lblFiltro.setText(id <= 0 ? "Vista general · todos los conciertos"
                : "Vista del concierto: " + cmbConciertos.getValue().nombre());

        List<Concierto> conciertos = conciertosFiltrados();
        List<Localidad> localidades = localidadesFiltradas();
        tablaConciertos.getItems().setAll(conciertos);
        tablaLocalidades.getItems().setAll(localidades);

        // Gráfico de ingresos netos: hasta 8 conciertos con mayor monto.
        List<Concierto> principales = conciertos.stream()
                .sorted(Comparator.comparing(Concierto::netos).reversed())
                .limit(8).toList();
        XYChart.Series<String, Number> serie = new XYChart.Series<>();
        for (Concierto c : principales) {
            String nombre = c.nombre();
            if (nombre.length() > 20) nombre = nombre.substring(0, 19) + "…";
            serie.getData().add(new XYChart.Data<>(nombre, c.netos().doubleValue()));
        }
        graficaIngresos.getData().clear();
        if (!serie.getData().isEmpty()) graficaIngresos.getData().add(serie);

        long otros = Math.max(0, r.emitidos() - r.sinUtilizar()
                - r.ingresos() - r.cancelados());
        List<PieChart.Data> sectores = new ArrayList<>();
        if (r.sinUtilizar() > 0) sectores.add(new PieChart.Data("Sin utilizar: " + r.sinUtilizar(), r.sinUtilizar()));
        if (r.ingresos() > 0) sectores.add(new PieChart.Data("Ingresaron: " + r.ingresos(), r.ingresos()));
        if (r.cancelados() > 0) sectores.add(new PieChart.Data("Anulados: " + r.cancelados(), r.cancelados()));
        if (otros > 0) sectores.add(new PieChart.Data("Otros: " + otros, otros));
        graficaEstados.setData(FXCollections.observableArrayList(sectores));
    }

    private List<Concierto> conciertosFiltrados() {
        if (datos == null) return List.of();
        int id = idSeleccionado();
        return datos.conciertos().stream().filter(c -> id <= 0 || c.idConcierto() == id).toList();
    }

    private List<Localidad> localidadesFiltradas() {
        if (datos == null) return List.of();
        int id = idSeleccionado();
        return datos.localidades().stream().filter(c -> id <= 0 || c.idConcierto() == id).toList();
    }

    @FXML
    private void exportarPDF() {
        if (datos == null || cargando || exportando) return;
        FileChooser selector = new FileChooser();
        selector.setTitle("Guardar reporte SVB-GUA");
        selector.setInitialFileName("SVB-GUA-Reporte-" + LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmm")) + ".pdf");
        selector.getExtensionFilters().add(new FileChooser.ExtensionFilter("Archivo PDF", "*.pdf"));
        Window ventana = btnExportar.getScene().getWindow();
        File elegido = selector.showSaveDialog(ventana);
        if (elegido == null) return;
        String ruta = elegido.getAbsolutePath().toLowerCase(Locale.ROOT).endsWith(".pdf")
                ? elegido.getAbsolutePath() : elegido.getAbsolutePath() + ".pdf";
        File destino = new File(ruta);
        Resumen resumen = ReportesDAO.resumir(datos, idSeleccionado());
        List<Concierto> conciertos = List.copyOf(conciertosFiltrados());
        List<Localidad> localidades = List.copyOf(localidadesFiltradas());
        String filtro = cmbConciertos.getValue() == null
                ? "Todos los conciertos" : cmbConciertos.getValue().nombre();
        exportando = true;
        lblEstado.setText("Generando reporte PDF...");
        actualizarControles();
        Task<Void> tarea = new Task<>() {
            @Override protected Void call() throws Exception {
                pdf.exportar(destino, filtro, resumen, conciertos, localidades);
                return null;
            }
        };
        tarea.setOnSucceeded(e -> {
            exportando = false;
            actualizarControles();
            lblEstado.setText("PDF guardado: " + destino.getName());
            alerta(Alert.AlertType.INFORMATION, "Reporte exportado",
                    "Archivo guardado correctamente:\n" + destino.getAbsolutePath());
        });
        tarea.setOnFailed(e -> {
            exportando = false;
            actualizarControles();
            lblEstado.setText("Error al exportar: " + error(tarea.getException()));
            alerta(Alert.AlertType.ERROR, "Error al exportar PDF", error(tarea.getException()));
        });
        ejecutar(tarea, "svb-gua-reportes-pdf");
    }

    private void actualizarControles() {
        btnActualizar.setDisable(cargando || exportando);
        btnExportar.setDisable(datos == null || cargando || exportando);
        cmbConciertos.setDisable(cargando || exportando);
    }

    private static void ejecutar(Task<?> tarea, String nombre) {
        Thread hilo = new Thread(tarea, nombre);
        hilo.setDaemon(true);
        hilo.start();
    }

    private static ReadOnlyStringWrapper str(String texto) {
        return new ReadOnlyStringWrapper(texto == null ? "" : texto);
    }

    private static String q(BigDecimal valor) {
        DecimalFormat df = new DecimalFormat("#,##0.00",
                DecimalFormatSymbols.getInstance(Locale.US));
        return "Q " + df.format(valor == null ? BigDecimal.ZERO : valor);
    }

    private static String fechaCorta(String fecha) {
        if (fecha == null) return "";
        return fecha.length() >= 16 ? fecha.substring(0, 16) : fecha;
    }

    private static String error(Throwable t) {
        Throwable causa = t;
        while (causa != null && causa.getCause() != null) causa = causa.getCause();
        return causa == null ? "Error desconocido" : String.valueOf(causa.getMessage());
    }

    private void alerta(Alert.AlertType tipo, String titulo, String mensaje) {
        Alert aviso = new Alert(tipo);
        aviso.setTitle("SVB-GUA");
        aviso.setHeaderText(titulo);
        aviso.setContentText(mensaje);
        aviso.showAndWait();
    }
}
