package controladorFx;

import dao.ArtistaDAO;
import dao.ConciertoDAO;
import java.io.File;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;
import modelo.Artista;
import modelo.Concierto;

public class ConciertosFXController {

    @FXML
    private ComboBox<String> cmbArtista;

    @FXML
    private TextField txtTituloEvento;

    @FXML
    private TextField txtRecinto;

    @FXML
    private DatePicker dpFechaConcierto;

    @FXML
    private TextField txtHoraConcierto;

    @FXML
    private TextField txtHoraApertura;

    @FXML
    private ComboBox<String> cmbEstado;

    @FXML
    private TextField txtImagenEvento;

    @FXML
    private TextArea txtDescripcion;

    @FXML
    private TextArea txtInformacionAdicional;

    @FXML
    private TableView<Concierto> tblConciertos;

    @FXML
    private TableColumn<Concierto, Integer> colId;

    @FXML
    private TableColumn<Concierto, String> colArtista;

    @FXML
    private TableColumn<Concierto, String> colEvento;

    @FXML
    private TableColumn<Concierto, String> colFecha;

    @FXML
    private TableColumn<Concierto, String> colHora;

    @FXML
    private TableColumn<Concierto, String> colRecinto;

    @FXML
    private TableColumn<Concierto, String> colEstado;

    private final ConciertoDAO conciertoDAO = new ConciertoDAO();
    private final ArtistaDAO artistaDAO = new ArtistaDAO();

    private List<Artista> artistas = new ArrayList<>();
    private List<Concierto> conciertos = new ArrayList<>();

    private int idSeleccionado = -1;

    private final DateTimeFormatter formatoHora =
            DateTimeFormatter.ofPattern("HH:mm");

    @FXML
    private void initialize() {

        cmbEstado.setItems(
                FXCollections.observableArrayList(
                        "programado",
                        "activo",
                        "finalizado",
                        "cancelado"
                )
        );

        cmbEstado.getSelectionModel().selectFirst();

        colId.setCellValueFactory(
                data -> new ReadOnlyObjectWrapper<>(
                        data.getValue().getIdConcierto()
                )
        );

        colArtista.setCellValueFactory(
                data -> new ReadOnlyStringWrapper(
                        obtenerNombreArtista(
                                data.getValue().getIdArtista()
                        )
                )
        );

        colEvento.setCellValueFactory(
                data -> new ReadOnlyStringWrapper(
                        data.getValue().getTituloEvento()
                )
        );

        colFecha.setCellValueFactory(
                data -> new ReadOnlyStringWrapper(
                        data.getValue().getFechaConcierto() == null
                                ? ""
                                : data.getValue()
                                        .getFechaConcierto()
                                        .toLocalDate()
                                        .toString()
                )
        );

        colHora.setCellValueFactory(
                data -> new ReadOnlyStringWrapper(
                        data.getValue().getFechaConcierto() == null
                                ? ""
                                : data.getValue()
                                        .getFechaConcierto()
                                        .toLocalTime()
                                        .format(formatoHora)
                )
        );

        colRecinto.setCellValueFactory(
                data -> new ReadOnlyStringWrapper(
                        data.getValue().getRecinto()
                )
        );

        colEstado.setCellValueFactory(
                data -> new ReadOnlyStringWrapper(
                        data.getValue().getEstado()
                )
        );

        tblConciertos
                .getSelectionModel()
                .selectedItemProperty()
                .addListener(
                        (observable, anterior, seleccionado) -> {
                            if (seleccionado != null) {
                                seleccionarConcierto(seleccionado);
                            }
                        }
                );

        cargarArtistas();
        cargarTabla();
    }

    private void cargarArtistas() {

        artistas = artistaDAO.listarTodos();

        cmbArtista.getItems().clear();

        for (Artista artista : artistas) {
            cmbArtista.getItems().add(
                    artista.getNombreArtistico()
            );
        }

        if (!artistas.isEmpty()) {
            cmbArtista.getSelectionModel().selectFirst();
        }
    }

    @FXML
    private void guardar() {

        if (!validarCampos()) {
            return;
        }

        try {

            Concierto concierto =
                    obtenerConciertoFormulario();

            if (conciertoDAO.insertar(concierto)) {

                mostrarAlerta(
                        Alert.AlertType.INFORMATION,
                        "Registro exitoso",
                        "Concierto registrado correctamente."
                );

                limpiar();
                cargarTabla();

            } else {

                mostrarAlerta(
                        Alert.AlertType.ERROR,
                        "Error",
                        "No se pudo registrar el concierto."
                );
            }

        } catch (DateTimeParseException e) {

            mostrarErrorHora();
        }
    }

