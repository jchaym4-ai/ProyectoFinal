
package controladorFx;

import dao.AsientoDAO;
import dao.ConciertoDAO;
import dao.MapaLocalidadDAO;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Group;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Ellipse;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.util.StringConverter;
import modelo.Asiento;
import modelo.Concierto;
import modelo.MapaLocalidad;
import modelo.Usuario;

import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.input.MouseButton;
import javafx.scene.transform.Scale;


public class NuevaVentaFXController {

    @FXML private ComboBox<Concierto> cmbConciertos;
    @FXML private Button btnConsultarConcierto;
    @FXML private StackPane contenedorMapa;
    @FXML private AnchorPane panelMapa;
    @FXML private Label lblLocalidadSeleccionada;
    @FXML private Label lblPrecioUnitario;
    @FXML private Label lblDisponibles;
    @FXML private Spinner<Integer> spnCantidad;
    @FXML private Label lblAsientosSeleccionados;
    @FXML private Label lblTotal;
    @FXML private Button btnConfirmarVenta;
    
    private static final double ZOOM_MINIMO = 1.0;
    private static final double ZOOM_MAXIMO = 3.0;
    private static final double PASO_ZOOM = 1.15;

    private double nivelZoom = 1.0;
    private double arrastreInicioX;
    private double arrastreInicioY;
    private double traslacionInicioX;
    private double traslacionInicioY;

    private final Scale escalaMapa = new Scale(1, 1, 295, 285);
    
    private boolean arrastrandoMapa = false;



    private Usuario usuario;
    private MapaLocalidad localidadSeleccionada;

    private final ConciertoDAO conciertoDAO =
            new ConciertoDAO();

    private final MapaLocalidadDAO mapaDAO =
            new MapaLocalidadDAO();

    private final AsientoDAO asientoDAO =
            new AsientoDAO();

    private final Map<Integer, Asiento> asientosSeleccionados =
            new LinkedHashMap<>();

    private final Map<Integer, Rectangle> casillasSeleccionadas =
            new HashMap<>();

    private final Map<Integer, Color> coloresOriginales =
            new HashMap<>();

    private final DateTimeFormatter formatoFecha =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @FXML
    private void initialize() {

        configurarSpinner();
        configurarComboBox();
        limpiarResumen();
        configurarZoomMapa();

        btnConsultarConcierto.setDisable(true);
        btnConfirmarVenta.setDisable(true);

        cmbConciertos.valueProperty().addListener(
                (obs, anterior, nuevo) -> {
                    limpiarResumen();
                    btnConsultarConcierto.setDisable(
                            nuevo == null
                    );
                }
        );

        spnCantidad.valueProperty().addListener(
                (obs, anterior, nuevo) -> actualizarTotal()
        );

        btnConsultarConcierto.setOnAction(
                e -> consultarConcierto()
        );

        cargarConciertos();
    }

