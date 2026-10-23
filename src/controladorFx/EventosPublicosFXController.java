package controladorFx;

import dao.EventosPublicosDAO;
import dao.EventosPublicosDAO.Asiento;
import dao.EventosPublicosDAO.Evento;
import dao.EventosPublicosDAO.Localidad;
import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Collections;
import javafx.scene.layout.AnchorPane;
import javafx.scene.input.MouseButton;
import javafx.scene.shape.Rectangle;
import javafx.scene.transform.Scale;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/** Solo consulta: no reserva, no cobra, no registra ventas ni altera inventario. */
public class EventosPublicosFXController {

    @FXML private Button btnActualizar;
    @FXML private Button btnVolver;
    @FXML private ScrollPane scrollCartelera;
    @FXML private ScrollPane scrollDetalle;
    @FXML private FlowPane panelTarjetas;
    @FXML private FlowPane panelLocalidades;
    @FXML private VBox panelAsientos;
    @FXML private VBox filasAsientos;
    @FXML private Label lblEstado;
    @FXML private Label lblDetalleTitulo;
    @FXML private Label lblDetalleMeta;
    @FXML private Label lblLocalidadElegida;
    @FXML private Label lblEleccion;
    @FXML private StackPane contenedorMapaPublico;
    @FXML private AnchorPane panelMapaPublico;
    @FXML private Label lblInfoMapa;

    private final EventosPublicosDAO dao = new EventosPublicosDAO();
    private static final DateTimeFormatter FORMATO_FECHA =
            DateTimeFormatter.ofPattern("dd/MM/yyyy · HH:mm", new Locale("es", "GT"));

    private Evento eventoActual;
    private Localidad localidadActual;
    private Button asientoMarcado;
    private Map<Integer, List<Asiento>> asientosDelEvento = Map.of();
    private final Scale escalaMapa = new Scale(1, 1, 295, 285);
    private double zoom = 1;
    private double inicioX, inicioY, trasladoX, trasladoY;
    private boolean arrastreMapa;

    private record DatosEvento(List<Localidad> localidades,
            Map<Integer, List<Asiento>> asientos) { }

    @FXML
    private void initialize() {
        panelTarjetas.prefWrapLengthProperty().bind(scrollCartelera.widthProperty().subtract(85));
        panelLocalidades.setPrefWrapLength(280);
        configurarZoomMapa();
        cargarCartelera();
    }

    @FXML
    private void actualizar() {
        if (scrollDetalle.isVisible() && eventoActual != null) {
            // Se conserva la vista del concierto, pero se vuelve a consultar el inventario.
            cargarLocalidades(eventoActual);
        } else {
            cargarCartelera();
        }
    }

    @FXML
    private void volver() {
        eventoActual = null;
        localidadActual = null;
        mostrarCartelera(true);
        cargarCartelera();
    }

    private void mostrarCartelera(boolean mostrar) {
        scrollCartelera.setVisible(mostrar);
        scrollCartelera.setManaged(mostrar);
        scrollDetalle.setVisible(!mostrar);
        scrollDetalle.setManaged(!mostrar);
    }

    private void cargarCartelera() {
        btnActualizar.setDisable(true);
        lblEstado.setText("Consultando eventos en Neon...");
        Task<List<Evento>> tarea = new Task<>() {
            @Override protected List<Evento> call() throws Exception {
                return dao.listarEventos();
            }
        };
        tarea.setOnSucceeded(e -> {
            btnActualizar.setDisable(false);
            panelTarjetas.getChildren().clear();
            List<Evento> eventos = tarea.getValue();
            if (eventos.isEmpty()) {
                Label mensaje = new Label("Por el momento no hay conciertos programados o activos.");
                mensaje.getStyleClass().add("placeholder-eventos");
                panelTarjetas.getChildren().add(mensaje);
            } else {
                for (Evento concierto : eventos) {
                    panelTarjetas.getChildren().add(crearTarjetaEvento(concierto));
                }
            }
            lblEstado.setText("Cartelera actualizada · " + eventos.size() + " evento(s)");
        });
        tarea.setOnFailed(e -> {
            btnActualizar.setDisable(false);
            lblEstado.setText("No fue posible cargar los eventos: " + mensajeError(tarea.getException()));
        });
        ejecutar(tarea, "eventos-cartelera");
    }

