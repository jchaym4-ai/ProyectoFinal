
package controladorFx;

import dao.AsientoDAO;
import dao.ConciertoDAO;
import dao.MapaLocalidadDAO;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
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
import javafx.scene.layout.BorderPane;
import javafx.application.Platform;
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
import servicio.VentaService;
import servicio.FacturaPDFService;
import servicio.PublicacionBoletosService;
import javafx.concurrent.Task;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.TextField;
import javafx.scene.control.ButtonBar;
import javafx.scene.layout.GridPane;
import javafx.geometry.HPos;
import javafx.scene.control.TextFormatter;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.DirectoryChooser;
import dao.BoletoDAO;
import modelo.Boleto;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;



public class NuevaVentaFXController {

    @FXML private ComboBox<Concierto> cmbConciertos;
    @FXML private Button btnConsultarConcierto;
    @FXML private ScrollPane scrollNuevaVenta;
    @FXML private BorderPane layoutVenta;
    @FXML private VBox panelResumen;
    @FXML private StackPane contenedorMapa;
    @FXML private AnchorPane panelMapa;
    @FXML private Label lblLocalidadSeleccionada;
    @FXML private Label lblPrecioUnitario;
    @FXML private Label lblDisponibles;
    @FXML private Spinner<Integer> spnCantidad;
    @FXML private Label lblAsientosSeleccionados;
    @FXML private Label lblTotal;
    @FXML private Button btnConfirmarVenta;
    
    // El dibujo original usa coordenadas constantes; el zoom se adapta al visor.
    private static final double ANCHO_MAPA_BASE = 590.0;
    private static final double ALTO_MAPA_BASE = 650.0;
    private static final double UMBRAL_RESUMEN_ABAJO = 880.0;

    private static final double ZOOM_MINIMO = 1.0;
    private static final double ZOOM_MAXIMO = 3.0;
    private static final double PASO_ZOOM = 1.15;

    // Colores de alto contraste. El primero corresponde a la zona mas cara.
    // Si hay mas localidades, se generan tonos adicionales sin duplicados RGB.
    private static final String[] PALETA_LOCALIDADES = {
        "#0D9488", // Turquesa
        "#F59E0B", // Dorado
        "#7C3AED", // Violeta
        "#DC2626", // Rojo
        "#0284C7", // Celeste
        "#65A30D", // Verde lima
        "#DB2777", // Rosa
        "#A16207", // Marron
        "#4338CA", // Indigo
        "#15803D", // Verde
        "#EA580C", // Naranja
        "#0E7490"  // Azul petroleo
    };

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
    
    private final VentaService ventaService =
            new VentaService();


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
        configurarPresentacionAdaptable();

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

