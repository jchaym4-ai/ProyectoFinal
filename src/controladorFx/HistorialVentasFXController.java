package controladorFx;

import dao.HistorialVentasDAO;
import modelo.HistorialVenta;
import modelo.DetalleHistorialVenta;
import modelo.Usuario;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

import javafx.beans.property.SimpleStringProperty;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;

public class HistorialVentasFXController {

    // ==========================================
    // BUSCADOR Y FILTROS
    // ==========================================
    @FXML
    private TextField txtBuscar;

    @FXML
    private DatePicker dpFechaDesde;

    @FXML
    private DatePicker dpFechaHasta;

    @FXML
    private Button btnBuscar;

    @FXML
    private Button btnLimpiar;

    @FXML
    private Button btnVerBoletos;

    // ==========================================
    // TABLA PRINCIPAL DE VENTAS
    // ==========================================
    @FXML
    private TableView<HistorialVenta> tablaHistorial;

    @FXML
    private TableColumn<HistorialVenta, String> colIdVenta;

    @FXML
    private TableColumn<HistorialVenta, String> colFecha;

    @FXML
    private TableColumn<HistorialVenta, String> colVendedor;

    @FXML
    private TableColumn<HistorialVenta, String> colCantidadBoletos;

    @FXML
    private TableColumn<HistorialVenta, String> colTotal;

    // ==========================================
    // TABLA INFERIOR: DETALLES DE LA VENTA
    // ==========================================
    @FXML
    private TableView<DetalleHistorialVenta> tablaDetalleVenta;

    @FXML
    private TableColumn<DetalleHistorialVenta, String> colConcierto;

    @FXML
    private TableColumn<DetalleHistorialVenta, String> colLocalidad;

    @FXML
    private TableColumn<DetalleHistorialVenta, String> colCantidadDetalle;

    @FXML
    private TableColumn<DetalleHistorialVenta, String> colPrecioUnitario;

    @FXML
    private TableColumn<DetalleHistorialVenta, String> colSubtotal;

    // ==========================================
    // ETIQUETAS DE INFORMACIÓN
    // ==========================================
    @FXML
    private Label lblCantidadResultados;

    @FXML
    private Label lblEstadoHistorial;

    @FXML
    private Label lblDetalleVenta;

    @FXML
    private Label lblDetalleVendedor;

    @FXML
    private Label lblDetalleTotal;

    // ==========================================
    // USUARIO Y DAO
    // ==========================================
    private Usuario usuario;

    private final HistorialVentasDAO dao
            = new HistorialVentasDAO();

    private final DateTimeFormatter formatoFecha
            = DateTimeFormatter.ofPattern(
                    "dd/MM/yyyy HH:mm"
            );

    // Control para evitar resultados antiguos.
    private long versionBusqueda = 0;
    private long versionDetalle = 0;

    // ==========================================
    // INICIALIZAR PANTALLA
    // ==========================================
    @FXML
    private void initialize() {

        configurarTabla();
        configurarTablaDetalles();

        btnBuscar.setDisable(true);
        btnLimpiar.setDisable(true);
        btnVerBoletos.setDisable(true);

        // BOTÓN BUSCAR
        btnBuscar.setOnAction(
                e -> buscarVentas()
        );

        // BOTÓN LIMPIAR FILTROS
        btnLimpiar.setOnAction(e -> {

            txtBuscar.clear();

            dpFechaDesde.setValue(null);
            dpFechaHasta.setValue(null);

            buscarVentas();
        });

        // BUSCAR CON ENTER
        txtBuscar.setOnAction(
                e -> buscarVentas()
        );

        // AL SELECCIONAR UNA VENTA
        tablaHistorial.getSelectionModel()
                .selectedItemProperty()
                .addListener(
                        (obs, anterior, seleccionada) -> {

                            mostrarResumen(seleccionada);

                            cargarDetallesVenta(
                                    seleccionada
                            );
                        }
                );

        mostrarResumen(null);

        lblCantidadResultados.setText("0 ventas");

        lblEstadoHistorial.setText(
                "Esperando vendedor autenticado..."
        );
    }

