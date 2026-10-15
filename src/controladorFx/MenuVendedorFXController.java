package controladorFx;

import java.io.IOException;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import modelo.Usuario;

public class MenuVendedorFXController {

    @FXML private Label lblNombreUsuario;
    @FXML private Button btnDashboard;
    @FXML private Button btnNuevaVenta;
    @FXML private Button btnHistorialVentas;
    @FXML private Button btnConsultarBoletos;
    @FXML private Button btnCerrarSesion;
    @FXML private StackPane contenedorCentral;

    private Usuario usuario;

    @FXML
    private void initialize() {
        btnDashboard.setOnAction(e -> abrirDashboard());
        btnNuevaVenta.setOnAction(e -> abrirNuevaVenta());
        btnHistorialVentas.setOnAction(e -> abrirHistorialVentas());
        btnConsultarBoletos.setOnAction(e -> abrirConsultarBoletos());
        btnCerrarSesion.setOnAction(e -> cerrarSesion());
        abrirDashboard();
    }

    public void setUsuario(Usuario usuario) {
        if (usuario == null) {
            throw new IllegalArgumentException("El usuario no puede ser nulo.");
        }
        this.usuario = usuario;
        lblNombreUsuario.setText(usuario.getNombreCompleto());
        abrirDashboard();
    }

    public Usuario getUsuario() {
        return usuario;
    }

    private void abrirDashboard() {
        if (usuario == null) return;
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(
                    "/vistas/fx/DashboardVendedorView.fxml"));
            Parent vista = loader.load();
            DashboardVendedorFXController controller = loader.getController();
            controller.setUsuario(usuario);
            contenedorCentral.getChildren().setAll(vista);
            marcarBotonActivo(btnDashboard);
        } catch (IOException | RuntimeException e) {
            mostrarError("No se pudo abrir Dashboard", e);
        }
    }

    private void abrirNuevaVenta() {
        if (usuario == null) return;
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(
                    "/vistas/fx/NuevaVentaView.fxml"));
            Parent vista = loader.load();
            NuevaVentaFXController controller = loader.getController();
            controller.setUsuario(usuario);
            contenedorCentral.getChildren().setAll(vista);
            marcarBotonActivo(btnNuevaVenta);
        } catch (IOException | RuntimeException e) {
            mostrarError("No se pudo abrir Nueva Venta", e);
        }
    }

    private void abrirHistorialVentas() {
        if (usuario == null) return;
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(
                    "/vistas/fx/HistorialVentasView.fxml"));
            Parent vista = loader.load();
            HistorialVentasFXController controller = loader.getController();
            controller.setUsuario(usuario);
            contenedorCentral.getChildren().setAll(vista);
            marcarBotonActivo(btnHistorialVentas);
        } catch (IOException | RuntimeException e) {
            mostrarError("No se pudo abrir Historial de Ventas", e);
        }
    }

    private void abrirConsultarBoletos() {
        if (usuario == null) return;
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(
                    "/vistas/fx/ConsultarBoletosView.fxml"));
            Parent vista = loader.load();
            contenedorCentral.getChildren().setAll(vista);
            marcarBotonActivo(btnConsultarBoletos);
        } catch (IOException | RuntimeException e) {
            mostrarError("No se pudo abrir Consultar Boletos", e);
        }
    }

    private void marcarBotonActivo(Button botonActivo) {
        Button[] botones = {
            btnDashboard, btnNuevaVenta,
            btnHistorialVentas, btnConsultarBoletos
        };
        for (Button boton : botones) {
            boton.getStyleClass().remove("menu-button-active");
        }
        if (!botonActivo.getStyleClass().contains("menu-button-active")) {
            botonActivo.getStyleClass().add("menu-button-active");
        }
    }

    @FXML
    private void cerrarSesion() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(
                    "/vistas/fx/LoginView.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) btnCerrarSesion.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("SVB-GUA - Inicio de Sesion");
            stage.setResizable(false);
            stage.sizeToScene();
            stage.centerOnScreen();
            usuario = null;
        } catch (IOException | RuntimeException e) {
            mostrarError("No se pudo cerrar sesion", e);
        }
    }

    private void mostrarError(String titulo, Exception e) {
        e.printStackTrace();
        Alert alerta = new Alert(Alert.AlertType.ERROR);
        alerta.setTitle("SVB-GUA - Vendedor");
        alerta.setHeaderText(titulo);
        alerta.setContentText(e.getMessage() == null
                ? "Error desconocido." : e.getMessage());
        alerta.showAndWait();
    }
}
