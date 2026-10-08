package controladorFx;

import dao.UsuarioDAO;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import modelo.Usuario;
import util.PasswordUtil;

public class UsuariosFXController {

    @FXML
    private TextField txtNombreCompleto;

    @FXML
    private TextField txtNombreUsuario;

    @FXML
    private PasswordField txtContrasena;

    @FXML
    private ComboBox<String> cmbRol;

    @FXML
    private TableView<Usuario> tblUsuarios;

    @FXML
    private TableColumn<Usuario, Integer> colId;

    @FXML
    private TableColumn<Usuario, String> colUsuario;

    @FXML
    private TableColumn<Usuario, String> colNombreCompleto;

    @FXML
    private TableColumn<Usuario, String> colRol;

    @FXML
    private TableColumn<Usuario, LocalDateTime> colFechaCreacion;

    @FXML
    private Button btnGuardar;

    @FXML
    private Button btnActualizar;

    @FXML
    private Button btnEliminar;

    @FXML
    private Button btnCambiarContrasena;

    @FXML
    private Button btnLimpiar;

    private final UsuarioDAO usuarioDAO;

    private int idSeleccionado = -1;

    public UsuariosFXController() {
        usuarioDAO = new UsuarioDAO();
    }

    @FXML
    private void initialize() {

        cmbRol.setItems(
                FXCollections.observableArrayList(
                        "administrador",
                        "vendedor"
                )
        );

        colId.setCellValueFactory(
                new PropertyValueFactory<>("idUsuario")
        );

        colUsuario.setCellValueFactory(
                new PropertyValueFactory<>("nombreUsuario")
        );

        colNombreCompleto.setCellValueFactory(
                new PropertyValueFactory<>("nombreCompleto")
        );

        colRol.setCellValueFactory(
                new PropertyValueFactory<>("rol")
        );

        colFechaCreacion.setCellValueFactory(
                new PropertyValueFactory<>("fechaCreacion")
        );

        DateTimeFormatter formato =
                DateTimeFormatter.ofPattern(
                        "dd/MM/yyyy HH:mm"
                );

        colFechaCreacion.setCellFactory(
                columna -> new TableCell<Usuario, LocalDateTime>() {

                    @Override
                    protected void updateItem(
                            LocalDateTime fecha,
                            boolean empty
                    ) {

                        super.updateItem(fecha, empty);

                        if (empty || fecha == null) {

                            setText(null);

                        } else {

                            setText(
                                    fecha.format(formato)
                            );
                        }
                    }
                }
        );

        tblUsuarios
                .getSelectionModel()
                .selectedItemProperty()
                .addListener(
                        (observable, anterior, seleccionado) -> {

                            if (seleccionado != null) {
                                seleccionarUsuario(seleccionado);
                            }
                        }
                );

        cargarTabla();
    }

    @FXML
    private void guardar() {

        String nombreCompleto =
                txtNombreCompleto.getText().trim();

        String nombreUsuario =
                txtNombreUsuario.getText().trim();

        String contrasena =
                txtContrasena.getText();

        String rol =
                cmbRol.getValue();

        if (nombreCompleto.isEmpty()) {

            mostrarAlerta(
                    Alert.AlertType.WARNING,
                    "Campo obligatorio",
                    "El nombre completo es obligatorio."
            );

            return;
        }

        if (nombreUsuario.isEmpty()) {

            mostrarAlerta(
                    Alert.AlertType.WARNING,
                    "Campo obligatorio",
                    "El nombre de usuario es obligatorio."
            );

            return;
        }

        if (contrasena == null
                || contrasena.isEmpty()) {

            mostrarAlerta(
                    Alert.AlertType.WARNING,
                    "Campo obligatorio",
                    "La contraseña es obligatoria."
            );

            return;
        }

        if (rol == null
                || rol.isBlank()) {

            mostrarAlerta(
                    Alert.AlertType.WARNING,
                    "Campo obligatorio",
                    "Seleccione un rol."
            );

            return;
        }

        Usuario existente =
                usuarioDAO.buscarPorNombreUsuario(
                        nombreUsuario
                );

        if (existente != null) {

            mostrarAlerta(
                    Alert.AlertType.WARNING,
                    "Usuario existente",
                    "El nombre de usuario ya está registrado."
            );

            return;
        }

        Usuario usuario =
                new Usuario();

        usuario.setNombreCompleto(
                nombreCompleto
        );

        usuario.setNombreUsuario(
                nombreUsuario
        );

        usuario.setContrasenaHash(
                PasswordUtil.encriptar(
                        contrasena
                )
        );

        usuario.setRol(
                rol
        );

        if (usuarioDAO.insertar(usuario)) {

            mostrarAlerta(
                    Alert.AlertType.INFORMATION,
                    "Registro exitoso",
                    "Usuario registrado correctamente."
            );

            limpiar();
            cargarTabla();

        } else {

            mostrarAlerta(
                    Alert.AlertType.ERROR,
                    "Error",
                    "No se pudo registrar el usuario."
            );
        }
    }

