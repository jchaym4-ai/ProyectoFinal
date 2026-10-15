package controladorFx;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import modelo.Boleto;

/**
 * Ventana reutilizable para descargar boletos ya publicados en Cloudflare.
 * No crea ventas nuevas ni modifica Neon.
 */
public final class VentanaDescargaBoletosFX {

    private static final String HOST =
            "svb-gua-boletos.jchaym4.workers.dev";

    private VentanaDescargaBoletosFX() {
    }

    public static void mostrar(
            Window propietario,
            int idVenta,
            List<Boleto> boletos,
            String mensaje) {

        Dialog<Void> ventana = new Dialog<>();
        ventana.setTitle("SVB-GUA - Descargar boletos");
        ventana.getDialogPane().setHeaderText(
                "Venta #" + idVenta + " - Boletos"
        );

        if (propietario != null) {
            ventana.initOwner(propietario);
        }

        ventana.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        ventana.getDialogPane().setPrefWidth(670);
        ventana.setResizable(true);

        Label resumen = new Label(mensaje);
        resumen.setWrapText(true);
        resumen.setStyle("-fx-text-fill: #334155;");

        Label estado = new Label(
                "Puede descargar los PDF publicados en Cloudflare."
        );
        estado.setWrapText(true);
        estado.setStyle("-fx-text-fill: #2563EB;");

        VBox filas = new VBox(9);
        List<Boleto> publicados = new ArrayList<>();

        for (Boleto boleto : boletos) {
            HBox fila = new HBox(10);
            fila.setAlignment(Pos.CENTER_LEFT);
            fila.setPadding(new Insets(10));
            fila.setStyle(
                    "-fx-background-color: #F8FAFC;"
                    + "-fx-border-color: #E2E8F0;"
                    + "-fx-border-radius: 7;"
                    + "-fx-background-radius: 7;"
            );

            Label titulo = new Label(
                    "Boleto #" + boleto.getIdBoleto()
            );
            titulo.setStyle("-fx-font-weight: bold;");
            titulo.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(titulo, Priority.ALWAYS);

            String ruta = boleto.getRutaPdf();
            boolean publicado = ruta != null && !ruta.isBlank();

            if (publicado) {
                publicados.add(boleto);

                Button abrir = new Button("Ver PDF");
                abrir.setOnAction(evento -> {
                    try {
                        URI uri = verificarDireccion(ruta);
                        if (!Desktop.isDesktopSupported()
                                || !Desktop.getDesktop().isSupported(
                                        Desktop.Action.BROWSE)) {
                            throw new IOException(
                                    "No se puede abrir el navegador en este equipo."
                            );
                        }
                        Desktop.getDesktop().browse(uri);
                    } catch (Exception ex) {
                        estado.setText("No se pudo abrir el boleto #"
                                + boleto.getIdBoleto() + ": " + ex.getMessage());
                    }
                });

                Button descargar = new Button("Descargar PDF");
                descargar.setStyle(
                        "-fx-background-color: #2563EB;"
                        + "-fx-text-fill: white;"
                );
                descargar.setOnAction(evento -> {
                    FileChooser selector = new FileChooser();
                    selector.setTitle("Guardar boleto #" + boleto.getIdBoleto());
                    selector.setInitialFileName(
                            "boleto-" + boleto.getIdBoleto() + ".pdf"
                    );
                    selector.getExtensionFilters().add(
                            new FileChooser.ExtensionFilter(
                                    "Documento PDF", "*.pdf"
                            )
                    );

                    File destino = selector.showSaveDialog(
                            ventana.getDialogPane().getScene().getWindow()
                    );
                    if (destino == null) {
                        return;
                    }

                    iniciarDescarga(
                            List.of(new Descarga(boleto, destino.toPath())),
                            descargar,
                            estado
                    );
                });

                fila.getChildren().addAll(titulo, abrir, descargar);
            } else {
                Label pendiente = new Label("PDF pendiente");
                pendiente.setStyle("-fx-text-fill: #B45309;");
                fila.getChildren().addAll(titulo, pendiente);
            }
            filas.getChildren().add(fila);
        }

        if (boletos.isEmpty()) {
            filas.getChildren().add(new Label(
                    "No se pudieron recuperar los boletos de esta venta."
            ));
        }

        ScrollPane desplazamiento = new ScrollPane(filas);
        desplazamiento.setFitToWidth(true);
        desplazamiento.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        desplazamiento.setPrefViewportHeight(
                Math.min(340, Math.max(90, boletos.size() * 61))
        );
        desplazamiento.setStyle("-fx-background-color: transparent;");

        VBox contenido = new VBox(13, resumen, desplazamiento);
        contenido.setPadding(new Insets(8));

        if (publicados.size() > 1) {
            Button descargarTodos = new Button(
                    "Descargar todos (" + publicados.size() + ")"
            );
            descargarTodos.setStyle(
                    "-fx-background-color: #0F172A;"
                    + "-fx-text-fill: white;"
                    + "-fx-font-weight: bold;"
            );
            descargarTodos.setOnAction(evento -> {
                DirectoryChooser selector = new DirectoryChooser();
                selector.setTitle("Elegir carpeta para los boletos");
                File carpeta = selector.showDialog(
                        ventana.getDialogPane().getScene().getWindow()
                );
                if (carpeta == null) {
                    return;
                }

                List<Descarga> trabajos = new ArrayList<>();
                for (Boleto boleto : publicados) {
                    Path destino = nombreDisponible(
                            carpeta.toPath(), boleto.getIdBoleto()
                    );
                    trabajos.add(new Descarga(boleto, destino));
                }
                iniciarDescarga(trabajos, descargarTodos, estado);
            });
            contenido.getChildren().add(descargarTodos);
        }

        contenido.getChildren().add(estado);
        ventana.getDialogPane().setContent(contenido);
        ventana.showAndWait();
    }