    private VBox crearTarjetaEvento(Evento evento) {
        VBox tarjeta = new VBox(11);
        tarjeta.setPrefWidth(360);
        tarjeta.setMaxWidth(360);
        tarjeta.getStyleClass().add("card-evento");

        StackPane portada = new StackPane();
        portada.setPrefSize(340, 191);
        portada.setMinSize(340, 191);
        portada.getStyleClass().add("imagen-evento");
        if (evento.banner() != null && evento.banner().length > 0) {
            try {
                Image imagen = new Image(new ByteArrayInputStream(evento.banner()));
                if (!imagen.isError()) {
                    ImageView imagenVista = new ImageView(imagen);
                    imagenVista.setFitWidth(340);
                    imagenVista.setFitHeight(191);
                    imagenVista.setPreserveRatio(false);
                    portada.getChildren().add(imagenVista);
                } else {
                    ponerPlaceholder(portada);
                }
            } catch (Exception ex) {
                ponerPlaceholder(portada);
            }
        } else {
            ponerPlaceholder(portada);
        }

        Label titulo = new Label(evento.titulo());
        titulo.getStyleClass().add("titulo-evento");
        titulo.setWrapText(true);
        Label artista = crearLabel("ARTISTA · " + texto(evento.artista()), "dato-evento");
        Label fecha = crearLabel("FECHA · " + fecha(evento.fecha()), "dato-evento");
        Label lugar = crearLabel("LUGAR · " + texto(evento.recinto()), "dato-evento");
        lugar.setWrapText(true);
        Button abrir = new Button("Ver localidades  →");
        abrir.getStyleClass().add("boton-evento");
        abrir.setMaxWidth(Double.MAX_VALUE);
        abrir.setOnAction(e -> abrirEvento(evento));
        tarjeta.getChildren().addAll(portada, titulo, artista, fecha, lugar, abrir);
        return tarjeta;
    }

    private void ponerPlaceholder(StackPane portada) {
        Label marca = crearLabel("SVB-GUA · EVENTOS", "marca-sin-imagen");
        portada.getChildren().add(marca);
    }

    private void abrirEvento(Evento evento) {
        eventoActual = evento;
        localidadActual = null;
        panelAsientos.setManaged(false);
        panelAsientos.setVisible(false);
        panelMapaPublico.getChildren().clear();
        lblInfoMapa.setText("Cargando plano del evento desde Neon...");
        restablecerZoom();
        lblDetalleTitulo.setText(evento.titulo());
        lblDetalleMeta.setText(texto(evento.artista()) + "  ·  " + fecha(evento.fecha())
                + "  ·  " + texto(evento.recinto()));
        mostrarCartelera(false);
        scrollDetalle.setVvalue(0);
        cargarLocalidades(evento);
    }

    private void cargarLocalidades(Evento evento) {
        final int id = evento.id();
        localidadActual = null;
        asientosDelEvento = Map.of();
        panelAsientos.setManaged(false);
        panelAsientos.setVisible(false);
        panelLocalidades.getChildren().clear();
        panelMapaPublico.getChildren().clear();
        lblInfoMapa.setText("Consultando localidades y asientos en Neon...");
        lblEstado.setText("Consultando plano y disponibilidad...");
        btnActualizar.setDisable(true);

        Task<DatosEvento> tarea = new Task<>() {
            @Override protected DatosEvento call() throws Exception {
                List<Localidad> localidades = dao.listarLocalidades(id);
                Map<Integer, List<Asiento>> asientos = new HashMap<>();
                for (Localidad l : localidades) {
                    if ("numerado".equalsIgnoreCase(l.tipoVenta())) {
                        asientos.put(l.idInventario(),
                                dao.listarAsientos(l.idInventario()));
                    }
                }
                return new DatosEvento(localidades, asientos);
            }
        };
        tarea.setOnSucceeded(e -> {
            btnActualizar.setDisable(false);
            if (eventoActual == null || eventoActual.id() != id) return;
            DatosEvento datos = tarea.getValue();
            asientosDelEvento = datos.asientos();
            for (Localidad l : datos.localidades()) {
                panelLocalidades.getChildren().add(crearTarjetaLocalidad(l));
            }
            if (datos.localidades().isEmpty()) {
                panelLocalidades.getChildren().add(crearLabel(
                        "Este concierto aún no tiene localidades.", "placeholder-eventos"));
                lblInfoMapa.setText("El plano aparecerá cuando se registren localidades.");
            } else {
                MapaEventosPublicos.dibujar(panelMapaPublico,
                        datos.localidades(), datos.asientos(), this::seleccionarDesdeMapa);
                lblInfoMapa.setText("Haz clic en cualquier zona o asiento del plano. "
                        + "La disponibilidad no se reserva aquí.");
            }
            lblEstado.setText("Plano actualizado desde Neon · "
                    + datos.localidades().size() + " localidades");
        });
        tarea.setOnFailed(e -> {
            btnActualizar.setDisable(false);
            if (eventoActual != null && eventoActual.id() == id) {
                lblInfoMapa.setText("No se pudo cargar el mapa. Usa Actualizar para reintentar.");
                lblEstado.setText("Error al consultar Neon: "
                        + mensajeError(tarea.getException()));
            }
        });
        ejecutar(tarea, "eventos-mapa-consulta");
    }

