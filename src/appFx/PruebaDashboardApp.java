
package appFx;

import controladorFx.DashboardVendedorFXController;
import modelo.Usuario;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.TextInputDialog;
import javafx.stage.Stage;

import java.util.Optional;

public class PruebaDashboardApp extends Application {

    @Override
    public void start(Stage stage) throws Exception {

        FXMLLoader loader = new FXMLLoader(
                getClass().getResource(
                    "/vistas/fx/DashboardVendedorView.fxml"
                )
        );

        Parent root = loader.load();

        DashboardVendedorFXController controller =
                loader.getController();

        if (controller == null) {
            throw new IllegalStateException(
                "Falta configurar fx:controller en el FXML."
            );
        }

        Scene scene = new Scene(root, 1020, 700);

        stage.setTitle("SVB-GUA - Prueba Dashboard");
        stage.setScene(scene);
        stage.show();

        TextInputDialog dialog = new TextInputDialog();

        dialog.setTitle("Prueba del Dashboard");
        dialog.setHeaderText("Seleccionar vendedor de prueba");
        dialog.setContentText(
                "Ingresa un ID real de vendedor:"
        );

        Optional<String> resultado = dialog.showAndWait();

        if (resultado.isEmpty()) {
            stage.close();
            return;
        }

        try {
            int idVendedor = Integer.parseInt(
                    resultado.get().trim()
            );

            if (idVendedor <= 0) {
                throw new NumberFormatException();
            }

            Usuario usuario = new Usuario();
            usuario.setIdUsuario(idVendedor);

            controller.setUsuario(usuario);

        } catch (NumberFormatException ex) {

            Alert alerta = new Alert(
                    Alert.AlertType.ERROR
            );

            alerta.setTitle("ID incorrecto");
            alerta.setHeaderText("ID de vendedor inválido");
            alerta.setContentText(
                    "Debes ingresar un número entero positivo."
            );

            alerta.showAndWait();
            stage.close();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