    private record Descarga(Boleto boleto, Path destino) {
    }

    private static Path nombreDisponible(Path carpeta, int idBoleto) {
        Path archivo = carpeta.resolve("boleto-" + idBoleto + ".pdf");
        int numero = 2;
        while (Files.exists(archivo)) {
            archivo = carpeta.resolve(
                    "boleto-" + idBoleto + "-" + numero + ".pdf"
            );
            numero++;
        }
        return archivo;
    }

    private static URI verificarDireccion(String url) throws IOException {
        try {
            URI uri = URI.create(url);
            if (!"https".equalsIgnoreCase(uri.getScheme())
                    || !HOST.equalsIgnoreCase(uri.getHost())
                    || !uri.getPath().matches("/b/[a-f0-9]{32}")
                    || uri.getRawQuery() != null
                    || uri.getRawFragment() != null) {
                throw new IOException("La URL del boleto no es valida.");
            }
            return uri;
        } catch (IllegalArgumentException ex) {
            throw new IOException("La URL del boleto no es valida.", ex);
        }
    }

    private static void iniciarDescarga(
            List<Descarga> trabajos,
            Button boton,
            Label estado) {

        boton.setDisable(true);
        estado.setText("Descargando " + trabajos.size() + " boleto(s)...");

        Task<Integer> tarea = new Task<>() {
            @Override
            protected Integer call() throws Exception {
                int descargados = 0;
                try (HttpClient cliente = HttpClient.newBuilder()
                        .connectTimeout(Duration.ofSeconds(20))
                        .build()) {
                    for (Descarga trabajo : trabajos) {
                        URI uri = verificarDireccion(
                                trabajo.boleto().getRutaPdf()
                        );
                        HttpRequest solicitud = HttpRequest.newBuilder(uri)
                                .timeout(Duration.ofSeconds(60))
                                .GET()
                                .build();
                        HttpResponse<byte[]> respuesta = cliente.send(
                                solicitud,
                                HttpResponse.BodyHandlers.ofByteArray()
                        );
                        if (respuesta.statusCode() != 200) {
                            throw new IOException(
                                    "El boleto #" + trabajo.boleto().getIdBoleto()
                                    + " respondió HTTP " + respuesta.statusCode()
                            );
                        }
                        byte[] bytes = respuesta.body();
                        if (bytes == null || bytes.length < 5
                                || bytes[0] != '%'
                                || bytes[1] != 'P'
                                || bytes[2] != 'D'
                                || bytes[3] != 'F'
                                || bytes[4] != '-') {
                            throw new IOException(
                                    "El servidor no devolvio un PDF valido para el boleto #"
                                    + trabajo.boleto().getIdBoleto()
                            );
                        }

                        Path destino = trabajo.destino();
                        Path temporal = Files.createTempFile(
                                destino.toAbsolutePath().getParent(),
                                "svb-gua-", ".tmp"
                        );
                        try {
                            Files.write(temporal, bytes);
                            Files.move(
                                    temporal,
                                    destino,
                                    StandardCopyOption.REPLACE_EXISTING
                            );
                        } finally {
                            Files.deleteIfExists(temporal);
                        }
                        descargados++;
                    }
                }
                return descargados;
            }
        };

        tarea.setOnSucceeded(evento -> {
            boton.setDisable(false);
            estado.setText(
                    "Descarga completada: " + tarea.getValue()
                    + " PDF guardado(s) en la carpeta seleccionada."
            );
        });
        tarea.setOnFailed(evento -> {
            boton.setDisable(false);
            Throwable error = tarea.getException();
            estado.setText(
                    "No se completo la descarga: "
                    + (error == null ? "error desconocido" : error.getMessage())
                    + ". Si selecciono varios boletos, algunos podrian haberse guardado."
            );
            if (error != null) {
                error.printStackTrace();
            }
        });

        Thread hilo = new Thread(tarea, "svb-gua-descargar-pdf");
        hilo.setDaemon(true);
        hilo.start();
    }
}
