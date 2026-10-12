
package controladorFx;

import dao.ConciertoDAO;
import dao.MapaLocalidadDAO;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
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
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Ellipse;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.util.StringConverter;
import modelo.Concierto;
import modelo.MapaLocalidad;
import modelo.Usuario;

public class NuevaVentaFXController {

    @FXML
    private ComboBox<Concierto> cmbConciertos;

    @FXML
    private Button btnConsultarConcierto;

    @FXML
    private StackPane contenedorMapa;

    @FXML
    private AnchorPane panelMapa;

    @FXML
    private Label lblLocalidadSeleccionada;

    @FXML
    private Label lblPrecioUnitario;

    @FXML
    private Label lblDisponibles;

    @FXML
    private Spinner<Integer> spnCantidad;

    @FXML
    private Label lblAsientosSeleccionados;

    @FXML
    private Label lblTotal;

    @FXML
    private Button btnConfirmarVenta;

    private Usuario usuario;

    private MapaLocalidad localidadSeleccionada;

    private Group grupoSeleccionado;

    private final ConciertoDAO conciertoDAO =
            new ConciertoDAO();

    private final MapaLocalidadDAO mapaDAO =
            new MapaLocalidadDAO();

    private final DateTimeFormatter formatoFecha =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @FXML
    private void initialize() {

        configurarSpinner();
        configurarComboBox();
        limpiarResumen();

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
                (obs, anterior, nuevo) ->
                        actualizarTotal()
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
                                + " - "
                                + fecha
                                + " - "
                                + concierto.getRecinto();
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

        for (Concierto concierto :
                conciertoDAO.listarTodos()) {

            if ("programado".equals(concierto.getEstado())
                    || "activo".equals(concierto.getEstado())) {

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
                        "Este concierto no tiene localidades "
                        + "configuradas en el mapa."
                );
                return;
            }

            dibujarMapa(localidades);

        } catch (SQLException e) {

            e.printStackTrace();

            mostrarMensajeMapa(
                    "No se pudo cargar el mapa."
            );

            Alert alerta = new Alert(
                    Alert.AlertType.ERROR
            );

            alerta.setTitle("Error");
            alerta.setHeaderText(
                    "Error al consultar PostgreSQL"
            );

            alerta.setContentText(e.getMessage());
            alerta.showAndWait();
        }
    }

    private void dibujarMapa(
            List<MapaLocalidad> localidades) {

        panelMapa.getChildren().clear();
        panelMapa.setPrefSize(590, 650);

        Ellipse estadio = new Ellipse(
                295, 290, 280, 222
        );

        estadio.setFill(Color.web("#E2E8F0"));
        estadio.setStroke(Color.web("#CBD5E1"));
        estadio.setStrokeWidth(4);

        Ellipse interior = new Ellipse(
                295, 290, 112, 76
        );

        interior.setFill(Color.web("#F8FAFC"));
        interior.setStroke(Color.web("#CBD5E1"));
        interior.setStrokeWidth(2);

        panelMapa.getChildren().add(estadio);

        MapaLocalidad vip = buscarLocalidad(
                localidades, "VIP"
        );

        MapaLocalidad oro = buscarLocalidad(
                localidades, "ORO"
        );

        MapaLocalidad estandar = buscarLocalidad(
                localidades, "ESTANDAR"
        );

        HBox leyenda = new HBox(12);
        leyenda.setAlignment(Pos.CENTER_LEFT);
        leyenda.setLayoutX(20);
        leyenda.setLayoutY(540);

        if (estandar != null) {
            agregarZona(
                    estandar,
                    4,
                    220,
                    169,
                    13,
                    11,
                    leyenda
            );
        }

        if (oro != null) {
            agregarZona(
                    oro,
                    4,
                    163,
                    121,
                    12,
                    11,
                    leyenda
            );
        }

        if (vip != null) {
            agregarZona(
                    vip,
                    3,
                    118,
                    87,
                    13,
                    10,
                    leyenda
            );
        }

        panelMapa.getChildren().add(interior);

        Rectangle escenario = new Rectangle(
                234, 258, 122, 64
        );

        escenario.setArcWidth(16);
        escenario.setArcHeight(16);
        escenario.setFill(Color.web("#334155"));

        Label lblEscenario = new Label("ESCENARIO");

        lblEscenario.setFont(
                Font.font(
                        "System",
                        FontWeight.BOLD,
                        15
                )
        );

        lblEscenario.setTextFill(Color.WHITE);
        lblEscenario.setAlignment(Pos.CENTER);
        lblEscenario.setLayoutX(234);
        lblEscenario.setLayoutY(278);
        lblEscenario.setPrefWidth(122);

        panelMapa.getChildren().addAll(
                escenario,
                lblEscenario
        );

        panelMapa.getChildren().add(leyenda);

        Label nota = new Label(
                "Distribución ilustrativa de zonas de aforo."
        );

        nota.setTextFill(Color.web("#64748B"));
        nota.setFont(Font.font("System", 12));
        nota.setLayoutX(20);
        nota.setLayoutY(605);

        panelMapa.getChildren().add(nota);
    }

    private MapaLocalidad buscarLocalidad(
            List<MapaLocalidad> localidades,
            String nombre) {

        for (MapaLocalidad localidad : localidades) {

            if (localidad.getNombreLocalidad() != null
                    && localidad.getNombreLocalidad()
                            .equalsIgnoreCase(nombre)) {

                return localidad;
            }
        }

        return null;
    }

    private void agregarZona(
            MapaLocalidad localidad,
            int filas,
            double radioXInicial,
            double radioYInicial,
            double pasoX,
            double pasoY,
            HBox leyenda) {

        Group grupo = new Group();

        String codigoColor = colorSeguro(
                localidad.getColor()
        );

        Color color = Color.web(codigoColor);

        boolean agotado =
                localidad.getCantidadDisponible() <= 0;

        if (agotado) {
            color = Color.web("#94A3B8");
        }

        for (int fila = 0; fila < filas; fila++) {

            double radioX =
                    radioXInicial + fila * pasoX;

            double radioY =
                    radioYInicial + fila * pasoY;

            for (int angulo = 0;
                    angulo < 360;
                    angulo += 5) {

                if (angulo % 45 == 0) {
                    continue;
                }

                double radianes =
                        Math.toRadians(angulo);

                double x = 295
                        + radioX * Math.cos(radianes);

                double y = 290
                        + radioY * Math.sin(radianes);

                Rectangle casilla = new Rectangle(
                        8, 10
                );

                casilla.setArcWidth(2);
                casilla.setArcHeight(2);

                casilla.setLayoutX(x - 4);
                casilla.setLayoutY(y - 5);

                casilla.setRotate(angulo + 90);

                casilla.setFill(color);

                casilla.setStroke(Color.WHITE);
                casilla.setStrokeWidth(0.7);

                grupo.getChildren().add(casilla);
            }
        }

        grupo.setOpacity(
                agotado ? 0.35 : 0.85
        );

        if (!agotado) {

            grupo.setCursor(Cursor.HAND);

            grupo.setOnMouseClicked(
                    e -> seleccionarLocalidad(
                            localidad,
                            grupo
                    )
            );
        }

        panelMapa.getChildren().add(grupo);

        Rectangle muestraColor = new Rectangle(
                17, 17
        );

        muestraColor.setArcWidth(4);
        muestraColor.setArcHeight(4);
        muestraColor.setFill(color);

        Label nombre = new Label(
                localidad.getNombreLocalidad()
        );

        nombre.setFont(
                Font.font(
                        "System",
                        FontWeight.BOLD,
                        12
                )
        );

        nombre.setTextFill(
                Color.web("#334155")
        );

        HBox elementoLeyenda = new HBox(
                6,
                muestraColor,
                nombre
        );

        elementoLeyenda.setAlignment(
                Pos.CENTER_LEFT
        );

        elementoLeyenda.setStyle(
                "-fx-background-color: white;"
                + "-fx-border-color: #E2E8F0;"
                + "-fx-border-radius: 7px;"
                + "-fx-background-radius: 7px;"
                + "-fx-padding: 8px;"
        );

        if (!agotado) {

            elementoLeyenda.setCursor(Cursor.HAND);

            elementoLeyenda.setOnMouseClicked(
                    e -> seleccionarLocalidad(
                            localidad,
                            grupo
                    )
            );

        } else {
            nombre.setText(
                    localidad.getNombreLocalidad()
                    + " (agotado)"
            );
        }

        leyenda.getChildren().add(
                elementoLeyenda
        );
    }

    private void seleccionarLocalidad(
            MapaLocalidad localidad,
            Group grupo) {

        if (localidad.getCantidadDisponible() <= 0) {
            return;
        }

        if (grupoSeleccionado != null) {
            grupoSeleccionado.setEffect(null);
            grupoSeleccionado.setOpacity(0.85);
        }

        localidadSeleccionada = localidad;
        grupoSeleccionado = grupo;

        grupoSeleccionado.setOpacity(1.0);

        grupoSeleccionado.setEffect(
                new DropShadow(
                        12,
                        Color.web("#1E40AF")
                )
        );

        lblLocalidadSeleccionada.setText(
                localidad.getNombreLocalidad()
        );

        lblPrecioUnitario.setText(
                formatearPrecio(localidad.getPrecio())
        );

        lblDisponibles.setText(
                localidad.getCantidadDisponible()
                + " boletos"
        );

        if ("aforo".equals(localidad.getTipoVenta())) {

            spnCantidad.setValueFactory(
                    new SpinnerValueFactory.IntegerSpinnerValueFactory(
                            1,
                            localidad.getCantidadDisponible(),
                            1
                    )
            );

            spnCantidad.setDisable(false);

            lblAsientosSeleccionados.setText(
                    "No aplica (aforo)"
            );

        } else {

            configurarSpinner();

            lblAsientosSeleccionados.setText(
                    "Pendiente: selección de asientos"
            );
        }

        actualizarTotal();
    }

    private void actualizarTotal() {

        if (localidadSeleccionada == null
                || !"aforo".equals(
                        localidadSeleccionada.getTipoVenta()
                )) {

            lblTotal.setText("Q 0.00");
            return;
        }

        Integer cantidad = spnCantidad.getValue();

        if (cantidad == null || cantidad <= 0) {
            lblTotal.setText("Q 0.00");
            return;
        }

        BigDecimal total =
                localidadSeleccionada.getPrecio()
                        .multiply(
                                BigDecimal.valueOf(cantidad)
                        );

        lblTotal.setText(
                formatearPrecio(total)
        );
    }

    private String formatearPrecio(
            BigDecimal precio) {

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

    private void mostrarMensajeMapa(
            String texto) {

        panelMapa.getChildren().clear();

        Label mensaje = new Label(texto);

        mensaje.setWrapText(true);
        mensaje.setLayoutX(20);
        mensaje.setLayoutY(20);
        mensaje.setMaxWidth(450);

        panelMapa.getChildren().add(mensaje);
    }

    private void limpiarResumen() {

        localidadSeleccionada = null;
        grupoSeleccionado = null;

        lblLocalidadSeleccionada.setText(
                "Sin seleccionar"
        );

        lblPrecioUnitario.setText(
                "Q 0.00"
        );

        lblDisponibles.setText(
                "0 boletos"
        );

        lblAsientosSeleccionados.setText(
                "Ninguno"
        );

        lblTotal.setText(
                "Q 0.00"
        );

        configurarSpinner();

        panelMapa.getChildren().clear();

        btnConfirmarVenta.setDisable(true);
    }
}