        btnConfirmarVenta.setOnAction(
                e -> confirmarVenta()
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
        actualizarTotal();
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

        // Del escenario hacia afuera: precio mas alto primero.
        // En caso de empate, el ID de inventario mantiene un orden estable.
        List<MapaLocalidad> localidadesOrdenadas =
                new ArrayList<>(localidades);
        localidadesOrdenadas.sort((primera, segunda) -> {
            BigDecimal p1 = primera.getPrecio() == null
                    ? BigDecimal.ZERO : primera.getPrecio();
            BigDecimal p2 = segunda.getPrecio() == null
                    ? BigDecimal.ZERO : segunda.getPrecio();
            int porPrecio = p2.compareTo(p1);
            if (porPrecio != 0) {
                return porPrecio;
            }
            return Integer.compare(
                    primera.getIdInventario(),
                    segunda.getIdInventario()
            );
        });

        // Los colores no dependen de Neon ni se repiten dentro del concierto.
        // La misma lista ordenada conserva sus colores al volver a consultar.
        List<Color> coloresLocalidades =
                generarColoresDistintos(localidadesOrdenadas.size());

        List<List<Asiento>> asientosPorLocalidad =
                new ArrayList<>();

        List<Integer> anillosPorLocalidad =
                new ArrayList<>();

        int totalAnillos = 0;

        for (MapaLocalidad localidad : localidadesOrdenadas) {

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
                indice < localidadesOrdenadas.size();
                indice++) {

            MapaLocalidad localidad = localidadesOrdenadas.get(indice);

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
                    coloresLocalidades.get(indice),
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
            Color color,
            FlowPane leyenda) {

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
            btnConfirmarVenta.setDisable(true);
            return;
        }

        int cantidad;

        if ("numerado".equalsIgnoreCase(
                localidadSeleccionada.getTipoVenta())) {

            cantidad = asientosSeleccionados.size();

        } else if ("aforo".equalsIgnoreCase(
                localidadSeleccionada.getTipoVenta())) {

            Integer valor = spnCantidad.getValue();

            cantidad = valor == null ? 0 : valor;

        } else {

            lblTotal.setText("Q 0.00");
            btnConfirmarVenta.setDisable(true);
            return;
        }

        if (cantidad <= 0
                || cantidad > localidadSeleccionada
                        .getCantidadDisponible()) {

            lblTotal.setText("Q 0.00");
            btnConfirmarVenta.setDisable(true);
            return;
        }

        BigDecimal total = localidadSeleccionada
                .getPrecio()
                .multiply(BigDecimal.valueOf(cantidad));

        lblTotal.setText(formatearPrecio(total));

        btnConfirmarVenta.setDisable(usuario == null);
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

    private List<Color> generarColoresDistintos(int cantidad) {

        List<Color> resultado = new ArrayList<>();
        Set<String> usados = new HashSet<>();

        for (int indice = 0; indice < cantidad; indice++) {
            Color color;
            int intento = 0;

            // Paleta visible para las primeras 12 localidades. Para
            // conciertos grandes se distribuyen tonos con angulo dorado.
            do {
                if (indice < PALETA_LOCALIDADES.length && intento == 0) {
                    color = Color.web(PALETA_LOCALIDADES[indice]);
                } else {
                    int semilla = indice + intento * 53;
                    double tono = (semilla * 137.50776405) % 360.0;
                    double saturacion = 0.69 + (semilla % 3) * 0.09;
                    double brillo = 0.73 + ((semilla / 3) % 3) * 0.06;
                    color = Color.hsb(tono, saturacion, brillo);
                }
                intento++;
            } while (!usados.add(claveRGB(color)));

            resultado.add(color);
        }

        return resultado;
    }

    private String claveRGB(Color color) {
        return String.format(Locale.ROOT, "%02X%02X%02X",
                (int) Math.round(color.getRed() * 255.0),
                (int) Math.round(color.getGreen() * 255.0),
                (int) Math.round(color.getBlue() * 255.0));
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

    /**
     * Distribucion responsive: en ventanas estrechas, el resumen de compra
     * se coloca bajo el mapa. El ScrollPane exterior sigue permitiendo bajar.
     */
    private void configurarPresentacionAdaptable() {
        scrollNuevaVenta.viewportBoundsProperty().addListener(
                (obs, anterior, actual) -> {
                    if (actual != null) {
                        actualizarDistribucion(actual.getWidth());
                    }
                });
        Platform.runLater(() ->
                actualizarDistribucion(scrollNuevaVenta.getViewportBounds().getWidth()));
    }

    private void actualizarDistribucion(double anchoVisible) {
        if (anchoVisible <= 0 || layoutVenta == null || panelResumen == null) {
            return;
        }
        boolean compacta = anchoVisible < UMBRAL_RESUMEN_ABAJO;
        if (compacta && layoutVenta.getBottom() != panelResumen) {
            layoutVenta.setRight(null);
            BorderPane.setAlignment(panelResumen, Pos.TOP_CENTER);
            BorderPane.setMargin(panelResumen,
                    new javafx.geometry.Insets(18, 20, 12, 20));
            layoutVenta.setBottom(panelResumen);
        } else if (!compacta && layoutVenta.getRight() != panelResumen) {
            layoutVenta.setBottom(null);
            BorderPane.setAlignment(panelResumen, Pos.TOP_CENTER);
            BorderPane.setMargin(panelResumen, null);
            layoutVenta.setRight(panelResumen);
        }
        Platform.runLater(this::ajustarMapaAlViewport);
    }

    /**
     * El visor tiene dimensiones flexibles, pero el dibujo conserva sus
     * coordenadas fijas (590 x 650) para no afectar la seleccion de asientos.
     */
    private void configurarZoomMapa() {
        Rectangle recorte = new Rectangle();
        recorte.widthProperty().bind(contenedorMapa.widthProperty());
        recorte.heightProperty().bind(contenedorMapa.heightProperty());
        contenedorMapa.setClip(recorte);

        // El hijo no debe imponer un ancho minimo de 590 a la ventana.
        // StackPane tampoco debe redistribuirlo despues de un zoom.
        panelMapa.setManaged(false);
        panelMapa.setMinSize(ANCHO_MAPA_BASE, ALTO_MAPA_BASE);
        panelMapa.setPrefSize(ANCHO_MAPA_BASE, ALTO_MAPA_BASE);
        panelMapa.setMaxSize(ANCHO_MAPA_BASE, ALTO_MAPA_BASE);
        panelMapa.resize(ANCHO_MAPA_BASE, ALTO_MAPA_BASE);
        panelMapa.getTransforms().add(escalaMapa);

        contenedorMapa.widthProperty().addListener((obs, anterior, nuevo) ->
                Platform.runLater(this::ajustarMapaAlViewport));
        contenedorMapa.heightProperty().addListener((obs, anterior, nuevo) ->
                Platform.runLater(this::ajustarMapaAlViewport));
        Platform.runLater(this::ajustarMapaAlViewport);

        contenedorMapa.setOnScroll(event -> {
            if (panelMapa.getChildren().isEmpty() || event.getDeltaY() == 0) {
                return;
            }
            aplicarZoom(event.getDeltaY() > 0
                    ? PASO_ZOOM : 1.0 / PASO_ZOOM);
            event.consume();
        });

        contenedorMapa.setOnMousePressed(event -> {
            arrastrandoMapa = false;
            if (panelMapa.getChildren().isEmpty()
                    || nivelZoom <= 1.0001
                    || (event.getButton() != MouseButton.PRIMARY
                    && event.getButton() != MouseButton.MIDDLE)) {
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
            panelMapa.setTranslateX(traslacionInicioX
                    + event.getSceneX() - arrastreInicioX);
            panelMapa.setTranslateY(traslacionInicioY
                    + event.getSceneY() - arrastreInicioY);
            limitarArrastreMapa();
            event.consume();
        });
        contenedorMapa.setOnMouseReleased(event -> arrastrandoMapa = false);
        contenedorMapa.setOnMouseClicked(event -> {
            if (event.getButton() == MouseButton.SECONDARY
                    && event.getClickCount() == 2) {
                restablecerZoom();
                event.consume();
            }
        });
    }

    @FXML
    private void acercarMapa() {
        aplicarZoom(PASO_ZOOM);
    }

    @FXML
    private void alejarMapa() {
        aplicarZoom(1.0 / PASO_ZOOM);
    }

    @FXML
    private void restablecerMapa() {
        restablecerZoom();
    }

    private void aplicarZoom(double factor) {
        if (panelMapa.getChildren().isEmpty()) {
            return;
        }
        nivelZoom = Math.max(ZOOM_MINIMO,
                Math.min(ZOOM_MAXIMO, nivelZoom * factor));
        ajustarMapaAlViewport();
    }

    /**
     * Auto-fit: cuando el visor es menor a 590 px el mapa se reduce.
     * nivelZoom representa solo el zoom manual, por eso se conserva al
     * redimensionar la ventana y se mantiene el comportamiento de botones.
     */
    private void ajustarMapaAlViewport() {
        double ancho = contenedorMapa.getWidth();
        double alto = contenedorMapa.getHeight();
        if (ancho <= 0 || alto <= 0) {
            return;
        }
        double ajusteBase = Math.min(1.0, Math.min(
                ancho / ANCHO_MAPA_BASE, alto / ALTO_MAPA_BASE));
        double zoomFinal = ajusteBase * nivelZoom;
        escalaMapa.setX(zoomFinal);
        escalaMapa.setY(zoomFinal);
        limitarArrastreMapa();
    }

    /**
     * Mantiene visible la geometria transformada del plano. Cuando cabe,
     * centra el estadio; con zoom, limita el arrastre a sus bordes.
     */
    private void limitarArrastreMapa() {
        double ancho = contenedorMapa.getWidth();
        double alto = contenedorMapa.getHeight();
        if (ancho <= 0 || alto <= 0) {
            return;
        }

        javafx.geometry.Bounds b = panelMapa.getBoundsInParent();
        double nuevoMinX;
        double nuevoMinY;

        if (b.getWidth() <= ancho) {
            nuevoMinX = (ancho - b.getWidth()) / 2.0;
        } else {
            nuevoMinX = Math.max(ancho - b.getWidth(),
                    Math.min(0, b.getMinX()));
        }
        if (b.getHeight() <= alto) {
            nuevoMinY = (alto - b.getHeight()) / 2.0;
        } else {
            nuevoMinY = Math.max(alto - b.getHeight(),
                    Math.min(0, b.getMinY()));
        }

        panelMapa.setTranslateX(panelMapa.getTranslateX()
                + nuevoMinX - b.getMinX());
        panelMapa.setTranslateY(panelMapa.getTranslateY()
                + nuevoMinY - b.getMinY());
    }

    private void restablecerZoom() {
        arrastrandoMapa = false;
        nivelZoom = 1.0;
        panelMapa.setTranslateX(0);
        panelMapa.setTranslateY(0);
        ajustarMapaAlViewport();
    }


    private void confirmarVenta() {

        if (usuario == null || localidadSeleccionada == null) {
            mostrarAlertaVenta(
                    Alert.AlertType.WARNING,
                    "No existe un vendedor o una localidad seleccionada."
            );
            return;
        }

        boolean numerado = "numerado".equalsIgnoreCase(
                localidadSeleccionada.getTipoVenta()
        );

        List<Integer> idsAsientos = new ArrayList<>();
        int cantidad;

        if (numerado) {
            for (Asiento asiento : asientosSeleccionados.values()) {
                idsAsientos.add(asiento.getIdAsiento());
            }
            cantidad = idsAsientos.size();
        } else {
            Integer valor = spnCantidad.getValue();
            cantidad = valor == null ? 0 : valor;
        }

        if (cantidad <= 0) {
            mostrarAlertaVenta(
                    Alert.AlertType.WARNING,
                    "Debe seleccionar al menos un boleto."
            );
            return;
        }

        BigDecimal totalEstimado = localidadSeleccionada.getPrecio()
                .multiply(BigDecimal.valueOf(cantidad));

        Alert confirmacion = new Alert(Alert.AlertType.CONFIRMATION);
        confirmacion.setTitle("Confirmar venta");
        confirmacion.setHeaderText("Resumen de la venta");
        confirmacion.setContentText(
                "Localidad: " + localidadSeleccionada.getNombreLocalidad()
                + "\nCantidad: " + cantidad
                + "\nTipo: " + localidadSeleccionada.getTipoVenta()
                + "\nTotal estimado: " + formatearPrecio(totalEstimado)
                + "\n\n¿Desea registrar esta venta?"
        );

        if (confirmacion.showAndWait()
                .orElse(ButtonType.CANCEL) != ButtonType.OK) {
            return;
        }

        // Solicitar los datos ANTES de guardar la venta.
        DatosComprador comprador = solicitarDatosComprador();
        if (comprador == null) {
            return; // Cancelar aquí NO crea venta ni descuenta inventario.
        }

        // Capturar datos antes de iniciar el trabajo en segundo plano.
        final int idVendedor = usuario.getIdUsuario();
        final int idInventario = localidadSeleccionada.getIdInventario();
        final int cantidadFinal = cantidad;
        final List<Integer> asientosFinales = new ArrayList<>(idsAsientos);

        btnConfirmarVenta.setDisable(true);
        cmbConciertos.setDisable(true);
        btnConsultarConcierto.setDisable(true);
        contenedorMapa.setDisable(true);
        spnCantidad.setDisable(true);

        Task<ResultadoVenta> tarea = new Task<>() {
            @Override
            protected ResultadoVenta call() throws Exception {

                // La transaccion se confirma primero en Neon.
                int idVenta = ventaService.registrarVenta(
                        idVendedor,
                        idInventario,
                        cantidadFinal,
                        asientosFinales,
                        comprador.nombre(),
                        comprador.nit(),
                        comprador.direccion()
                );

                // Un error de Cloudflare NO revierte una venta ya registrada.
                try {
                    PublicacionBoletosService publicacion =
                            new PublicacionBoletosService();
                    List<String> enlaces =
                            publicacion.publicarBoletosVenta(idVenta);

                    return new ResultadoVenta(
                            idVenta,
                            cantidadFinal,
                            true,
                            "Venta registrada correctamente."
                    );

                } catch (Exception errorPublicacion) {
                    System.err.println(
                            "Publicacion pendiente de venta #" + idVenta
                    );
                    errorPublicacion.printStackTrace();

                    return new ResultadoVenta(
                            idVenta,
                            cantidadFinal,
                            false,
                            "Venta registrada en Neon, pero algunos PDF "
                            + "no pudieron publicarse. No repita la venta."
                    );
                }
            }
        };

        tarea.setOnSucceeded(e -> {
            ResultadoVenta resultado = tarea.getValue();
            restaurarInterfazVenta();
            consultarConcierto();
            mostrarVentanaBoletos(resultado);
        });

        tarea.setOnFailed(e -> {
            Throwable error = tarea.getException();
            restaurarInterfazVenta();
            mostrarAlertaVenta(
                    Alert.AlertType.ERROR,
                    "No se pudo registrar la venta."
                    + "\n\n" + (error == null
                            ? "Error desconocido."
                            : error.getMessage())
                    + "\n\nSe actualizará el mapa para consultar "
                    + "la disponibilidad vigente."
            );
            consultarConcierto();
        });

        Thread hilo = new Thread(tarea, "svb-gua-publicar-venta");
        hilo.setDaemon(true);
        hilo.start();
    }

    // Datos para la factura academica; no es una factura FEL.
    private static record DatosComprador(
            String nombre, String nit, String direccion) { }

    private DatosComprador solicitarDatosComprador() {
        Dialog<DatosComprador> dialogo = new Dialog<>();
        dialogo.setTitle("SVB-GUA - Datos de facturacion");
        dialogo.setHeaderText("Datos del comprador (factura de demostracion)");
        dialogo.initOwner(btnConfirmarVenta.getScene().getWindow());

        ButtonType aceptar = new ButtonType(
                "Continuar con la venta", ButtonBar.ButtonData.OK_DONE);
        dialogo.getDialogPane().getButtonTypes().addAll(
                aceptar, ButtonType.CANCEL);

        TextField txtNombre = new TextField("Consumidor Final");
        TextField txtNit = new TextField("CF");
        TextField txtDireccion = new TextField();
        txtNombre.setPromptText("Nombre completo o Consumidor Final");
        txtNit.setPromptText("NIT o CF");
        txtDireccion.setPromptText("Direccion (opcional)");

        txtNombre.setTextFormatter(new TextFormatter<String>(c ->
                c.getControlNewText().length() <= 150 ? c : null));
        txtNit.setTextFormatter(new TextFormatter<String>(c ->
                c.getControlNewText().length() <= 20 ? c : null));
        txtDireccion.setTextFormatter(new TextFormatter<String>(c ->
                c.getControlNewText().length() <= 250 ? c : null));

        GridPane formulario = new GridPane();
        formulario.setHgap(12);
        formulario.setVgap(12);
        formulario.setStyle("-fx-padding: 18;");
        formulario.add(new Label("Nombre:"), 0, 0);
        formulario.add(txtNombre, 1, 0);
        formulario.add(new Label("NIT:"), 0, 1);
        formulario.add(txtNit, 1, 1);
        formulario.add(new Label("Direccion:"), 0, 2);
        formulario.add(txtDireccion, 1, 2);
        Label nota = new Label(
                "Comprobante academico sin validez tributaria.\n"
                + "Para consumidor final puedes dejar CF.");
        nota.setWrapText(true);
        formulario.add(nota, 0, 3, 2, 1);
        dialogo.getDialogPane().setContent(formulario);

        Node botonAceptar = dialogo.getDialogPane().lookupButton(aceptar);
        botonAceptar.addEventFilter(javafx.event.ActionEvent.ACTION, ev -> {
            if (txtNombre.getText().trim().isEmpty()
                    || txtNit.getText().trim().isEmpty()) {
                ev.consume();
                Alert aviso = new Alert(Alert.AlertType.WARNING,
                        "Ingresa el nombre y el NIT, o usa CF para consumidor final.",
                        ButtonType.OK);
                aviso.initOwner(dialogo.getDialogPane().getScene().getWindow());
                aviso.showAndWait();
            }
        });

        dialogo.setResultConverter(opcion -> {
            if (opcion != aceptar) {
                return null;
            }
            return new DatosComprador(
                    txtNombre.getText().trim(),
                    txtNit.getText().trim().toUpperCase(Locale.ROOT),
                    txtDireccion.getText().trim());
        });
        return dialogo.showAndWait().orElse(null);
    }

    private static record ResultadoVenta(
            int idVenta, int cantidad, boolean publicado, String mensaje) { }

    private void mostrarVentanaBoletos(ResultadoVenta resultado) {
        try {
            List<Boleto> boletos = new BoletoDAO().listarPorVenta(resultado.idVenta());

            Dialog<Void> ventana = new Dialog<>();
            ventana.setTitle("SVB-GUA - Boletos de la venta");
            ventana.setHeaderText("Venta #" + resultado.idVenta()
                    + " - " + resultado.cantidad() + " boleto(s)");
            ventana.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
            ventana.initOwner(btnConfirmarVenta.getScene().getWindow());

            VBox contenido = new VBox(12);
            contenido.setStyle("-fx-padding: 15;");
            Label estado = new Label(resultado.mensaje());
            estado.setWrapText(true);
            contenido.getChildren().add(estado);

            int publicados = 0;
            for (Boleto boleto : boletos) {
                boolean disponible = boleto.getRutaPdf() != null
                        && !boleto.getRutaPdf().isBlank();
                if (disponible) publicados++;

                Label titulo = new Label("Boleto #" + boleto.getIdBoleto()
                        + (disponible ? " - PDF listo" : " - Pendiente"));
                titulo.setMinWidth(180);
                Button descargar = new Button("Descargar PDF");
                descargar.setDisable(!disponible);
                descargar.setOnAction(ev -> descargarBoleto(boleto));
                HBox fila = new HBox(12, titulo, descargar);
                fila.setAlignment(Pos.CENTER_LEFT);
                contenido.getChildren().add(fila);
            }

            if (publicados > 1) {
                Button descargarTodos = new Button("Descargar todos");
                descargarTodos.setOnAction(ev -> descargarTodos(boletos));
                contenido.getChildren().add(descargarTodos);
            }

            // Una factura por venta, independientemente de cuántos boletos tenga.
            Button descargarFactura = new Button("Descargar factura PDF");
            descargarFactura.setStyle(
                    "-fx-background-color: #0D9488;"
                    + "-fx-text-fill: white;"
                    + "-fx-font-weight: bold;"
                    + "-fx-padding: 10 18;"
                    + "-fx-background-radius: 7;"
            );
            descargarFactura.setMaxWidth(Double.MAX_VALUE);
            descargarFactura.setOnAction(ev -> descargarFactura(resultado.idVenta()));
            contenido.getChildren().add(new javafx.scene.control.Separator());
            contenido.getChildren().add(new Label("Factura ficticia de la compra"));
            contenido.getChildren().add(descargarFactura);

            if (publicados == 0) {
                Label aviso = new Label("No hay PDF publicados aún. "
                        + "La venta permanece registrada en Neon.");
                aviso.setWrapText(true);
                contenido.getChildren().add(aviso);
            }

            ScrollPane scroll = new ScrollPane(contenido);
            scroll.setFitToWidth(true);
            scroll.setPrefViewportWidth(490);
            scroll.setPrefViewportHeight(Math.min(380, 110 + boletos.size() * 54));
            ventana.getDialogPane().setContent(scroll);
            ventana.showAndWait();

        } catch (SQLException ex) {
            mostrarAlertaVenta(Alert.AlertType.WARNING,
                    "Venta #" + resultado.idVenta() + " registrada. "
                    + "No se pudo cargar la ventana de descarga: " + ex.getMessage());
        }
    }

    private void descargarFactura(int idVenta) {
        FileChooser selector = new FileChooser();
        selector.setTitle("Guardar factura - Venta #" + idVenta);
        selector.setInitialFileName("factura-venta-" + idVenta + ".pdf");
        selector.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Archivos PDF", "*.pdf"));

        File destino = selector.showSaveDialog(
                btnConfirmarVenta.getScene().getWindow());
        if (destino == null) {
            return;
        }

        try {
            new FacturaPDFService().generarFactura(idVenta, destino.toPath());
            mostrarAlertaVenta(Alert.AlertType.INFORMATION,
                    "Factura guardada en:\n" + destino.getAbsolutePath());
        } catch (Exception ex) {
            mostrarAlertaVenta(Alert.AlertType.ERROR,
                    "No se pudo generar la factura de la venta #"
                    + idVenta + ":\n" + ex.getMessage());
        }
    }

    private void descargarBoleto(Boleto boleto) {
        FileChooser selector = new FileChooser();
        selector.setTitle("Guardar boleto #" + boleto.getIdBoleto());
        selector.setInitialFileName("boleto-" + boleto.getIdBoleto() + ".pdf");
        selector.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Archivos PDF", "*.pdf"));
        File destino = selector.showSaveDialog(btnConfirmarVenta.getScene().getWindow());
        if (destino == null) return;

        try {
            guardarPDF(boleto, destino.toPath());
            mostrarAlertaVenta(Alert.AlertType.INFORMATION,
                    "Boleto descargado en:\n" + destino.getAbsolutePath());
        } catch (Exception ex) {
            mostrarAlertaVenta(Alert.AlertType.ERROR,
                    "No se pudo descargar el boleto #" + boleto.getIdBoleto()
                    + ":\n" + ex.getMessage());
        }
    }

    private void descargarTodos(List<Boleto> boletos) {
        DirectoryChooser selector = new DirectoryChooser();
        selector.setTitle("Elegir carpeta para los boletos");
        File carpeta = selector.showDialog(btnConfirmarVenta.getScene().getWindow());
        if (carpeta == null) return;

        int guardados = 0;
        StringBuilder errores = new StringBuilder();
        for (Boleto boleto : boletos) {
            if (boleto.getRutaPdf() == null || boleto.getRutaPdf().isBlank()) continue;
            try {
                guardarPDF(boleto, carpeta.toPath().resolve(
                        "boleto-" + boleto.getIdBoleto() + ".pdf"));
                guardados++;
            } catch (Exception ex) {
                errores.append("Boleto #").append(boleto.getIdBoleto())
                        .append(": ").append(ex.getMessage()).append("\n");
            }
        }
        mostrarAlertaVenta(errores.isEmpty()
                        ? Alert.AlertType.INFORMATION : Alert.AlertType.WARNING,
                guardados + " PDF guardados en:\n" + carpeta.getAbsolutePath()
                        + (errores.isEmpty() ? "" : "\n\nErrores:\n" + errores));
    }

    private void guardarPDF(Boleto boleto, Path destino) throws Exception {
        if (boleto.getRutaPdf() == null || boleto.getRutaPdf().isBlank()) {
            throw new IOException("El PDF aún no está publicado.");
        }
        // La URL guardada en Neon es la referencia definitiva, no un PDF regenerado.
        URI url = URI.create(boleto.getRutaPdf());
        if (!"https".equalsIgnoreCase(url.getScheme())
                || !"svb-gua-boletos.jchaym4.workers.dev".equalsIgnoreCase(url.getHost())
                || !url.getPath().matches("/b/[a-f0-9]{32}")) {
            throw new IOException("La URL del PDF no corresponde al servidor SVB-GUA.");
        }
        HttpRequest solicitud = HttpRequest.newBuilder(url)
                .timeout(Duration.ofSeconds(30)).GET().build();
        try (HttpClient cliente = HttpClient.newHttpClient()) {
            HttpResponse<byte[]> respuesta = cliente.send(
                    solicitud, HttpResponse.BodyHandlers.ofByteArray());
            byte[] datos = respuesta.body();
            if (respuesta.statusCode() != 200 || datos.length < 5
                    || datos[0] != '%' || datos[1] != 'P' || datos[2] != 'D'
                    || datos[3] != 'F' || datos[4] != '-') {
                throw new IOException("Cloudflare no devolvió un PDF válido (HTTP "
                        + respuesta.statusCode() + ").");
            }
            Files.write(destino, datos);
        }
    }

    private void restaurarInterfazVenta() {
        cmbConciertos.setDisable(false);
        btnConsultarConcierto.setDisable(cmbConciertos.getValue() == null);
        contenedorMapa.setDisable(false);
        // consultarConcierto reconstruye la seleccion y el spinner.
    }

    private void mostrarAlertaVenta(
            Alert.AlertType tipo,
            String mensaje) {

        Alert alerta = new Alert(tipo);

        alerta.setTitle("SVB-GUA - Nueva Venta");
        alerta.setHeaderText(null);
        alerta.setContentText(mensaje);
        alerta.showAndWait();
    }


}