    public void setUsuario(Usuario usuario) {

        if (usuario == null) {
            throw new IllegalArgumentException(
                    "El vendedor no puede ser nulo."
            );
        }

        this.usuario = usuario;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    private void configurarComboBox() {

        cmbConciertos.setConverter(
                new StringConverter<Concierto>() {

                    @Override
                    public String toString(Concierto concierto) {

                        if (concierto == null) {
                            return "";
                        }

                        String fecha = "Sin fecha";

                        if (concierto.getFechaConcierto() != null) {
                            fecha = concierto.getFechaConcierto()
                                    .format(formatoFecha);
                        }

                        return concierto.getTituloEvento()
                                + " - " + fecha
                                + " - " + concierto.getRecinto();
                    }

                    @Override
                    public Concierto fromString(String texto) {
                        return null;
                    }
                }
        );
    }

    private void cargarConciertos() {

        cmbConciertos.getItems().clear();

        for (Concierto concierto : conciertoDAO.listarTodos()) {

            String estado = concierto.getEstado();

            if ("programado".equalsIgnoreCase(estado)
                    || "activo".equalsIgnoreCase(estado)) {

                cmbConciertos.getItems().add(concierto);
            }
        }
    }

    private void configurarSpinner() {

        spnCantidad.setValueFactory(
                new SpinnerValueFactory.IntegerSpinnerValueFactory(
                        0, 0, 0
                )
        );

        spnCantidad.setEditable(false);
        spnCantidad.setDisable(true);
    }

    private void consultarConcierto() {

        Concierto concierto = cmbConciertos.getValue();

        if (concierto == null) {
            return;
        }

        limpiarResumen();

        try {

            List<MapaLocalidad> localidades =
                    mapaDAO.listarPorConcierto(
                            concierto.getIdConcierto()
                    );

            if (localidades.isEmpty()) {
                mostrarMensajeMapa(
                        "Este concierto no tiene localidades registradas."
                );
                return;
            }

            dibujarMapa(localidades);

        } catch (SQLException e) {

            mostrarMensajeMapa(
                    "No se pudo cargar el mapa."
            );

            Alert alerta = new Alert(Alert.AlertType.ERROR);
            alerta.setTitle("Error");
            alerta.setHeaderText(
                    "Error al consultar PostgreSQL"
            );
            alerta.setContentText(e.getMessage());
            alerta.showAndWait();
        }
    }

    private void dibujarMapa(
            List<MapaLocalidad> localidades) throws SQLException {

        panelMapa.getChildren().clear();
        panelMapa.setPrefSize(590, 650);

        List<List<Asiento>> asientosPorLocalidad =
                new ArrayList<>();

        List<Integer> anillosPorLocalidad =
                new ArrayList<>();

        int totalAnillos = 0;

        for (MapaLocalidad localidad : localidades) {

            List<Asiento> asientos = new ArrayList<>();

            int anillos;

            if ("numerado".equalsIgnoreCase(
                    localidad.getTipoVenta())) {

                asientos = asientoDAO.listarPorInventario(
                        localidad.getIdInventario()
                );

                anillos = Math.max(
                        1,
                        (int) Math.ceil(asientos.size() / 65.0)
                );

            } else {

                anillos = 2;
            }

            asientosPorLocalidad.add(asientos);
            anillosPorLocalidad.add(anillos);
            totalAnillos += anillos;
        }

        Ellipse estadio = new Ellipse(
                295, 285, 280, 222
        );

        estadio.setFill(Color.web("#E2E8F0"));
        estadio.setStroke(Color.web("#CBD5E1"));
        estadio.setStrokeWidth(4);

        panelMapa.getChildren().add(estadio);

        FlowPane leyenda = new FlowPane();

        leyenda.setHgap(8);
        leyenda.setVgap(8);
        leyenda.setPrefWrapLength(540);
        leyenda.setLayoutX(20);
        leyenda.setLayoutY(530);

        int anilloActual = 0;

        for (int indice = 0;
                indice < localidades.size();
                indice++) {

            MapaLocalidad localidad = localidades.get(indice);

            int cantidadAnillos =
                    anillosPorLocalidad.get(indice);

            List<Asiento> asientos =
                    asientosPorLocalidad.get(indice);

            dibujarLocalidad(
                    localidad,
                    asientos,
                    anilloActual,
                    cantidadAnillos,
                    totalAnillos,
                    leyenda
            );

            anilloActual += cantidadAnillos;
        }

        Ellipse interior = new Ellipse(
                295, 285, 110, 78
        );

        interior.setFill(Color.web("#F8FAFC"));
        interior.setStroke(Color.web("#CBD5E1"));
        interior.setStrokeWidth(2);

        Rectangle escenario = new Rectangle(
                234, 253, 122, 64
        );

        escenario.setArcWidth(16);
        escenario.setArcHeight(16);
        escenario.setFill(Color.web("#334155"));

        Label lblEscenario = new Label("ESCENARIO");

        lblEscenario.setFont(
                Font.font("System", FontWeight.BOLD, 15)
        );

        lblEscenario.setTextFill(Color.WHITE);
        lblEscenario.setAlignment(Pos.CENTER);
        lblEscenario.setLayoutX(234);
        lblEscenario.setLayoutY(273);
        lblEscenario.setPrefWidth(122);

        panelMapa.getChildren().addAll(
                interior,
                escenario,
                lblEscenario,
                leyenda
        );

        Label nota = new Label(
                "Selecciona una casilla para consultar su asiento."
        );

        nota.setTextFill(Color.web("#64748B"));
        nota.setFont(Font.font("System", 12));
        nota.setLayoutX(20);
        nota.setLayoutY(615);

        panelMapa.getChildren().add(nota);
    }

    private void dibujarLocalidad(
            MapaLocalidad localidad,
            List<Asiento> asientos,
            int anilloInicial,
            int cantidadAnillos,
            int totalAnillos,
            FlowPane leyenda) {

        Color color = Color.web(
                colorSeguro(localidad.getColor())
        );

        boolean numerado = "numerado".equalsIgnoreCase(
                localidad.getTipoVenta()
        );

        int indiceAsiento = 0;
        Group grupoAforo = new Group();

        for (int anillo = 0;
                anillo < cantidadAnillos;
                anillo++) {

            int posicionAnillo = anilloInicial + anillo;

            double proporcion =
                    (posicionAnillo + 0.5)
                    / Math.max(1.0, totalAnillos);

            double radioX = 119 + proporcion * 147;
            double radioY = 87 + proporcion * 124;

            int cantidadCasillas;

            if (numerado) {

                int restantes =
                        asientos.size() - indiceAsiento;

                int anillosRestantes =
                        cantidadAnillos - anillo;

                cantidadCasillas = (int) Math.ceil(
                        (double) restantes / anillosRestantes
                );

            } else {

                cantidadCasillas = 60;
            }

            for (int posicion = 0;
                    posicion < cantidadCasillas;
                    posicion++) {

                double angulo =
                        (2 * Math.PI * posicion)
                        / cantidadCasillas;

                double x = 295 + radioX * Math.cos(angulo);
                double y = 285 + radioY * Math.sin(angulo);

                Rectangle casilla = new Rectangle(7, 9);

                casilla.setX(x - 3.5);
                casilla.setY(y - 4.5);
                casilla.setRotate(Math.toDegrees(angulo) + 90);
                casilla.setArcWidth(2);
                casilla.setArcHeight(2);
                casilla.setStroke(Color.WHITE);
                casilla.setStrokeWidth(0.5);

                if (numerado) {

                    Asiento asiento =
                            asientos.get(indiceAsiento++);

                    boolean disponible =
                            "disponible".equalsIgnoreCase(
                                    asiento.getEstado()
                            );

                    casilla.setFill(
                            disponible
                            ? color
                            : Color.web("#94A3B8")
                    );

                    Tooltip.install(
                            casilla,
                            new Tooltip(
                                    localidad.getNombreLocalidad()
                                    + "\nFila: " + asiento.getFila()
                                    + "\nAsiento: " + asiento.getNumero()
                                    + "\nEstado: " + asiento.getEstado()
                            )
                    );

                    
                    if (disponible) {

                        casilla.setCursor(Cursor.HAND);

                        casilla.setOnMousePressed(e -> {
                            arrastrandoMapa = false;
                            e.consume();
                        });

                        casilla.setOnMouseClicked(e -> {
                            seleccionarAsiento(
                                    localidad,
                                    asiento,
                                    casilla,
                                    color
                            );
                            e.consume();
                        });
                    }


                    panelMapa.getChildren().add(casilla);

                } else {

                    casilla.setFill(
                            localidad.getCantidadDisponible() > 0
                            ? color
                            : Color.web("#94A3B8")
                    );

                    grupoAforo.getChildren().add(casilla);
                }
            }
        }

        if (!numerado) {

            if (localidad.getCantidadDisponible() > 0) {

                grupoAforo.setCursor(Cursor.HAND);

                grupoAforo.setOnMouseClicked(
                        e -> seleccionarAforo(localidad)
                );
            }

            panelMapa.getChildren().add(grupoAforo);
        }

        Rectangle muestra = new Rectangle(14, 14);
        muestra.setArcWidth(3);
        muestra.setArcHeight(3);
        muestra.setFill(color);

        Label nombre = new Label(
                localidad.getNombreLocalidad()
        );

        nombre.setFont(
                Font.font("System", FontWeight.BOLD, 11)
        );

        HBox elemento = new HBox(6, muestra, nombre);
        elemento.setAlignment(Pos.CENTER_LEFT);
        elemento.setStyle(
                "-fx-background-color: white;"
                + "-fx-border-color: #E2E8F0;"
                + "-fx-border-radius: 7px;"
                + "-fx-background-radius: 7px;"
                + "-fx-padding: 7px;"
        );

        if (!numerado
                && localidad.getCantidadDisponible() > 0) {

            elemento.setCursor(Cursor.HAND);
            elemento.setOnMouseClicked(
                    e -> seleccionarAforo(localidad)
            );
        }

        leyenda.getChildren().add(elemento);
    }

    private void seleccionarAsiento(
            MapaLocalidad localidad,
            Asiento asiento,
            Rectangle casilla,
            Color colorOriginal) {

        if (localidadSeleccionada == null
                || localidadSeleccionada.getIdInventario()
                != localidad.getIdInventario()) {

            limpiarAsientosSeleccionados();
            localidadSeleccionada = localidad;
        }

        int idAsiento = asiento.getIdAsiento();

        if (asientosSeleccionados.containsKey(idAsiento)) {

            asientosSeleccionados.remove(idAsiento);
            casilla.setFill(colorOriginal);
            casillasSeleccionadas.remove(idAsiento);
            coloresOriginales.remove(idAsiento);

        } else {

            asientosSeleccionados.put(idAsiento, asiento);
            casillasSeleccionadas.put(idAsiento, casilla);
            coloresOriginales.put(idAsiento, colorOriginal);

            casilla.setFill(Color.web("#2563EB"));
        }

        configurarSpinner();

        lblLocalidadSeleccionada.setText(
                localidad.getNombreLocalidad()
        );

        lblPrecioUnitario.setText(
                formatearPrecio(localidad.getPrecio())
        );

        lblDisponibles.setText(
                localidad.getCantidadDisponible() + " boletos"
        );

        actualizarResumenNumerado();
    }

    private void seleccionarAforo(MapaLocalidad localidad) {

        limpiarAsientosSeleccionados();

        localidadSeleccionada = localidad;

        lblLocalidadSeleccionada.setText(
                localidad.getNombreLocalidad()
        );

        lblPrecioUnitario.setText(
                formatearPrecio(localidad.getPrecio())
        );

        lblDisponibles.setText(
                localidad.getCantidadDisponible() + " boletos"
        );

        int disponibles = localidad.getCantidadDisponible();

        spnCantidad.setValueFactory(
                new SpinnerValueFactory.IntegerSpinnerValueFactory(
                        1, disponibles, 1
                )
        );

        spnCantidad.setDisable(false);

        lblAsientosSeleccionados.setText(
                "No aplica (aforo)"
        );

        actualizarTotal();
    }

    private void actualizarResumenNumerado() {

        int cantidad = asientosSeleccionados.size();

        if (cantidad == 0) {

            lblAsientosSeleccionados.setText("Ninguno");
            lblTotal.setText("Q 0.00");
            return;
        }

        StringBuilder detalle = new StringBuilder();

        int mostrados = 0;

        for (Asiento asiento : asientosSeleccionados.values()) {

            if (mostrados == 8) {
                break;
            }

            if (mostrados > 0) {
                detalle.append(", ");
            }

            detalle.append(asiento.getFila())
                    .append("-")
                    .append(asiento.getNumero());

            mostrados++;
        }

        if (cantidad > mostrados) {
            detalle.append(" +")
                    .append(cantidad - mostrados)
                    .append(" más");
        }

        lblAsientosSeleccionados.setText(
                detalle.toString()
        );

        actualizarTotal();
    }

    private void actualizarTotal() {

        if (localidadSeleccionada == null) {

            lblTotal.setText("Q 0.00");
            return;
        }

        int cantidad;

        if ("numerado".equalsIgnoreCase(
                localidadSeleccionada.getTipoVenta())) {

            cantidad = asientosSeleccionados.size();

        } else {

            Integer valor = spnCantidad.getValue();
            cantidad = valor == null ? 0 : valor;
        }

        BigDecimal total = localidadSeleccionada
                .getPrecio()
                .multiply(BigDecimal.valueOf(cantidad));

        lblTotal.setText(formatearPrecio(total));
    }

    private void limpiarAsientosSeleccionados() {

        for (Map.Entry<Integer, Rectangle> entrada
                : casillasSeleccionadas.entrySet()) {

            Color original = coloresOriginales.get(
                    entrada.getKey()
            );

            if (original != null) {
                entrada.getValue().setFill(original);
            }
        }

        asientosSeleccionados.clear();
        casillasSeleccionadas.clear();
        coloresOriginales.clear();
    }

    private String formatearPrecio(BigDecimal precio) {

        if (precio == null) {
            return "Q 0.00";
        }

        return String.format(
                Locale.US,
                "Q %,.2f",
                precio
        );
    }

    private String colorSeguro(String color) {

        if (color != null
                && color.matches("#[0-9a-fA-F]{6}")) {

            return color;
        }

        return "#64748B";
    }

    private void mostrarMensajeMapa(String texto) {

        panelMapa.getChildren().clear();

        Label mensaje = new Label(texto);
        mensaje.setWrapText(true);
        mensaje.setLayoutX(20);
        mensaje.setLayoutY(20);
        mensaje.setMaxWidth(450);

        panelMapa.getChildren().add(mensaje);
    }

    private void limpiarResumen() {

        limpiarAsientosSeleccionados();

        localidadSeleccionada = null;

        lblLocalidadSeleccionada.setText(
                "Sin seleccionar"
        );

        lblPrecioUnitario.setText("Q 0.00");
        lblDisponibles.setText("0 boletos");
        lblAsientosSeleccionados.setText("Ninguno");
        lblTotal.setText("Q 0.00");

        configurarSpinner();

        panelMapa.getChildren().clear();

        btnConfirmarVenta.setDisable(true);
        restablecerZoom();
    }

    private void configurarZoomMapa() {
        
        javafx.scene.shape.Rectangle recorte =
                new javafx.scene.shape.Rectangle();

        recorte.widthProperty().bind(
                contenedorMapa.widthProperty()
        );

        recorte.heightProperty().bind(
                contenedorMapa.heightProperty()
        );

        contenedorMapa.setClip(recorte);


        panelMapa.getTransforms().add(escalaMapa);

        contenedorMapa.setOnScroll(event -> {

            if (panelMapa.getChildren().isEmpty()) {
                return;
            }

            double factor = event.getDeltaY() > 0
                    ? PASO_ZOOM
                    : 1.0 / PASO_ZOOM;

            aplicarZoom(factor);

            event.consume();
        });


        contenedorMapa.setOnMousePressed(event -> {

            if (event.getButton() != MouseButton.PRIMARY
                    && event.getButton() != MouseButton.SECONDARY
                    && event.getButton() != MouseButton.MIDDLE) {
                return;
            }

            if (event.getButton() == MouseButton.PRIMARY
                    && event.getTarget() instanceof Rectangle
                    && nivelZoom <= 1.0) {
                return;
            }

            arrastrandoMapa = true;

            arrastreInicioX = event.getSceneX();
            arrastreInicioY = event.getSceneY();

            traslacionInicioX = panelMapa.getTranslateX();
            traslacionInicioY = panelMapa.getTranslateY();
        });

        contenedorMapa.setOnMouseDragged(event -> {

            if (!arrastrandoMapa) {
                return;
            }

            double desplazamientoX =
                    event.getSceneX() - arrastreInicioX;

            double desplazamientoY =
                    event.getSceneY() - arrastreInicioY;

            panelMapa.setTranslateX(
                    traslacionInicioX + desplazamientoX
            );

            panelMapa.setTranslateY(
                    traslacionInicioY + desplazamientoY
            );
        });

        contenedorMapa.setOnMouseReleased(event -> {
            arrastrandoMapa = false;
        });

        contenedorMapa.setOnMouseClicked(event -> {

            if (event.getButton() == MouseButton.SECONDARY
                    && event.getClickCount() == 2) {

                restablecerZoom();
                event.consume();
            }
        });
    }

    private void aplicarZoom(double factor) {

        double nuevoZoom = nivelZoom * factor;

        nuevoZoom = Math.max(
                ZOOM_MINIMO,
                Math.min(ZOOM_MAXIMO, nuevoZoom)
        );

        nivelZoom = nuevoZoom;

        escalaMapa.setX(nivelZoom);
        escalaMapa.setY(nivelZoom);
    }

    private void restablecerZoom() {

        nivelZoom = 1.0;

        escalaMapa.setX(1.0);
        escalaMapa.setY(1.0);

        panelMapa.setTranslateX(0);
        panelMapa.setTranslateY(0);
    }

}