    private VBox crearTarjetaLocalidad(Localidad l) {
        VBox tarjeta = new VBox(11);
        tarjeta.setPrefWidth(270);
        tarjeta.setMaxWidth(270);
        tarjeta.getStyleClass().add("card-localidad");
        Label nombre = crearLabel(l.nombre(), "nombre-localidad");
        Label precio = crearLabel(moneda(l.precio()), "precio-localidad");
        String tipo = "numerado".equalsIgnoreCase(l.tipoVenta()) ? "Asientos numerados" : "Admisión general";
        Label detalle = crearLabel(tipo + " · " + l.disponibles() + " disponibles de " + l.capacidad(),
                "disponibilidad-localidad");
        detalle.setWrapText(true);
        Button ver = new Button("numerado".equalsIgnoreCase(l.tipoVenta())
                ? "Ver filas y asientos  →" : "Ver disponibilidad  →");
        ver.getStyleClass().add("boton-evento");
        ver.setMaxWidth(Double.MAX_VALUE);
        ver.setOnAction(e -> seleccionarLocalidad(l));
        tarjeta.getChildren().addAll(nombre, precio, detalle, ver);
        return tarjeta;
    }

    private void seleccionarLocalidad(Localidad l) {
        seleccionarLocalidad(l, null);
    }

    private void seleccionarDesdeMapa(Localidad l, Asiento asiento) {
        seleccionarLocalidad(l, asiento);
    }

    private void seleccionarLocalidad(Localidad l, Asiento asientoDesdeMapa) {
        localidadActual = l;
        panelAsientos.setVisible(true);
        panelAsientos.setManaged(true);
        lblLocalidadElegida.setText(l.nombre() + " · " + moneda(l.precio()));
        lblEleccion.setText("Consulta pública: no hay reserva ni cobro.");
        filasAsientos.getChildren().clear();
        asientoMarcado = null;

        if (!"numerado".equalsIgnoreCase(l.tipoVenta())) {
            lblInfoMapa.setText(l.nombre() + " · Admisión general · "
                    + l.disponibles() + " de " + l.capacidad()
                    + " boletos disponibles. Sin filas ni asientos asignados.");
            filasAsientos.getChildren().add(crearLabel(
                    "Admisión general: " + l.disponibles()
                            + " boletos disponibles de " + l.capacidad()
                            + ". Indica la localidad al vendedor.",
                    "mensaje-seleccion"));
            return;
        }

        List<Asiento> asientos = asientosDelEvento.getOrDefault(l.idInventario(), List.of());
        if (asientos.isEmpty()) {
            filasAsientos.getChildren().add(crearLabel(
                    "Todavía no hay asientos numerados para esta localidad.",
                    "mensaje-seleccion"));
        } else {
            mostrarFilas(asientos, l);
        }
        if (asientoDesdeMapa == null) {
            lblInfoMapa.setText(l.nombre() + " · " + l.disponibles()
                    + " asientos disponibles. Consulta las filas detalladas más abajo.");
        } else {
            String estado = asientoDesdeMapa.estado() == null
                    ? "desconocido" : asientoDesdeMapa.estado();
            String detalle = l.nombre() + " · FILA " + asientoDesdeMapa.fila()
                    + " · ASIENTO " + asientoDesdeMapa.numero()
                    + " · " + estado.toUpperCase(Locale.ROOT);
            lblInfoMapa.setText(detalle);
            lblEleccion.setText("Consulta en taquilla: " + detalle
                    + ". Confirma disponibilidad con el vendedor.");
        }
    }


