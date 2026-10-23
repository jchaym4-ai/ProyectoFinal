package controladorFx;

import dao.EventosPublicosDAO.Asiento;
import dao.EventosPublicosDAO.Localidad;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.BiConsumer;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Group;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Ellipse;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

/**
 * Versión de SOLO LECTURA del plano radial empleado en Nueva Venta.
 * No hace INSERT/UPDATE, no reserva asientos y no referencia VentaService.
 * Se dibuja sobre un AnchorPane de 590x650 para mantener las coordenadas
 * utilizadas por el mapa del Vendedor.
 */
public final class MapaEventosPublicos {

    private static final double CX = 295;
    private static final double CY = 285;
    private static final String[] COLORES = {
        "#0D9488", "#F59E0B", "#7C3AED", "#DC2626", "#0284C7",
        "#65A30D", "#DB2777", "#A16207", "#4338CA", "#15803D"
    };

    private MapaEventosPublicos() { }

    public static void dibujar(AnchorPane panel,
            List<Localidad> localidades,
            Map<Integer, List<Asiento>> asientosPorInventario,
            BiConsumer<Localidad, Asiento> onConsulta) {

        panel.getChildren().clear();
        panel.setPrefSize(590, 650);
        panel.setMinSize(590, 650);
        panel.setMaxSize(590, 650);

        List<Localidad> ordenadas = new ArrayList<>(localidades);
        ordenadas.sort(Comparator
                .comparing((Localidad l) -> l.precio() == null
                        ? BigDecimal.ZERO : l.precio())
                .reversed()
                .thenComparingInt(Localidad::idInventario));

        List<Integer> anillos = new ArrayList<>();
        int total = 0;
        for (Localidad l : ordenadas) {
            List<Asiento> asientos = asientosPorInventario
                    .getOrDefault(l.idInventario(), List.of());
            int cantidad = "numerado".equalsIgnoreCase(l.tipoVenta())
                    ? Math.max(1, (int) Math.ceil(asientos.size() / 65.0)) : 2;
            anillos.add(cantidad);
            total += cantidad;
        }

        Ellipse campo = new Ellipse(CX, CY, 280, 222);
        campo.setFill(Color.web("#24283B"));
        campo.setStroke(Color.web("#5B5578"));
        campo.setStrokeWidth(3);
        panel.getChildren().add(campo);

        FlowPane leyenda = new FlowPane(8, 8);
        leyenda.setPrefWrapLength(550);
        leyenda.setLayoutX(20);
        leyenda.setLayoutY(530);

        int anilloActual = 0;
        for (int i = 0; i < ordenadas.size(); i++) {
            Localidad l = ordenadas.get(i);
            Color color = Color.web(COLORES[i % COLORES.length]);
            dibujarLocalidad(panel, l,
                    asientosPorInventario.getOrDefault(l.idInventario(), List.of()),
                    anilloActual, anillos.get(i), Math.max(total, 1), color,
                    leyenda, onConsulta);
            anilloActual += anillos.get(i);
        }

        Ellipse interior = new Ellipse(CX, CY, 110, 78);
        interior.setFill(Color.web("#171822"));
        interior.setStroke(Color.web("#7B738F"));
        interior.setStrokeWidth(2);

        Rectangle escenario = new Rectangle(234, 253, 122, 64);
        escenario.setFill(Color.web("#423A67"));
        escenario.setArcHeight(16);
        escenario.setArcWidth(16);
        escenario.setStroke(Color.web("#9D88E9"));

        Label textoEscenario = new Label("ESCENARIO");
        textoEscenario.setTextFill(Color.WHITE);
        textoEscenario.setFont(Font.font("System", FontWeight.BOLD, 15));
        textoEscenario.setLayoutX(234);
        textoEscenario.setLayoutY(273);
        textoEscenario.setAlignment(Pos.CENTER);
        textoEscenario.setPrefWidth(122);
        // Los elementos interiores no deben interceptar clics de localidades.
        interior.setMouseTransparent(true);
        escenario.setMouseTransparent(true);
        textoEscenario.setMouseTransparent(true);

        panel.getChildren().addAll(interior, escenario, textoEscenario, leyenda);

        Label ayuda = new Label("Haz clic en una zona o asiento. Rueda: zoom · Arrastra: mover.");
        ayuda.setLayoutX(20);
        ayuda.setLayoutY(618);
        ayuda.setTextFill(Color.web("#C5BEDA"));
        ayuda.setFont(Font.font("System", 12));
        panel.getChildren().add(ayuda);
    }