    @FXML
    private void actualizar() {

        if (idSeleccionado == -1) {

            mostrarAlerta(
                    Alert.AlertType.WARNING,
                    "Selección requerida",
                    "Seleccione un concierto de la tabla."
            );

            return;
        }

        if (!validarCampos()) {
            return;
        }

        try {

            Concierto concierto =
                    obtenerConciertoFormulario();

            concierto.setIdConcierto(
                    idSeleccionado
            );

            if (conciertoDAO.actualizar(concierto)) {

                mostrarAlerta(
                        Alert.AlertType.INFORMATION,
                        "Actualización exitosa",
                        "Concierto actualizado correctamente."
                );

                limpiar();
                cargarTabla();

            } else {

                mostrarAlerta(
                        Alert.AlertType.ERROR,
                        "Error",
                        "No se pudo actualizar el concierto."
                );
            }

        } catch (DateTimeParseException e) {

            mostrarErrorHora();
        }
    }

    @FXML
    private void eliminar() {

        if (idSeleccionado == -1) {

            mostrarAlerta(
                    Alert.AlertType.WARNING,
                    "Selección requerida",
                    "Seleccione un concierto de la tabla."
            );

            return;
        }

        Alert confirmacion =
                new Alert(
                        Alert.AlertType.CONFIRMATION,
                        "¿Está seguro de eliminar este concierto?",
                        ButtonType.YES,
                        ButtonType.NO
                );

        confirmacion.setTitle(
                "Confirmar eliminación"
        );

        confirmacion.setHeaderText(
                null
        );

        ButtonType respuesta =
                confirmacion.showAndWait()
                        .orElse(ButtonType.NO);

        if (respuesta != ButtonType.YES) {
            return;
        }

        if (conciertoDAO.eliminar(idSeleccionado)) {

            mostrarAlerta(
                    Alert.AlertType.INFORMATION,
                    "Eliminación exitosa",
                    "Concierto eliminado correctamente."
            );

            limpiar();
            cargarTabla();

        } else {

            mostrarAlerta(
                    Alert.AlertType.ERROR,
                    "Error",
                    "No se pudo eliminar el concierto."
            );
        }
    }

    @FXML
    private void seleccionarImagen() {

        FileChooser chooser =
                new FileChooser();

        chooser.setTitle(
                "Seleccionar imagen del evento"
        );

        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter(
                        "Imágenes JPG, JPEG y PNG",
                        "*.jpg",
                        "*.jpeg",
                        "*.png"
                )
        );

        File archivo =
                chooser.showOpenDialog(
                        txtImagenEvento
                                .getScene()
                                .getWindow()
                );

