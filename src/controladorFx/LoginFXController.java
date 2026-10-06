package controladorFx;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import modelo.Usuario;
import servicio.AuthService;
import java.io.IOException;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class LoginFXController {

    @FXML
    private TextField txtUsuario;

    @FXML
    private PasswordField txtPassword;

    @FXML
    private Label lblMensaje;

    @FXML
    private Button btnIniciarSesion;

    private final AuthService authService;

    public LoginFXController() {
        this.authService = new AuthService();
    }

    @FXML
    private void iniciarSesion() {

        String nombreUsuario = txtUsuario.getText();
        String contrasena = txtPassword.getText();

        limpiarMensaje();

        // Validar campos vacíos
        if (nombreUsuario == null || nombreUsuario.trim().isEmpty()
                || contrasena == null || contrasena.isEmpty()) {

            mostrarError("Debe ingresar usuario y contraseña.");
            return;
        }

        // Autenticar utilizando el servicio existente
        Usuario usuario = authService.iniciarSesion(
                nombreUsuario,
                contrasena
        );

        // Credenciales incorrectas
        if (usuario == null) {

            mostrarError("Usuario o contraseña incorrectos.");

            txtPassword.clear();
            txtPassword.requestFocus();

            return;
        }

        // Login correcto
        mostrarExito(
                "Bienvenido " + usuario.getNombreCompleto()
        );

        // Revisar rol
        if ("administrador".equals(usuario.getRol())) {

            System.out.println(
                    "Login correcto - ADMINISTRADOR"
            );

            abrirMenuAdministrador(usuario);
        } else if ("vendedor".equals(usuario.getRol())) {

            System.out.println(
                    "Login correcto - VENDEDOR"
            );

            // Aquí abriremos después el menú de vendedor
        }
    }

    private void mostrarError(String mensaje) {

        lblMensaje.getStyleClass().removeAll(
                "error-label",
                "success-label"
        );

        lblMensaje.getStyleClass().add("error-label");
        lblMensaje.setText(mensaje);
    }

    private void mostrarExito(String mensaje) {

        lblMensaje.getStyleClass().removeAll(
                "error-label",
                "success-label"
        );

        lblMensaje.getStyleClass().add("success-label");
        lblMensaje.setText(mensaje);
    }

    private void limpiarMensaje() {

        lblMensaje.setText("");

        lblMensaje.getStyleClass().removeAll(
                "error-label",
                "success-label"
        );
    }
    
    private void abrirMenuAdministrador(Usuario usuario) {

        try {

            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource(
                            "/vistas/fx/MenuAdministradorView.fxml"
                    )
            );

            Parent root = loader.load();

            MenuAdministradorFXController controller =
                    loader.getController();

            controller.setUsuario(usuario);

            Stage stage = new Stage();

            Scene scene = new Scene(root);

            stage.setTitle("SVB-GUA - Administración");
            stage.setScene(scene);
            stage.setResizable(false);
            stage.show();

            Stage loginStage =
                    (Stage) btnIniciarSesion
                            .getScene()
                            .getWindow();

            loginStage.close();

        } catch (IOException e) {

            e.printStackTrace();

            mostrarError(
                    "No se pudo abrir el menú de administración."
            );
        }
    }    
}