    // ==========================================
    // CONFIGURAR TABLA PRINCIPAL
    // ==========================================
    private void configurarTabla() {

        // NÚMERO DE VENTA
        colIdVenta.setCellValueFactory(d
                -> new SimpleStringProperty(
                        String.valueOf(
                                d.getValue().getIdVenta()
                        )
                )
        );

        // FECHA
        colFecha.setCellValueFactory(d -> {

            HistorialVenta venta = d.getValue();

            String fecha = venta.getFechaVenta() == null
                    ? "Sin fecha"
                    : venta.getFechaVenta()
                            .format(formatoFecha);

            return new SimpleStringProperty(fecha);
        });

        // VENDEDOR
        colVendedor.setCellValueFactory(d
                -> new SimpleStringProperty(
                        d.getValue().getVendedor()
                )
        );

        // BOLETOS VENDIDOS
        colCantidadBoletos.setCellValueFactory(d
                -> new SimpleStringProperty(
                        String.valueOf(
                                d.getValue()
                                        .getCantidadBoletos()
                        )
                )
        );

        // TOTAL DE LA VENTA
        colTotal.setCellValueFactory(d
                -> new SimpleStringProperty(
                        formatoDinero(
                                d.getValue().getTotalVenta()
                        )
                )
        );

        tablaHistorial.setPlaceholder(
                new Label(
                        "No hay ventas que mostrar."
                )
        );
    }

    // ==========================================
    // CONFIGURAR TABLA INFERIOR
    // ==========================================
    private void configurarTablaDetalles() {

        // CONCIERTO
        colConcierto.setCellValueFactory(d
                -> new SimpleStringProperty(
                        d.getValue().getConcierto()
                )
        );

        // LOCALIDAD
        colLocalidad.setCellValueFactory(d
                -> new SimpleStringProperty(
                        d.getValue().getLocalidad()
                )
        );

        // CANTIDAD
        colCantidadDetalle.setCellValueFactory(d
                -> new SimpleStringProperty(
                        String.valueOf(
                                d.getValue().getCantidad()
                        )
                )
        );

        // PRECIO UNITARIO
        colPrecioUnitario.setCellValueFactory(d
                -> new SimpleStringProperty(
                        formatoDinero(
                                d.getValue().getPrecioUnitario()
                        )
                )
        );

        // SUBTOTAL
        colSubtotal.setCellValueFactory(d
                -> new SimpleStringProperty(
                        formatoDinero(
                                d.getValue().getSubtotal()
                        )
                )
        );

        tablaDetalleVenta.setPlaceholder(
                new Label(
                        "Selecciona una venta para ver sus artículos."
                )
        );
    }

    // ==========================================
    // FORMATO DE DINERO
    // ==========================================
    private String formatoDinero(
            BigDecimal cantidad) {

        if (cantidad == null) {
            return "Q 0.00";
        }

        return String.format(
                Locale.US,
                "Q %,.2f",
                cantidad
        );
    }

    // ==========================================
    // RECIBIR USUARIO AUTENTICADO
    // ==========================================
    public void setUsuario(Usuario usuario) {

        if (usuario == null
                || usuario.getIdUsuario() <= 0) {

            throw new IllegalArgumentException(
                    "Se necesita un vendedor válido."
            );
        }

        this.usuario = usuario;

        buscarVentas();
    }

    // ==========================================
    // MOSTRAR RESUMEN DE LA VENTA
    // ==========================================
    private void mostrarResumen(
            HistorialVenta venta) {

        if (venta == null) {

            lblDetalleVenta.setText("—");
            lblDetalleVendedor.setText("—");
            lblDetalleTotal.setText("—");

            btnVerBoletos.setDisable(true);

            return;
        }

        lblDetalleVenta.setText(
                "#" + venta.getIdVenta()
        );

        lblDetalleVendedor.setText(
                venta.getVendedor()
        );

        lblDetalleTotal.setText(
                formatoDinero(
                        venta.getTotalVenta()
                )
        );

        // Este botón se conectará en el
        // módulo de Consulta de Boletos.
        btnVerBoletos.setDisable(true);
    }

