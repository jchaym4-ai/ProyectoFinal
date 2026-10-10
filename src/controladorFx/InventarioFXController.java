
package controladorFx;

import dao.ConciertoDAO;
import dao.LocalidadDAO;
import dao.InventarioBoletoDAO;

import modelo.Concierto;
import modelo.Localidad;
import modelo.InventarioBoleto;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;

import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.util.StringConverter;

public class InventarioFXController {

    @FXML private ComboBox<Concierto> cmbConcierto;
    @FXML private ComboBox<Localidad> cmbLocalidad;
    @FXML private ComboBox<String> cmbTipoVenta;

    @FXML private TextField txtPrecio;
    @FXML private TextField txtCantidadTotal;
    @FXML private TextField txtCantidadDisponible;
    @FXML private TextField txtBuscar;

    @FXML private TableView<InventarioBoleto> tablaInventario;

    @FXML private TableColumn<InventarioBoleto, Integer> colIdInventario;
    @FXML private TableColumn<InventarioBoleto, String> colConcierto;
    @FXML private TableColumn<InventarioBoleto, String> colLocalidad;
    @FXML private TableColumn<InventarioBoleto, String> colPrecio;
    @FXML private TableColumn<InventarioBoleto, Integer> colCantidadTotal;
    @FXML private TableColumn<InventarioBoleto, Integer> colCantidadDisponible;
    @FXML private TableColumn<InventarioBoleto, String> colTipoVenta;

    private final InventarioBoletoDAO inventarioDAO =
            new InventarioBoletoDAO();

    private final ConciertoDAO conciertoDAO =
            new ConciertoDAO();

    private final LocalidadDAO localidadDAO =
            new LocalidadDAO();

    private final ObservableList<InventarioBoleto> inventarios =
            FXCollections.observableArrayList();

    @FXML
    private void initialize() {

        configurarCombos();
        configurarTabla();
        configurarBusqueda();
        configurarSeleccion();

        txtCantidadDisponible.setEditable(false);

        txtCantidadTotal.textProperty().addListener(
                (obs, anterior, nuevo) ->
                        calcularDisponible()
        );

        cargarCatalogos();
        cargarTabla();
    }

    private void configurarCombos() {

        cmbTipoVenta.setItems(
                FXCollections.observableArrayList(
                        "aforo",
                        "numerado"
                )
        );

        cmbTipoVenta.getSelectionModel().select("aforo");

        cmbConcierto.setConverter(
                new StringConverter<Concierto>() {

                    @Override
                    public String toString(Concierto concierto) {
                        if (concierto == null) {
                            return "";
                        }

                        return concierto.getIdConcierto()
                                + " - "
                                + concierto.getTituloEvento();
                    }

                    @Override
                    public Concierto fromString(String texto) {
                        return null;
                    }
                }
        );

        cmbLocalidad.setConverter(
                new StringConverter<Localidad>() {

                    @Override
                    public String toString(Localidad localidad) {
                        if (localidad == null) {
                            return "";
                        }

                        return localidad.getIdLocalidad()
                                + " - "
                                + localidad.getNombre();
                    }

                    @Override
                    public Localidad fromString(String texto) {
                        return null;
                    }
                }
        );
    }

    private void cargarCatalogos() {

        cmbConcierto.setItems(
                FXCollections.observableArrayList(
                        conciertoDAO.listarTodos()
                )
        );

        cmbLocalidad.setItems(
                FXCollections.observableArrayList(
                        localidadDAO.listarTodos()
                )
        );
    }

    private String nombreConcierto(int id) {

        for (Concierto concierto : cmbConcierto.getItems()) {

            if (concierto.getIdConcierto() == id) {
                return concierto.getTituloEvento();
            }
        }

        return "Concierto #" + id;
    }

    private String nombreLocalidad(int id) {

        for (Localidad localidad : cmbLocalidad.getItems()) {

            if (localidad.getIdLocalidad() == id) {
                return localidad.getNombre();
            }
        }

        return "Localidad #" + id;
    }

