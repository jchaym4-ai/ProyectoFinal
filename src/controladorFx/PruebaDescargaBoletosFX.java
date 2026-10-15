package controladorFx;

import dao.BoletoDAO;
import java.util.List;
import javafx.application.Application;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import modelo.Boleto;

/** Prueba la descarga de boletos existentes sin registrar otra venta. */
public class PruebaDescargaBoletosFX extends Application {

    @Override
    public void start(Stage stage) {
        Label descripcion = new Label(
                "Prueba de descarga de boletos existentes. No registra ventas."
        );
        descripcion.setWrapText(true);

        TextField campoVenta = new TextField("7");
        campoVenta.setPromptText("ID de venta en Neon");
        Button probar = new Button("Mostrar boletos de esta venta");
        VBox panel = new VBox(15, descripcion, campoVenta, probar);
        panel.setPadding(new Insets(20));

        stage.setTitle("SVB-GUA - Prueba de descarga");
        stage.setScene(new Scene(panel, 440, 185));
        stage.show();

        probar.setOnAction(evento -> {
            final int idVenta;
            try {
                idVenta = Integer.parseInt(campoVenta.getText().trim());
                if (idVenta <= 0) {
                    throw new NumberFormatException();
                }
            } catch (NumberFormatException ex) {
                new Alert(Alert.AlertType.WARNING,
                        "Ingrese un ID de venta numerico valido.")
                        .showAndWait();
                return;
            }

            probar.setDisable(true);
            Task<List<Boleto>> tarea = new Task<>() {
                @Override
                protected List<Boleto> call() throws Exception {
                    return new BoletoDAO().listarPorVenta(idVenta);
                }
            };
            tarea.setOnSucceeded(e -> {
                probar.setDisable(false);
                List<Boleto> boletos = tarea.getValue();
                VentanaDescargaBoletosFX.mostrar(
                        stage,
                        idVenta,
                        boletos,
                        "Prueba con boletos existentes: " + boletos.size()
                );
            });
            tarea.setOnFailed(e -> {
                probar.setDisable(false);
                new Alert(
                        Alert.AlertType.ERROR,
                        "No se pudieron consultar los boletos: "
                        + tarea.getException().getMessage()
                ).showAndWait();
            });
            Thread hilo = new Thread(tarea, "svb-gua-probar-descargas");
            hilo.setDaemon(true);
            hilo.start();
        });
    }

    public static void main(String[] args) {
        launch(args);
    }
}
