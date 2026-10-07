package controladorFx;

import dao.LocalidadDAO;
import java.util.List;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import modelo.Localidad;

public class LocalidadesFXController {

    @FXML
    private TextField txtNombre;

    @FXML
    private TableView<Localidad> tblLocalidades;

    @FXML
    private TableColumn<Localidad, Integer> colId;

    @FXML
    private TableColumn<Localidad, String> colNombre;

    @FXML
    private Button btnGuardar;

    @FXML
    private Button btnActualizar;

    @FXML
    private Button btnEliminar;

    @FXML
    private Button btnLimpiar;

    private final LocalidadDAO localidadDAO;

    private int idSeleccionado = -1;

    public LocalidadesFXController() {
        this.localidadDAO = new LocalidadDAO();
    }

    @FXML
    private void initialize() {

        colId.setCellValueFactory(
                new PropertyValueFactory<>("idLocalidad")
        );

        colNombre.setCellValueFactory(
                new PropertyValueFactory<>("nombre")
        );

        tblLocalidades
                .getSelectionModel()
                .selectedItemProperty()
                .addListener((observable, anterior, seleccionada) -> {

                    if (seleccionada != null) {
                        seleccionarFila(seleccionada);
                    }
                });

        cargarTabla();
    }

    @FXML
    private void guardar() {

        String nombre = txtNombre.getText().trim();

        if (nombre.isEmpty()) {

            mostrarAlerta(
                    Alert.AlertType.WARNING,
                    "Campo obligatorio",
                    "El nombre de la localidad es obligatorio."
            );

            return;
        }

        Localidad localidad = new Localidad();

        localidad.setNombre(nombre);

        if (localidadDAO.insertar(localidad)) {

            mostrarAlerta(
                    Alert.AlertType.INFORMATION,
                    "Registro exitoso",
                    "Localidad registrada correctamente."
            );

            limpiar();
            cargarTabla();

        } else {

            mostrarAlerta(
                    Alert.AlertType.ERROR,
                    "Error",
                    "No se pudo registrar la localidad."
            );
        }
    }

    @FXML
    private void actualizar() {

        if (idSeleccionado == -1) {

            mostrarAlerta(
                    Alert.AlertType.WARNING,
                    "Selección requerida",
                    "Seleccione una localidad."
            );

            return;
        }

        String nombre = txtNombre.getText().trim();

        if (nombre.isEmpty()) {

            mostrarAlerta(
                    Alert.AlertType.WARNING,
                    "Campo obligatorio",
                    "El nombre de la localidad es obligatorio."
            );

            return;
        }

        Localidad localidad = new Localidad();

        localidad.setIdLocalidad(idSeleccionado);
        localidad.setNombre(nombre);

        if (localidadDAO.actualizar(localidad)) {

            mostrarAlerta(
                    Alert.AlertType.INFORMATION,
                    "Actualización exitosa",
                    "Localidad actualizada correctamente."
            );

            limpiar();
            cargarTabla();

        } else {

            mostrarAlerta(
                    Alert.AlertType.ERROR,
                    "Error",
                    "No se pudo actualizar la localidad."
            );
        }
    }

    @FXML
    private void eliminar() {

        if (idSeleccionado == -1) {

            mostrarAlerta(
                    Alert.AlertType.WARNING,
                    "Selección requerida",
                    "Seleccione una localidad."
            );

            return;
        }

        Alert confirmacion = new Alert(
                Alert.AlertType.CONFIRMATION
        );

        confirmacion.setTitle("Confirmar eliminación");
        confirmacion.setHeaderText(null);
        confirmacion.setContentText(
                "¿Desea eliminar esta localidad?"
        );

        confirmacion.getButtonTypes().setAll(
                ButtonType.YES,
                ButtonType.NO
        );

        ButtonType respuesta = confirmacion
                .showAndWait()
                .orElse(ButtonType.NO);

        if (respuesta != ButtonType.YES) {
            return;
        }

        if (localidadDAO.eliminar(idSeleccionado)) {

            mostrarAlerta(
                    Alert.AlertType.INFORMATION,
                    "Eliminación exitosa",
                    "Localidad eliminada correctamente."
            );

            limpiar();
            cargarTabla();

        } else {

            mostrarAlerta(
                    Alert.AlertType.ERROR,
                    "Error",
                    "No se pudo eliminar la localidad."
            );
        }
    }

    @FXML
    private void limpiar() {

        idSeleccionado = -1;

        txtNombre.clear();

        tblLocalidades
                .getSelectionModel()
                .clearSelection();
    }

    private void cargarTabla() {

        List<Localidad> localidades =
                localidadDAO.listarTodos();

        tblLocalidades.setItems(
                FXCollections.observableArrayList(
                        localidades
                )
        );
    }

    private void seleccionarFila(Localidad localidad) {

        idSeleccionado =
                localidad.getIdLocalidad();

        txtNombre.setText(
                localidad.getNombre()
        );
    }

    private void mostrarAlerta(
            Alert.AlertType tipo,
            String titulo,
            String mensaje
    ) {

        Alert alerta = new Alert(tipo);

        alerta.setTitle(titulo);
        alerta.setHeaderText(null);
        alerta.setContentText(mensaje);

        alerta.showAndWait();
    }
}