    private void configurarTabla() {

        colIdInventario.setCellValueFactory(dato ->
                new ReadOnlyObjectWrapper<>(
                        dato.getValue().getIdInventario()
                )
        );

        colConcierto.setCellValueFactory(dato ->
                new ReadOnlyStringWrapper(
                        nombreConcierto(
                                dato.getValue().getIdConcierto()
                        )
                )
        );

        colLocalidad.setCellValueFactory(dato ->
                new ReadOnlyStringWrapper(
                        nombreLocalidad(
                                dato.getValue().getIdLocalidad()
                        )
                )
        );

        colPrecio.setCellValueFactory(dato ->
                new ReadOnlyStringWrapper(
                        "Q " + dato.getValue()
                                .getPrecio().toPlainString()
                )
        );

        colCantidadTotal.setCellValueFactory(dato ->
                new ReadOnlyObjectWrapper<>(
                        dato.getValue().getCantidadTotal()
                )
        );

        colCantidadDisponible.setCellValueFactory(dato ->
                new ReadOnlyObjectWrapper<>(
                        dato.getValue().getCantidadDisponible()
                )
        );

        colTipoVenta.setCellValueFactory(dato ->
                new ReadOnlyStringWrapper(
                        dato.getValue().getTipoVenta()
                )
        );
    }

    private void configurarBusqueda() {

        FilteredList<InventarioBoleto> filtrados =
                new FilteredList<>(inventarios, i -> true);

        txtBuscar.textProperty().addListener(
                (obs, anterior, nuevo) -> {

                    String busqueda = normalizar(nuevo);

                    filtrados.setPredicate(inventario -> {

                        if (busqueda.isEmpty()) {
                            return true;
                        }

                        return normalizar(
                                nombreConcierto(
                                        inventario.getIdConcierto()
                                )
                        ).contains(busqueda)

                        || normalizar(
                                nombreLocalidad(
                                        inventario.getIdLocalidad()
                                )
                        ).contains(busqueda)

                        || normalizar(
                                inventario.getTipoVenta()
                        ).contains(busqueda)

                        || String.valueOf(
                                inventario.getIdInventario()
                        ).contains(busqueda);
                    });
                }
        );

        SortedList<InventarioBoleto> ordenados =
                new SortedList<>(filtrados);

        ordenados.comparatorProperty().bind(
                tablaInventario.comparatorProperty()
        );

        tablaInventario.setItems(ordenados);
    }

    private String normalizar(String texto) {

        return texto == null
                ? ""
                : texto.trim().toLowerCase(Locale.ROOT);
    }

    private void configurarSeleccion() {

        tablaInventario.getSelectionModel()
                .selectedItemProperty()
                .addListener((obs, anterior, seleccionado) -> {

                    if (seleccionado == null) {

                        cmbConcierto.setDisable(false);
                        cmbLocalidad.setDisable(false);
                        cmbTipoVenta.setDisable(false);
                        return;
                    }

                    for (Concierto concierto : cmbConcierto.getItems()) {

                        if (concierto.getIdConcierto()
                                == seleccionado.getIdConcierto()) {

                            cmbConcierto.getSelectionModel()
                                    .select(concierto);
                            break;
                        }
                    }

                    for (Localidad localidad : cmbLocalidad.getItems()) {

                        if (localidad.getIdLocalidad()
                                == seleccionado.getIdLocalidad()) {

                            cmbLocalidad.getSelectionModel()
                                    .select(localidad);
                            break;
                        }
                    }

                    cmbTipoVenta.getSelectionModel().select(
                            seleccionado.getTipoVenta()
                    );

                    txtPrecio.setText(
                            seleccionado.getPrecio().toPlainString()
                    );

                    txtCantidadTotal.setText(
                            String.valueOf(
                                    seleccionado.getCantidadTotal()
                            )
                    );

                    txtCantidadDisponible.setText(
                            String.valueOf(
                                    seleccionado.getCantidadDisponible()
                            )
                    );

                    cmbConcierto.setDisable(true);
                    cmbLocalidad.setDisable(true);
                    cmbTipoVenta.setDisable(true);
                });
    }

    private void cargarTabla() {

        inventarios.setAll(
                inventarioDAO.listarTodos()
        );
    }