    // ==========================================
    // BUSCAR VENTAS EN POSTGRESQL
    // ==========================================
    private void buscarVentas() {

        if (usuario == null) {

            lblEstadoHistorial.setText(
                    "No hay vendedor autenticado."
            );

            return;
        }

        LocalDate desde
                = dpFechaDesde.getValue();

        LocalDate hasta
                = dpFechaHasta.getValue();

        // VALIDAR FECHAS
        if (desde != null
                && hasta != null
                && desde.isAfter(hasta)) {

            lblEstadoHistorial.setText(
                    "La fecha Desde no puede ser "
                    + "posterior a Hasta."
            );

            return;
        }

        final int idVendedor
                = usuario.getIdUsuario();

        final String texto
                = txtBuscar.getText();

        final long versionActual
                = ++versionBusqueda;

        // Invalidar cualquier detalle anterior.
        versionDetalle++;

        tablaHistorial.getSelectionModel()
                .clearSelection();

        tablaDetalleVenta.getItems().clear();

        btnBuscar.setDisable(true);
        btnLimpiar.setDisable(true);

        lblEstadoHistorial.setText(
                "Consultando ventas en PostgreSQL..."
        );

        Task<List<HistorialVenta>> tarea
                = new Task<>() {

            @Override
            protected List<HistorialVenta> call()
                    throws Exception {

                return dao.buscarVentas(
                        idVendedor,
                        texto,
                        desde,
                        hasta
                );
            }
        };

        // ======================================
        // CONSULTA EXITOSA
        // ======================================
        tarea.setOnSucceeded(e -> {

            if (versionActual != versionBusqueda) {
                return;
            }

            List<HistorialVenta> ventas
                    = tarea.getValue();

            tablaHistorial.getItems().setAll(
                    ventas
            );

            mostrarResumen(null);

            lblCantidadResultados.setText(
                    ventas.size() + " ventas"
            );

            lblEstadoHistorial.setText(
                    "Consulta realizada correctamente."
            );

            btnBuscar.setDisable(false);
            btnLimpiar.setDisable(false);
        });

        // ======================================
        // ERROR EN CONSULTA
        // ======================================
        tarea.setOnFailed(e -> {

            if (versionActual != versionBusqueda) {
                return;
            }

            tablaHistorial.getItems().clear();

            tablaDetalleVenta.getItems().clear();

            mostrarResumen(null);

            lblCantidadResultados.setText(
                    "Error de consulta"
            );

            lblEstadoHistorial.setText(
                    "Error al consultar PostgreSQL."
            );

            tarea.getException().printStackTrace();

            btnBuscar.setDisable(false);
            btnLimpiar.setDisable(false);
        });

        Thread hilo = new Thread(
                tarea,
                "historial-ventas-consulta"
        );

        hilo.setDaemon(true);

        hilo.start();
    }

    // ==========================================
    // CARGAR ARTÍCULOS DE UNA VENTA
    // ==========================================
    private void cargarDetallesVenta(
            HistorialVenta venta) {

        final long versionActual
                = ++versionDetalle;

        tablaDetalleVenta.getItems().clear();

        if (venta == null || usuario == null) {

            tablaDetalleVenta.setPlaceholder(
                    new Label(
                            "Selecciona una venta para ver sus artículos."
                    )
            );

            return;
        }

        final int idVenta
                = venta.getIdVenta();

        final int idVendedor
                = usuario.getIdUsuario();

        tablaDetalleVenta.setPlaceholder(
                new Label(
                        "Cargando artículos..."
                )
        );

        Task<List<DetalleHistorialVenta>> tarea
                = new Task<>() {

            @Override
            protected List<DetalleHistorialVenta> call()
                    throws Exception {

                return dao.obtenerDetallesVenta(
                        idVenta,
                        idVendedor
                );
            }
        };

        // ======================================
        // DETALLES CARGADOS
        // ======================================
        tarea.setOnSucceeded(e -> {

            if (versionActual != versionDetalle) {
                return;
            }

            if (tablaHistorial
                    .getSelectionModel()
                    .getSelectedItem() != venta) {

                return;
            }

            List<DetalleHistorialVenta> detalles
                    = tarea.getValue();

            tablaDetalleVenta.getItems().setAll(
                    detalles
            );

            tablaDetalleVenta.setPlaceholder(
                    new Label(
                            "Esta venta no tiene artículos registrados."
                    )
            );
        });

        // ======================================
        // ERROR AL CARGAR DETALLES
        // ======================================
        tarea.setOnFailed(e -> {

            if (versionActual != versionDetalle) {
                return;
            }

            if (tablaHistorial
                    .getSelectionModel()
                    .getSelectedItem() != venta) {

                return;
            }

            tablaDetalleVenta.getItems().clear();

            tablaDetalleVenta.setPlaceholder(
                    new Label(
                            "Error al cargar los artículos."
                    )
            );

            lblEstadoHistorial.setText(
                    "Error al consultar el detalle de la venta."
            );

            tarea.getException().printStackTrace();
        });

        Thread hilo = new Thread(
                tarea,
                "historial-detalle-venta"
        );

        hilo.setDaemon(true);

        hilo.start();
    }
}