    private static void dibujarLocalidad(AnchorPane panel,
            Localidad localidad, List<Asiento> asientos,
            int primerAnillo, int cantidadAnillos, int totalAnillos,
            Color color, FlowPane leyenda,
            BiConsumer<Localidad, Asiento> alConsultar) {

        boolean numerado = "numerado".equalsIgnoreCase(localidad.tipoVenta());
        Group grupoAforo = new Group();
        int indice = 0;

        for (int anillo = 0; anillo < cantidadAnillos; anillo++) {
            double proporcion = (primerAnillo + anillo + 0.5) / totalAnillos;
            double radioX = 119 + proporcion * 147;
            double radioY = 87 + proporcion * 124;
            int casillas;
            if (numerado) {
                int restantes = asientos.size() - indice;
                int restantesAnillos = cantidadAnillos - anillo;
                casillas = (int) Math.ceil((double) restantes / restantesAnillos);
            } else {
                casillas = 60;
            }

            for (int posicion = 0; posicion < casillas; posicion++) {
                double angulo = 2 * Math.PI * posicion / Math.max(casillas, 1);
                double x = CX + radioX * Math.cos(angulo);
                double y = CY + radioY * Math.sin(angulo);
                Rectangle casilla = new Rectangle(7, 9);
                casilla.setX(x - 3.5);
                casilla.setY(y - 4.5);
                casilla.setRotate(Math.toDegrees(angulo) + 90);
                casilla.setArcWidth(2);
                casilla.setArcHeight(2);
                casilla.setStroke(Color.web("#151522"));
                casilla.setStrokeWidth(0.5);

                if (numerado) {
                    Asiento asiento = asientos.get(indice++);
                    String estado = asiento.estado() == null ? "bloqueado"
                            : asiento.estado().toLowerCase(Locale.ROOT).trim();
                    Color relleno = switch (estado) {
                        case "disponible" -> color;
                        case "vendido" -> Color.web("#768098");
                        case "reservado" -> Color.web("#D99B38");
                        default -> Color.web("#CF5365");
                    };
                    casilla.setFill(relleno);
                    casilla.setCursor(Cursor.HAND);
                    Tooltip.install(casilla, new Tooltip(localidad.nombre()
                            + "\nFila " + asiento.fila()
                            + " · Asiento " + asiento.numero()
                            + "\nEstado: " + estado));
                    casilla.setOnMouseClicked(e -> {
                        if (e.isStillSincePress()) {
                            alConsultar.accept(localidad, asiento);
                        }
                        e.consume();
                    });
                    panel.getChildren().add(casilla);
                } else {
                    boolean quedan = localidad.disponibles() > 0;
                    casilla.setFill(quedan ? color : Color.web("#768098"));
                    grupoAforo.getChildren().add(casilla);
                }
            }
        }

        if (!numerado) {
            grupoAforo.setCursor(Cursor.HAND);
            Tooltip.install(grupoAforo, new Tooltip(localidad.nombre()
                    + " · Admisión general\n" + localidad.disponibles()
                    + " disponibles de " + localidad.capacidad()));
            grupoAforo.setOnMouseClicked(e -> {
                if (e.isStillSincePress()) alConsultar.accept(localidad, null);
                e.consume();
            });
            panel.getChildren().add(grupoAforo);
        }

        Rectangle muestra = new Rectangle(13, 13, color);
        muestra.setArcWidth(3);
        muestra.setArcHeight(3);
        Label nombre = new Label(localidad.nombre());
        nombre.setTextFill(Color.web("#EBE6FF"));
        nombre.setFont(Font.font("System", FontWeight.BOLD, 11));
        HBox chip = new HBox(6, muestra, nombre);
        chip.setAlignment(Pos.CENTER_LEFT);
        chip.setStyle("-fx-background-color: #232035; -fx-border-color: #41375D;"
                + " -fx-background-radius: 8; -fx-border-radius: 8; -fx-padding: 7;");
        chip.setCursor(Cursor.HAND);
        chip.setOnMouseClicked(e -> alConsultar.accept(localidad, null));
        leyenda.getChildren().add(chip);
    }
}
