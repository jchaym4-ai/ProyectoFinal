
package controladorFx;

import java.io.IOException;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import modelo.Usuario;

public class MenuVendedorFXController {

    // COMPONENTES DEL FXML

    @FXML
    private Label lblNombreUsuario;

    @FXML
    private Button btnDashboard;

    @FXML
    private Button btnNuevaVenta;

    @FXML
    private Button btnHistorialVentas;

    @FXML
    private Button btnConsultarBoletos;

    @FXML
    private Button btnCerrarSesion;

    @FXML
    private StackPane contenedorCentral;

    // USUARIO AUTENTICADO

    private Usuario usuario;

    // INICIALIZACIÓN

    @FXML
    private void initialize() {

        btnDashboard.setOnAction(
                e -> abrirDashboard()
        );

        btnNuevaVenta.setOnAction(
                e -> abrirNuevaVenta()
        );

        btnHistorialVentas.setOnAction(
                e -> abrirHistorialVentas()
        );

        btnConsultarBoletos.setOnAction(
                e -> abrirConsultarBoletos()
        );

        btnCerrarSesion.setOnAction(
                e -> cerrarSesion()
        );

        abrirDashboard();
    }

    // RECIBIR USUARIO DEL LOGIN

    public void setUsuario(Usuario usuario) {

        if (usuario == null) {
            throw new IllegalArgumentException(
                    "El usuario no puede ser nulo."
            );
        }

        this.usuario = usuario;

        lblNombreUsuario.setText(
                usuario.getNombreCompleto()
        );
    }

    public Usuario getUsuario() {
        return usuario;
    }

    // DASHBOARD VENDEDOR

    private void abrirDashboard() {

        mostrarSeccion(
                "Dashboard Vendedor",
                "Bienvenido al panel de ventas de SVB-GUA."
        );

        marcarBotonActivo(btnDashboard);
    }

    // NUEVA VENTA

    private void abrirNuevaVenta() {

        mostrarSeccion(
                "Nueva Venta",
                "Aquí se realizará la venta de boletos."
        );

        marcarBotonActivo(btnNuevaVenta);
    }

    // HISTORIAL DE VENTAS

    private void abrirHistorialVentas() {

        mostrarSeccion(
                "Historial de Ventas",
                "Aquí se consultarán las ventas registradas."
        );

        marcarBotonActivo(btnHistorialVentas);
    }

    // CONSULTAR BOLETOS

    private void abrirConsultarBoletos() {

        mostrarSeccion(
                "Consultar Boletos",
                "Aquí se consultarán los boletos emitidos."
        );

        marcarBotonActivo(btnConsultarBoletos);
    }

    // CONTENIDO TEMPORAL DEL PANEL CENTRAL

    private void mostrarSeccion(
            String titulo,
            String descripcion) {

        Label lblTitulo = new Label(titulo);
        lblTitulo.getStyleClass().add(
                "content-title"
        );

        Label lblDescripcion = new Label(descripcion);
        lblDescripcion.getStyleClass().add(
                "content-subtitle"
        );

        VBox contenido = new VBox(
                12,
                lblTitulo,
                lblDescripcion
        );

        contenido.getStyleClass().add(
                "content-area"
        );

        contenido.setPadding(
                new Insets(35)
        );

        contenido.setMaxSize(
                Double.MAX_VALUE,
                Double.MAX_VALUE
        );

        contenedorCentral.getChildren().setAll(
                contenido
        );
    }

    // MARCAR BOTÓN SELECCIONADO

    private void marcarBotonActivo(
            Button botonActivo) {

        Button[] botones = {
            btnDashboard,
            btnNuevaVenta,
            btnHistorialVentas,
            btnConsultarBoletos
        };

        for (Button boton : botones) {

            boton.getStyleClass().remove(
                    "menu-button-active"
            );
        }

        if (!botonActivo.getStyleClass().contains(
                "menu-button-active")) {

            botonActivo.getStyleClass().add(
                    "menu-button-active"
            );
        }
    }

    // CERRAR SESIÓN

    @FXML
    private void cerrarSesion() {

        try {

            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource(
                            "/vistas/fx/LoginView.fxml"
                    )
            );

            Parent root = loader.load();

            Stage stage = (Stage) btnCerrarSesion
                    .getScene()
                    .getWindow();

            stage.setScene(
                    new Scene(root)
            );

            stage.setTitle(
                    "SVB-GUA - Inicio de Sesión"
            );

            stage.setResizable(false);
            stage.sizeToScene();
            stage.centerOnScreen();

            usuario = null;

        } catch (IOException e) {

            e.printStackTrace();
        }
    }
}
