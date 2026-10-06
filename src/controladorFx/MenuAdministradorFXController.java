package controladorFx;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import modelo.Usuario;
import java.io.IOException;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class MenuAdministradorFXController {

    @FXML
    private Label lblNombreUsuario;

    @FXML
    private Button btnDashboard;

    @FXML
    private Button btnArtistas;

    @FXML
    private Button btnLocalidades;

    @FXML
    private Button btnConciertos;

    @FXML
    private Button btnInventario;

    @FXML
    private Button btnUsuarios;

    @FXML
    private Button btnCerrarSesion;

    private Usuario usuario;

    public void setUsuario(Usuario usuario) {

        this.usuario = usuario;

        if (usuario != null) {
            lblNombreUsuario.setText(
                    usuario.getNombreCompleto()
            );
        }
    }

    public Usuario getUsuario() {
        return usuario;
    }
    
    @FXML
    private void cerrarSesion() {

        try {

            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource(
                            "/vistas/fx/LoginView.fxml"
                    )
            );

            Parent root = loader.load();

            Stage loginStage = new Stage();

            loginStage.setScene(new Scene(root));
            loginStage.setTitle("SVB-GUA");
            loginStage.setResizable(false);
            loginStage.show();

            Stage menuStage =
                    (Stage) btnCerrarSesion
                            .getScene()
                            .getWindow();

            menuStage.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }    
}