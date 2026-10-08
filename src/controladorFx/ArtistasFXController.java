package controladorFx;

import dao.ArtistaDAO;
import modelo.Artista;

import java.util.Locale;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;

import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;

import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;

import java.util.Objects;

public class ArtistasFXController {

    @FXML private TextField txtNombreArtistico;
    @FXML private TextField txtGeneroMusical;
    @FXML private TextField txtPaisOrigen;
    @FXML private TextField txtBuscar;

    @FXML private TableView<Artista> tablaArtistas;

    @FXML private TableColumn<Artista, Integer> colId;
    @FXML private TableColumn<Artista, String> colNombreArtistico;
    @FXML private TableColumn<Artista, String> colGeneroMusical;
    @FXML private TableColumn<Artista, String> colPaisOrigen;

    private final ArtistaDAO artistaDAO = new ArtistaDAO();

    private final ObservableList<Artista> artistas =
            FXCollections.observableArrayList();

    private int idSeleccionado = -1;

    @FXML
    private void initialize() {

        // Configurar columnas
        colId.setCellValueFactory(dato ->
                new ReadOnlyObjectWrapper<>(
                        dato.getValue().getIdArtista()
                )
        );

        colNombreArtistico.setCellValueFactory(dato ->
                new ReadOnlyStringWrapper(
                        dato.getValue().getNombreArtistico()
                )
        );

        colGeneroMusical.setCellValueFactory(dato ->
                new ReadOnlyStringWrapper(
                        dato.getValue().getGeneroMusical()
                )
        );

        colPaisOrigen.setCellValueFactory(dato ->
                new ReadOnlyStringWrapper(
                        dato.getValue().getPaisOrigen()
                )
        );

        // Buscar artistas
        FilteredList<Artista> filtrados =
                new FilteredList<>(artistas, a -> true);

        txtBuscar.textProperty().addListener(
                (obs, anterior, nuevo) -> {

                    String busqueda = nuevo == null
                            ? ""
                            : nuevo.trim().toLowerCase(Locale.ROOT);

                    filtrados.setPredicate(artista ->
                            busqueda.isEmpty()
                            || contiene(artista.getNombreArtistico(), busqueda)
                            || contiene(artista.getGeneroMusical(), busqueda)
                            || contiene(artista.getPaisOrigen(), busqueda)
                    );
                }
        );

        SortedList<Artista> ordenados =
                new SortedList<>(filtrados);

        ordenados.comparatorProperty().bind(
                tablaArtistas.comparatorProperty()
        );

        tablaArtistas.setItems(ordenados);

        // Seleccionar artista de la tabla
        tablaArtistas.getSelectionModel()
                .selectedItemProperty()
                .addListener((obs, anterior, seleccionado) -> {

                    if (seleccionado == null) {
                        idSeleccionado = -1;
                        return;
                    }

                    idSeleccionado = seleccionado.getIdArtista();

                    txtNombreArtistico.setText(
                            seleccionado.getNombreArtistico()
                    );

                    txtGeneroMusical.setText(
                            seleccionado.getGeneroMusical()
                    );

                    txtPaisOrigen.setText(
                            seleccionado.getPaisOrigen() == null
                                    ? ""
                                    : seleccionado.getPaisOrigen()
                    );
                });

        cargarTabla();
    }

    private boolean contiene(String valor, String busqueda) {
        return valor != null
                && valor.toLowerCase(Locale.ROOT).contains(busqueda);
    }

    private void cargarTabla() {
        artistas.setAll(artistaDAO.listarTodos());
    }

    @FXML
    private void limpiar() {

        idSeleccionado = -1;

        txtNombreArtistico.clear();
        txtGeneroMusical.clear();
        txtPaisOrigen.clear();

        tablaArtistas.getSelectionModel().clearSelection();
    }