    private void calcularDisponible() {

        try {

            int total = Integer.parseInt(
                    txtCantidadTotal.getText().trim()
            );

            if (total < 0) {
                txtCantidadDisponible.clear();
                return;
            }

            InventarioBoleto seleccionado =
                    tablaInventario.getSelectionModel()
                            .getSelectedItem();

            if (seleccionado == null) {

                txtCantidadDisponible.setText(
                        String.valueOf(total)
                );

                return;
            }

            long vendidos =
                    (long) seleccionado.getCantidadTotal()
                    - seleccionado.getCantidadDisponible();

            long disponible = total - vendidos;

            if (disponible < 0) {
                txtCantidadDisponible.clear();
            } else {
                txtCantidadDisponible.setText(
                        String.valueOf(disponible)
                );
            }

        } catch (NumberFormatException e) {

            txtCantidadDisponible.clear();
        }
    }

    private InventarioBoleto obtenerDatosFormulario() {

        Concierto concierto = cmbConcierto.getValue();
        Localidad localidad = cmbLocalidad.getValue();
        String tipo = cmbTipoVenta.getValue();

        if (concierto == null || localidad == null || tipo == null) {

            mostrarMensaje(
                    Alert.AlertType.WARNING,
                    "Seleccione concierto, localidad y tipo de venta."
            );

            return null;
        }

        BigDecimal precio;

        try {

            precio = new BigDecimal(
                    txtPrecio.getText().trim()
            ).setScale(2, RoundingMode.UNNECESSARY);

        } catch (NumberFormatException | ArithmeticException e) {

            mostrarMensaje(
                    Alert.AlertType.WARNING,
                    "Ingrese un precio válido con máximo dos decimales."
            );

            return null;
        }

        if (precio.signum() < 0
                || precio.compareTo(
                        new BigDecimal("99999999.99")
                ) > 0) {

            mostrarMensaje(
                    Alert.AlertType.WARNING,
                    "El precio debe estar entre Q 0.00 y Q 99,999,999.99."
            );

            return null;
        }

        int total;

        try {

            total = Integer.parseInt(
                    txtCantidadTotal.getText().trim()
            );

        } catch (NumberFormatException e) {

            mostrarMensaje(
                    Alert.AlertType.WARNING,
                    "Ingrese una cantidad total válida."
            );

            return null;
        }

        if (total < 0) {

            mostrarMensaje(
                    Alert.AlertType.WARNING,
                    "La cantidad total no puede ser negativa."
            );

            return null;
        }

        InventarioBoleto inventario =
                new InventarioBoleto();

        inventario.setIdConcierto(
                concierto.getIdConcierto()
        );

        inventario.setIdLocalidad(
                localidad.getIdLocalidad()
        );

        inventario.setPrecio(precio);
        inventario.setCantidadTotal(total);
        inventario.setCantidadDisponible(total);
        inventario.setTipoVenta(tipo);

        return inventario;
    }

    @FXML
    private void guardar() {

        if (tablaInventario.getSelectionModel()
                .getSelectedItem() != null) {

            mostrarMensaje(
                    Alert.AlertType.WARNING,
                    "Presione Limpiar antes de registrar un nuevo inventario."
            );

            return;
        }

        InventarioBoleto nuevo = obtenerDatosFormulario();

        if (nuevo == null) {
            return;
        }

        for (InventarioBoleto existente : inventarios) {

            if (existente.getIdConcierto() == nuevo.getIdConcierto()
                    && existente.getIdLocalidad() == nuevo.getIdLocalidad()) {

                mostrarMensaje(
                        Alert.AlertType.WARNING,
                        "Ya existe inventario para ese concierto y localidad."
                );

                return;
            }
        }

        if (inventarioDAO.insertar(nuevo)) {

            mostrarMensaje(
                    Alert.AlertType.INFORMATION,
                    "Inventario registrado correctamente."
            );

            limpiar();
            cargarTabla();

        } else {

            mostrarMensaje(
                    Alert.AlertType.ERROR,
                    "No se pudo registrar el inventario."
            );
        }
    }