    private void mostrarFilas(List<Asiento> asientos, Localidad localidad) {
        Map<String, FlowPane> porFila = new LinkedHashMap<>();
        for (Asiento asiento : asientos) {
            String fila = asiento.fila() == null ? "SIN FILA" : asiento.fila();
            FlowPane contenedor = porFila.get(fila);
            if (contenedor == null) {
                contenedor = new FlowPane(8, 8);
                contenedor.setPrefWrapLength(900);
                porFila.put(fila, contenedor);
            }
            Button boton = new Button(String.valueOf(asiento.numero()));
            boton.setPrefWidth(45);
            boton.setMinHeight(39);
            boton.getStyleClass().add("asiento");
            String estado = asiento.estado() == null ? "bloqueado"
                    : asiento.estado().trim().toLowerCase(Locale.ROOT);
            switch (estado) {
                case "disponible" -> {
                    boton.getStyleClass().add("asiento-disponible");
                    boton.setOnAction(e -> {
                        if (asientoMarcado != null) asientoMarcado.getStyleClass().remove("asiento-elegido");
                        asientoMarcado = boton;
                        boton.getStyleClass().add("asiento-elegido");
                        lblEleccion.setText("Solicita en taquilla: " + localidad.nombre() +
                                " · FILA " + fila + " · ASIENTO " + asiento.numero());
                    });
                }
                case "vendido" -> { boton.getStyleClass().add("asiento-vendido"); boton.setDisable(true); }
                case "reservado" -> { boton.getStyleClass().add("asiento-reservado"); boton.setDisable(true); }
                default -> { boton.getStyleClass().add("asiento-bloqueado"); boton.setDisable(true); }
            }
            Tooltip.install(boton, new Tooltip("Fila " + fila + " · Asiento " + asiento.numero() + " · " + estado));
            contenedor.getChildren().add(boton);
        }
        filasAsientos.getChildren().clear();
        porFila.forEach((fila, botones) -> {
            HBox filaVisual = new HBox(14);
            filaVisual.setAlignment(Pos.TOP_LEFT);
            Label rotulo = crearLabel("FILA " + fila, "fila-texto");
            rotulo.setMinWidth(82);
            filaVisual.getChildren().addAll(rotulo, botones);
            HBox.setHgrow(botones, Priority.ALWAYS);
            filasAsientos.getChildren().add(filaVisual);
        });
    }

    @FXML private void zoomMas() { cambiarZoom(1.2); }
    @FXML private void zoomMenos() { cambiarZoom(1.0 / 1.2); }
    @FXML private void resetZoom() { restablecerZoom(); }

    private void cambiarZoom(double factor) {
        zoom = Math.max(1.0, Math.min(3.0, zoom * factor));
        escalaMapa.setX(zoom);
        escalaMapa.setY(zoom);
    }

    private void restablecerZoom() {
        zoom = 1.0;
        escalaMapa.setX(1.0);
        escalaMapa.setY(1.0);
        panelMapaPublico.setTranslateX(0);
        panelMapaPublico.setTranslateY(0);
    }

    private void configurarZoomMapa() {
        Rectangle recorte = new Rectangle();
        recorte.widthProperty().bind(contenedorMapaPublico.widthProperty());
        recorte.heightProperty().bind(contenedorMapaPublico.heightProperty());
        contenedorMapaPublico.setClip(recorte);
        panelMapaPublico.getTransforms().add(escalaMapa);
        contenedorMapaPublico.setOnScroll(e -> {
            if (!panelMapaPublico.getChildren().isEmpty()) {
                cambiarZoom(e.getDeltaY() > 0 ? 1.15 : 1.0 / 1.15);
                e.consume();
            }
        });
        contenedorMapaPublico.setOnMousePressed(e -> {
            if (e.getButton() != MouseButton.PRIMARY || zoom <= 1.0) return;
            inicioX = e.getSceneX();
            inicioY = e.getSceneY();
            trasladoX = panelMapaPublico.getTranslateX();
            trasladoY = panelMapaPublico.getTranslateY();
            arrastreMapa = true;
        });
        contenedorMapaPublico.setOnMouseDragged(e -> {
            if (!arrastreMapa) return;
            panelMapaPublico.setTranslateX(trasladoX + e.getSceneX() - inicioX);
            panelMapaPublico.setTranslateY(trasladoY + e.getSceneY() - inicioY);
            e.consume();
        });
        contenedorMapaPublico.setOnMouseReleased(e -> arrastreMapa = false);
    }

    private static Label crearLabel(String texto, String clase) {
        Label label = new Label(texto);
        label.getStyleClass().add(clase);
        return label;
    }

    private static String texto(String valor) {
        return valor == null || valor.isBlank() ? "Sin especificar" : valor;
    }

    private static String fecha(LocalDateTime valor) {
        return valor == null ? "Por confirmar" : valor.format(FORMATO_FECHA);
    }

    private static String moneda(BigDecimal precio) {
        if (precio == null) return "Q 0.00";
        return "Q " + new DecimalFormat("#,##0.00").format(precio);
    }

    private static String mensajeError(Throwable ex) {
        if (ex == null) return "Error desconocido";
        return ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage();
    }

    private static void ejecutar(Task<?> tarea, String nombre) {
        Thread hilo = new Thread(tarea, nombre);
        hilo.setDaemon(true);
        hilo.start();
    }
}
