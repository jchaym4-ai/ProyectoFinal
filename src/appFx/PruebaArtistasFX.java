
package appFx;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class PruebaArtistasFX extends Application {

    @Override
    public void start(Stage stage) throws Exception {

        FXMLLoader loader = new FXMLLoader(
                getClass().getResource(
                        "/vistas/fx/ArtistasView.fxml"
                )
        );

        Parent root = loader.load();

        Scene scene = new Scene(root);

        stage.setTitle("SVB-GUA | Gestión de Artistas");
        stage.setScene(scene);
        stage.setWidth(1100);
        stage.setHeight(750);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