    @FXML
    private void actualizar() {

        InventarioBoleto original =
                tablaInventario.getSelectionModel()
                        .getSelectedItem();

        if (original == null) {

            mostrarMensaje(
                    Alert.AlertType.WARNING,
                    "Seleccione un inventario."
            );

            return;
        }

        InventarioBoleto nuevo = obtenerDatosFormulario();

        if (nuevo == null) {
            return;
        }

        boolean sinCambios =
                original.getPrecio().compareTo(nuevo.getPrecio()) == 0
                && original.getCantidadTotal() == nuevo.getCantidadTotal();

        if (sinCambios) {

            mostrarMensaje(
                    Alert.AlertType.INFORMATION,
                    "No se detectaron cambios en el inventario."
            );

            return;
        }

        long vendidos =
                (long) original.getCantidadTotal()
                - original.getCantidadDisponible();

        if (vendidos < 0) {

            mostrarMensaje(
                    Alert.AlertType.ERROR,
                    "El inventario actual tiene cantidades inconsistentes."
            );

            return;
        }

        if (nuevo.getCantidadTotal() < vendidos) {

            mostrarMensaje(
                    Alert.AlertType.WARNING,
                    "No puede establecer una cantidad menor "
                    + "a los boletos ya vendidos."
            );

            return;
        }

        nuevo.setIdInventario(
                original.getIdInventario()
        );

        nuevo.setCantidadDisponible(
                (int) (nuevo.getCantidadTotal() - vendidos)
        );

        if (inventarioDAO.actualizar(nuevo)) {

            mostrarMensaje(
                    Alert.AlertType.INFORMATION,
                    "Inventario actualizado correctamente."
            );

            limpiar();
            cargarTabla();

        } else {

            mostrarMensaje(
                    Alert.AlertType.ERROR,
                    "No se pudo actualizar el inventario."
            );
        }
    }

    @FXML
    private void eliminar() {

        InventarioBoleto seleccionado =
                tablaInventario.getSelectionModel()
                        .getSelectedItem();

        if (seleccionado == null) {

            mostrarMensaje(
                    Alert.AlertType.WARNING,
                    "Seleccione un inventario."
            );

            return;
        }

        long vendidos =
                (long) seleccionado.getCantidadTotal()
                - seleccionado.getCantidadDisponible();

        if (vendidos > 0) {

            mostrarMensaje(
                    Alert.AlertType.WARNING,
                    "No puede eliminar un inventario con boletos vendidos."
            );

            return;
        }

        Alert confirmacion = new Alert(
                Alert.AlertType.CONFIRMATION,
                "¿Desea eliminar este inventario? "
                + "También se eliminarán los asientos y datos del mapa "
                + "que dependan de este registro.",
                ButtonType.YES,
                ButtonType.NO
        );

        confirmacion.setTitle("Confirmar eliminación");
        confirmacion.setHeaderText(null);

        if (confirmacion.showAndWait()
                .orElse(ButtonType.NO) != ButtonType.YES) {
            return;
        }

        if (inventarioDAO.eliminar(
                seleccionado.getIdInventario()
        )) {

            mostrarMensaje(
                    Alert.AlertType.INFORMATION,
                    "Inventario eliminado correctamente."
            );

            limpiar();
            cargarTabla();

        } else {

            mostrarMensaje(
                    Alert.AlertType.ERROR,
                    "No se pudo eliminar el inventario. "
                    + "Verifique si tiene ventas relacionadas."
            );
        }
    }

    @FXML
    private void limpiar() {

        tablaInventario.getSelectionModel().clearSelection();

        cmbConcierto.setDisable(false);
        cmbLocalidad.setDisable(false);
        cmbTipoVenta.setDisable(false);

        cmbConcierto.getSelectionModel().clearSelection();
        cmbLocalidad.getSelectionModel().clearSelection();
        cmbTipoVenta.getSelectionModel().select("aforo");

        txtPrecio.clear();
        txtCantidadTotal.clear();
        txtCantidadDisponible.clear();
    }

    private void mostrarMensaje(
            Alert.AlertType tipo,
            String mensaje
    ) {

        Alert alerta = new Alert(tipo);

        alerta.setTitle("Gestión de Inventario");
        alerta.setHeaderText(null);
        alerta.setContentText(mensaje);
        alerta.showAndWait();
    }
}