    @FXML
    private void guardar() {

        Artista artista = obtenerDatosFormulario();

        if (artista == null) {
            return;
        }

        if (artistaDAO.insertar(artista)) {
            mostrarMensaje(Alert.AlertType.INFORMATION,
                    "Artista registrado correctamente.");

            limpiar();
            cargarTabla();
        } else {
            mostrarMensaje(Alert.AlertType.ERROR,
                    "No se pudo registrar el artista.");
        }
    }


    @FXML
    private void actualizar() {

        if (idSeleccionado == -1) {
            mostrarMensaje(Alert.AlertType.WARNING,
                    "Seleccione un artista.");
            return;
        }

        Artista artista = obtenerDatosFormulario();

        if (artista == null) {
            return;
        }

        Artista seleccionado =
                tablaArtistas.getSelectionModel().getSelectedItem();

        if (seleccionado == null
                || seleccionado.getIdArtista() != idSeleccionado) {
            mostrarMensaje(Alert.AlertType.WARNING,
                    "Seleccione un artista válido.");
            return;
        }

        // Verificar si realmente existen cambios
        boolean sinCambios =
                Objects.equals(
                        seleccionado.getNombreArtistico(),
                        artista.getNombreArtistico()
                )
                && Objects.equals(
                        seleccionado.getGeneroMusical(),
                        artista.getGeneroMusical()
                )
                && Objects.equals(
                        seleccionado.getPaisOrigen(),
                        artista.getPaisOrigen()
                );

        if (sinCambios) {
            mostrarMensaje(Alert.AlertType.INFORMATION,
                    "No se detectaron cambios en el artista.");
            return;
        }

        artista.setIdArtista(idSeleccionado);

        if (artistaDAO.actualizar(artista)) {

            mostrarMensaje(Alert.AlertType.INFORMATION,
                    "Artista actualizado correctamente.");

            limpiar();
            cargarTabla();

        } else {

            mostrarMensaje(Alert.AlertType.ERROR,
                    "No se pudo actualizar el artista.");
        }
    }


    @FXML
    private void eliminar() {

        if (idSeleccionado == -1) {
            mostrarMensaje(Alert.AlertType.WARNING,
                    "Seleccione un artista.");
            return;
        }

        Alert confirmacion = new Alert(
                Alert.AlertType.CONFIRMATION,
                "¿Desea eliminar este artista?",
                ButtonType.YES,
                ButtonType.NO
        );

        confirmacion.setTitle("Confirmar eliminación");
        confirmacion.setHeaderText(null);

        if (confirmacion.showAndWait()
                .orElse(ButtonType.NO) != ButtonType.YES) {
            return;
        }

        if (artistaDAO.eliminar(idSeleccionado)) {
            mostrarMensaje(Alert.AlertType.INFORMATION,
                    "Artista eliminado correctamente.");

            limpiar();
            cargarTabla();
        } else {
            mostrarMensaje(Alert.AlertType.ERROR,
                    "No se pudo eliminar el artista.");
        }
    }

    private Artista obtenerDatosFormulario() {

        String nombre = txtNombreArtistico.getText().trim();
        String genero = txtGeneroMusical.getText().trim();
        String pais = txtPaisOrigen.getText().trim();

        if (nombre.isEmpty() || genero.isEmpty()) {
            mostrarMensaje(Alert.AlertType.WARNING,
                    "Nombre artístico y género son obligatorios.");
            return null;
        }

        Artista artista = new Artista();

        artista.setNombreArtistico(nombre);
        artista.setGeneroMusical(genero);
        artista.setPaisOrigen(pais.isEmpty() ? null : pais);

        return artista;
    }

    private void mostrarMensaje(Alert.AlertType tipo,
                                String mensaje) {

        Alert alerta = new Alert(tipo);
        alerta.setTitle("Gestión de Artistas");
        alerta.setHeaderText(null);
        alerta.setContentText(mensaje);
        alerta.showAndWait();
    }

    
}