    @FXML
    private void actualizar() {

        if (idSeleccionado == -1) {

            mostrarAlerta(
                    Alert.AlertType.WARNING,
                    "Selección requerida",
                    "Seleccione un usuario."
            );

            return;
        }

        String nombreCompleto =
                txtNombreCompleto.getText().trim();

        String nombreUsuario =
                txtNombreUsuario.getText().trim();

        String rol =
                cmbRol.getValue();

        if (nombreCompleto.isEmpty()) {

            mostrarAlerta(
                    Alert.AlertType.WARNING,
                    "Campo obligatorio",
                    "El nombre completo es obligatorio."
            );

            return;
        }

        if (nombreUsuario.isEmpty()) {

            mostrarAlerta(
                    Alert.AlertType.WARNING,
                    "Campo obligatorio",
                    "El nombre de usuario es obligatorio."
            );

            return;
        }

        if (rol == null
                || rol.isBlank()) {

            mostrarAlerta(
                    Alert.AlertType.WARNING,
                    "Campo obligatorio",
                    "Seleccione un rol."
            );

            return;
        }

        Usuario existente =
                usuarioDAO.buscarPorNombreUsuario(
                        nombreUsuario
                );

        if (existente != null
                && existente.getIdUsuario()
                != idSeleccionado) {

            mostrarAlerta(
                    Alert.AlertType.WARNING,
                    "Usuario existente",
                    "El nombre de usuario ya está registrado."
            );

            return;
        }

        Usuario usuario =
                new Usuario();

        usuario.setIdUsuario(
                idSeleccionado
        );

        usuario.setNombreCompleto(
                nombreCompleto
        );

        usuario.setNombreUsuario(
                nombreUsuario
        );

        usuario.setRol(
                rol
        );

        if (usuarioDAO.actualizar(usuario)) {

            mostrarAlerta(
                    Alert.AlertType.INFORMATION,
                    "Actualización exitosa",
                    "Usuario actualizado correctamente."
            );

            limpiar();
            cargarTabla();

        } else {

            mostrarAlerta(
                    Alert.AlertType.ERROR,
                    "Error",
                    "No se pudo actualizar el usuario."
            );
        }
    }

    @FXML
    private void cambiarContrasena() {

        if (idSeleccionado == -1) {

            mostrarAlerta(
                    Alert.AlertType.WARNING,
                    "Selección requerida",
                    "Seleccione un usuario."
            );

            return;
        }

        String nuevaContrasena =
                txtContrasena.getText();

        if (nuevaContrasena == null
                || nuevaContrasena.isEmpty()) {

            mostrarAlerta(
                    Alert.AlertType.WARNING,
                    "Contraseña requerida",
                    "Ingrese la nueva contraseña."
            );

            return;
        }

        String hash =
                PasswordUtil.encriptar(
                        nuevaContrasena
                );

        if (usuarioDAO.actualizarContrasena(
                idSeleccionado,
                hash
        )) {

            mostrarAlerta(
                    Alert.AlertType.INFORMATION,
                    "Contraseña actualizada",
                    "La contraseña fue actualizada correctamente."
            );

            txtContrasena.clear();

        } else {

            mostrarAlerta(
                    Alert.AlertType.ERROR,
                    "Error",
                    "No se pudo actualizar la contraseña."
            );
        }
    }

    @FXML
    private void eliminar() {

        if (idSeleccionado == -1) {

            mostrarAlerta(
                    Alert.AlertType.WARNING,
                    "Selección requerida",
                    "Seleccione un usuario."
            );

            return;
        }

        Alert confirmacion =
                new Alert(
                        Alert.AlertType.CONFIRMATION
                );

        confirmacion.setTitle(
                "Confirmar eliminación"
        );

        confirmacion.setHeaderText(
                null
        );

        confirmacion.setContentText(
                "¿Desea eliminar este usuario?"
        );

        confirmacion.getButtonTypes()
                .setAll(
                        ButtonType.YES,
                        ButtonType.NO
                );

        ButtonType respuesta =
                confirmacion.showAndWait()
                        .orElse(
                                ButtonType.NO
                        );

        if (respuesta != ButtonType.YES) {
            return;
        }

        if (usuarioDAO.eliminar(
                idSeleccionado
        )) {

            mostrarAlerta(
                    Alert.AlertType.INFORMATION,
                    "Eliminación exitosa",
                    "Usuario eliminado correctamente."
            );

            limpiar();
            cargarTabla();

        } else {

            mostrarAlerta(
                    Alert.AlertType.ERROR,
                    "Error",
                    "No se pudo eliminar el usuario."
            );
        }
    }

    @FXML
    private void limpiar() {

        idSeleccionado = -1;

        txtNombreCompleto.clear();
        txtNombreUsuario.clear();
        txtContrasena.clear();

        cmbRol.getSelectionModel()
                .clearSelection();

        tblUsuarios
                .getSelectionModel()
                .clearSelection();
    }

    private void cargarTabla() {

        List<Usuario> usuarios =
                usuarioDAO.listarTodos();

        tblUsuarios.setItems(
                FXCollections.observableArrayList(
                        usuarios
                )
        );
    }

    private void seleccionarUsuario(
            Usuario usuario
    ) {

        idSeleccionado =
                usuario.getIdUsuario();

        txtNombreCompleto.setText(
                usuario.getNombreCompleto()
        );

        txtNombreUsuario.setText(
                usuario.getNombreUsuario()
        );

        cmbRol.setValue(
                usuario.getRol()
        );

        txtContrasena.clear();
    }

    private void mostrarAlerta(
            Alert.AlertType tipo,
            String titulo,
            String mensaje
    ) {

        Alert alerta =
                new Alert(tipo);

        alerta.setTitle(
                titulo
        );

        alerta.setHeaderText(
                null
        );

        alerta.setContentText(
                mensaje
        );

        alerta.showAndWait();
    }
}