        if (archivo != null) {
            txtImagenEvento.setText(
                    archivo.getAbsolutePath()
            );
        }
    }

    @FXML
    private void limpiar() {

        idSeleccionado = -1;

        txtTituloEvento.clear();
        txtRecinto.clear();
        dpFechaConcierto.setValue(null);
        txtHoraConcierto.clear();
        txtHoraApertura.clear();
        txtImagenEvento.clear();
        txtDescripcion.clear();
        txtInformacionAdicional.clear();

        if (!artistas.isEmpty()) {
            cmbArtista
                    .getSelectionModel()
                    .selectFirst();
        } else {
            cmbArtista
                    .getSelectionModel()
                    .clearSelection();
        }

        cmbEstado
                .getSelectionModel()
                .selectFirst();

        tblConciertos
                .getSelectionModel()
                .clearSelection();
    }

    private boolean validarCampos() {

        if (artistas.isEmpty()) {

            mostrarAlerta(
                    Alert.AlertType.WARNING,
                    "Artistas requeridos",
                    "Debe registrar al menos un artista antes de crear un concierto."
            );

            return false;
        }

        if (cmbArtista.getSelectionModel()
                .getSelectedIndex() == -1) {

            mostrarAlerta(
                    Alert.AlertType.WARNING,
                    "Campo obligatorio",
                    "Seleccione un artista."
            );

            return false;
        }

        if (txtTituloEvento
                .getText()
                .trim()
                .isEmpty()) {

            mostrarAlerta(
                    Alert.AlertType.WARNING,
                    "Campo obligatorio",
                    "El título del evento es obligatorio."
            );

            return false;
        }

        if (dpFechaConcierto.getValue() == null) {

            mostrarAlerta(
                    Alert.AlertType.WARNING,
                    "Campo obligatorio",
                    "La fecha del concierto es obligatoria."
            );

            return false;
        }

        if (txtHoraConcierto
                .getText()
                .trim()
                .isEmpty()) {

            mostrarAlerta(
                    Alert.AlertType.WARNING,
                    "Campo obligatorio",
                    "La hora del concierto es obligatoria."
            );

            return false;
        }

        if (txtRecinto
                .getText()
                .trim()
                .isEmpty()) {

            mostrarAlerta(
                    Alert.AlertType.WARNING,
                    "Campo obligatorio",
                    "El recinto es obligatorio."
            );

            return false;
        }

        if (cmbEstado.getValue() == null) {

            mostrarAlerta(
                    Alert.AlertType.WARNING,
                    "Campo obligatorio",
                    "Seleccione un estado."
            );

            return false;
        }

        return true;
    }

    private Concierto obtenerConciertoFormulario()
            throws DateTimeParseException {

        int indiceArtista =
                cmbArtista
                        .getSelectionModel()
                        .getSelectedIndex();

        Artista artista =
                artistas.get(indiceArtista);

        LocalTime horaConcierto =
                LocalTime.parse(
                        txtHoraConcierto
                                .getText()
                                .trim(),
                        formatoHora
                );

        LocalDateTime fechaHoraConcierto =
                LocalDateTime.of(
                        dpFechaConcierto.getValue(),
                        horaConcierto
                );

        LocalTime horaApertura = null;

        String textoHoraApertura =
                txtHoraApertura
                        .getText()
                        .trim();

        if (!textoHoraApertura.isEmpty()) {

            horaApertura =
                    LocalTime.parse(
                            textoHoraApertura,
                            formatoHora
                    );
        }

        Concierto concierto =
                new Concierto();

        concierto.setIdArtista(
                artista.getIdArtista()
        );

        concierto.setTituloEvento(
                txtTituloEvento
                        .getText()
                        .trim()
        );

        concierto.setFechaConcierto(
                fechaHoraConcierto
        );

        concierto.setRecinto(
                txtRecinto
                        .getText()
                        .trim()
        );

        concierto.setEstado(
                cmbEstado.getValue()
        );

        concierto.setDescripcion(
                convertirNull(
                        txtDescripcion.getText()
                )
        );

        concierto.setInformacionAdicional(
                convertirNull(
                        txtInformacionAdicional
                                .getText()
                )
        );

        concierto.setImagenEvento(
                convertirNull(
                        txtImagenEvento.getText()
                )
        );

        concierto.setHoraApertura(
                horaApertura
        );

        return concierto;
    }

    private void cargarTabla() {

        conciertos =
                conciertoDAO.listarTodos();

        tblConciertos.setItems(
                FXCollections.observableArrayList(
                        conciertos
                )
        );
    }

    private void seleccionarConcierto(
            Concierto concierto
    ) {

        idSeleccionado =
                concierto.getIdConcierto();

        seleccionarArtista(
                concierto.getIdArtista()
        );

        txtTituloEvento.setText(
                concierto.getTituloEvento()
        );

        txtRecinto.setText(
                concierto.getRecinto()
        );

        cmbEstado.setValue(
                concierto.getEstado()
        );

        txtDescripcion.setText(
                concierto.getDescripcion() == null
                        ? ""
                        : concierto.getDescripcion()
        );

        txtInformacionAdicional.setText(
                concierto.getInformacionAdicional() == null
                        ? ""
                        : concierto.getInformacionAdicional()
        );

        txtImagenEvento.setText(
                concierto.getImagenEvento() == null
                        ? ""
                        : concierto.getImagenEvento()
        );

        if (concierto.getFechaConcierto() != null) {

            dpFechaConcierto.setValue(
                    concierto
                            .getFechaConcierto()
                            .toLocalDate()
            );

            txtHoraConcierto.setText(
                    concierto
                            .getFechaConcierto()
                            .toLocalTime()
                            .format(formatoHora)
            );

        } else {

            dpFechaConcierto.setValue(null);
            txtHoraConcierto.clear();
        }

        if (concierto.getHoraApertura() != null) {

            txtHoraApertura.setText(
                    concierto
                            .getHoraApertura()
                            .format(formatoHora)
            );

        } else {

            txtHoraApertura.clear();
        }
    }

    private void seleccionarArtista(
            int idArtista
    ) {

        for (int i = 0; i < artistas.size(); i++) {

            if (artistas
                    .get(i)
                    .getIdArtista() == idArtista) {

                cmbArtista
                        .getSelectionModel()
                        .select(i);

                return;
            }
        }

        cmbArtista
                .getSelectionModel()
                .clearSelection();
    }

    private String obtenerNombreArtista(
            int idArtista
    ) {

        for (Artista artista : artistas) {

            if (artista.getIdArtista()
                    == idArtista) {

                return artista
                        .getNombreArtistico();
            }
        }

        return "Desconocido";
    }

    private String convertirNull(
            String texto
    ) {

        if (texto == null
                || texto.trim().isEmpty()) {

            return null;
        }

        return texto.trim();
    }

    private void mostrarErrorHora() {

        mostrarAlerta(
                Alert.AlertType.ERROR,
                "Formato incorrecto",
                "La hora debe usar el formato HH:mm. Ejemplo: 20:30."
        );
    }

    private void mostrarAlerta(
            Alert.AlertType tipo,
            String titulo,
            String mensaje
    ) {

        Alert alerta =
                new Alert(tipo);

        alerta.setTitle(titulo);
        alerta.setHeaderText(null);
        alerta.setContentText(mensaje);
        alerta.showAndWait();
    }
}
