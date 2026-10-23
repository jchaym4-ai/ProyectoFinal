package appFx;

import java.net.URL;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

/** Ventana independiente. No requiere iniciar sesión como Vendedor o Admin. */
public class EventosPublicosApp extends Application {
    @Override
    public void start(Stage stage) throws Exception {
        URL recurso = getClass().getResource("/vistas/fx/EventosPublicosView.fxml");
        if (recurso == null) {
            throw new IllegalStateException("Falta /vistas/fx/EventosPublicosView.fxml");
        }
        Parent vista = FXMLLoader.load(recurso);
        Scene escena = new Scene(vista, 1240, 810);
        stage.setTitle("SVB-GUA | EVENTOS - Cartelera pública");
        stage.setScene(escena);
        stage.setMinWidth(900);
        stage.setMinHeight(650);
        stage.setMaximized(true);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
