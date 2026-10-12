
package controladorFx;

import dao.DashboardVendedorDAO;
import modelo.EstadisticasVendedor;
import modelo.Usuario;
import modelo.VentaReciente;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.beans.property.SimpleStringProperty;

public class DashboardVendedorFXController {

    // TARJETAS DE ESTADÍSTICAS

    @FXML
    private Label lblVentasHoy;

    @FXML
    private Label lblBoletosHoy;

    @FXML
    private Label lblTotalRecaudado;

    @FXML
    private Label lblMisVentas;

    @FXML
    private Label lblEstadoDashboard;

    @FXML
    private Button btnActualizar;

    // TABLA DE VENTAS RECIENTES

    @FXML
    private TableView<VentaReciente> tablaVentasRecientes;

    @FXML
    private TableColumn<VentaReciente, String> colIdVenta;

    @FXML
    private TableColumn<VentaReciente, String> colFecha;

    @FXML
    private TableColumn<VentaReciente, String> colVendedor;

    @FXML
    private TableColumn<VentaReciente, String> colTotal;

    // DATOS Y CONEXIÓN

    private Usuario usuario;

    private final DashboardVendedorDAO dashboardDAO =
            new DashboardVendedorDAO();

    private final DateTimeFormatter formatoFecha =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    // RESULTADO DE LAS DOS CONSULTAS

    private static class ResultadoDashboard {

        EstadisticasVendedor estadisticas;
        List<VentaReciente> ventas;

        ResultadoDashboard(
                EstadisticasVendedor estadisticas,
                List<VentaReciente> ventas) {

            this.estadisticas = estadisticas;
            this.ventas = ventas;
        }
    }

    // INICIALIZAR PANTALLA

    @FXML
    private void initialize() {

        configurarTabla();

        btnActualizar.setDisable(true);

        btnActualizar.setOnAction(
                event -> cargarDashboard()
        );

        lblEstadoDashboard.setText(
                "Esperando vendedor autenticado..."
        );
    }

    // CONFIGURAR COLUMNAS DE LA TABLA

    private void configurarTabla() {

        colIdVenta.setCellValueFactory(
                dato -> new SimpleStringProperty(
                        String.valueOf(
                                dato.getValue().getIdVenta()
                        )
                )
        );

        colFecha.setCellValueFactory(dato -> {

            VentaReciente venta = dato.getValue();

            String fecha = venta.getFechaVenta() == null
                    ? "Sin fecha"
                    : venta.getFechaVenta().format(formatoFecha);

            return new SimpleStringProperty(fecha);
        });

        colVendedor.setCellValueFactory(
                dato -> new SimpleStringProperty(
                        dato.getValue().getNombreVendedor()
                )
        );

        colTotal.setCellValueFactory(dato -> {

            String total = String.format(
                    Locale.US,
                    "Q %,.2f",
                    dato.getValue().getTotalVenta()
            );

            return new SimpleStringProperty(total);
        });

        tablaVentasRecientes.setPlaceholder(
                new Label(
                        "No hay ventas recientes para este vendedor."
                )
        );
    }

    // RECIBIR VENDEDOR AUTENTICADO

    public void setUsuario(Usuario usuario) {

        if (usuario == null
                || usuario.getIdUsuario() <= 0) {

            throw new IllegalArgumentException(
                    "Se necesita un vendedor válido."
            );
        }

        this.usuario = usuario;

        cargarDashboard();
    }

    // CONSULTAR POSTGRESQL

    private void cargarDashboard() {

        if (usuario == null) {
            lblEstadoDashboard.setText(
                    "No hay vendedor autenticado."
            );
            return;
        }

        final int idVendedor = usuario.getIdUsuario();

        btnActualizar.setDisable(true);

        lblEstadoDashboard.setText(
                "Cargando estadísticas y ventas recientes..."
        );

        Task<ResultadoDashboard> tarea = new Task<>() {

            @Override
            protected ResultadoDashboard call()
                    throws Exception {

                EstadisticasVendedor estadisticas =
                        dashboardDAO.obtenerEstadisticas(
                                idVendedor
                        );

                List<VentaReciente> ventas =
                        dashboardDAO.obtenerVentasRecientes(
                                idVendedor
                        );

                return new ResultadoDashboard(
                        estadisticas,
                        ventas
                );
            }
        };

        // CUANDO LA CONSULTA FUNCIONA

        tarea.setOnSucceeded(event -> {

            ResultadoDashboard resultado =
                    tarea.getValue();

            EstadisticasVendedor datos =
                    resultado.estadisticas;

            lblVentasHoy.setText(
                    String.valueOf(datos.getVentasHoy())
            );

            lblBoletosHoy.setText(
                    String.valueOf(
                            datos.getBoletosVendidosHoy()
                    )
            );

            lblTotalRecaudado.setText(
                    String.format(
                            Locale.US,
                            "Q %,.2f",
                            datos.getTotalRecaudadoHoy()
                    )
            );

            lblMisVentas.setText(
                    String.valueOf(datos.getMisVentasHoy())
            );

            // ACTUALIZAR TABLA

            tablaVentasRecientes.getItems().setAll(
                    resultado.ventas
            );

            lblEstadoDashboard.setText(
                    "Dashboard actualizado correctamente."
            );

            btnActualizar.setDisable(false);
        });

        // CUANDO EXISTE UN ERROR

        tarea.setOnFailed(event -> {

            lblVentasHoy.setText("—");
            lblBoletosHoy.setText("—");
            lblTotalRecaudado.setText("—");
            lblMisVentas.setText("—");

            tablaVentasRecientes.getItems().clear();

            lblEstadoDashboard.setText(
                    "Error al consultar PostgreSQL."
            );

            tarea.getException().printStackTrace();

            btnActualizar.setDisable(false);
        });

        Thread hilo = new Thread(
                tarea,
                "dashboard-vendedor"
        );

        hilo.setDaemon(true);
        hilo.start();
    }
}
