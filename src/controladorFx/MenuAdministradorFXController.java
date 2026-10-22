package controladorFx;

import dao.ArtistaDAO;
import dao.ConciertoDAO;
import dao.InventarioBoletoDAO;
import dao.LocalidadDAO;
import java.io.IOException;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import modelo.Usuario;

public class MenuAdministradorFXController {

    @FXML private Label lblNombreUsuario;
    @FXML private Label lblTotalArtistas;
    @FXML private Label lblTotalConciertos;
    @FXML private Label lblTotalLocalidades;
    @FXML private Label lblTotalInventario;

    @FXML private Button btnDashboard;
    @FXML private Button btnArtistas;
    @FXML private Button btnLocalidades;
    @FXML private Button btnConciertos;
    @FXML private Button btnInventario;
    @FXML private Button btnUsuarios;
    @FXML private Button btnControlAcceso;
    @FXML private Button btnAnulaciones;
    @FXML private Button btnReportes;
    @FXML private Button btnCerrarSesion;

    private Usuario usuario;
    private Node dashboardOriginal;
    private ControlAccesoFXController controladorAcceso;

    @FXML
    private void initialize() {
        cargarContadoresDashboard();
        btnLocalidades.setOnAction(e -> abrirLocalidades());
        btnDashboard.setOnAction(e -> abrirDashboard());
        btnArtistas.setOnAction(e -> abrirArtistas());
        btnUsuarios.setOnAction(e -> abrirUsuarios());
        btnConciertos.setOnAction(e -> abrirConciertos());
        btnInventario.setOnAction(e -> abrirInventario());
        btnControlAcceso.setOnAction(e -> abrirControlAcceso());
        btnAnulaciones.setOnAction(e -> abrirAnulaciones());
        btnReportes.setOnAction(e -> abrirReportes());
    }

    public void setUsuario(Usuario usuario) {
        this.usuario = usuario;
        if (usuario != null) {
            lblNombreUsuario.setText(usuario.getNombreCompleto());
        }
    }

    public Usuario getUsuario() {
        return usuario;
    }

    private BorderPane obtenerRoot(Button boton) {
        return (BorderPane) boton.getScene().getRoot();
    }

    private void guardarDashboardSiEsNecesario(BorderPane root) {
        if (dashboardOriginal == null) {
            dashboardOriginal = root.getCenter();
        }
    }

    private void detenerCamaraAnterior() {
        if (controladorAcceso != null) {
            controladorAcceso.cerrar();
            controladorAcceso = null;
        }
    }

    public void abrirInventario() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/vistas/fx/InventarioView.fxml"));
            Parent vista = loader.load();
            BorderPane root = obtenerRoot(btnInventario);
            guardarDashboardSiEsNecesario(root);
            ScrollPane scroll = new ScrollPane(vista);
            scroll.setFitToWidth(true);
            scroll.setFitToHeight(false);
            scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
            scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
            scroll.setPannable(false);
            detenerCamaraAnterior();
            root.setCenter(scroll);
            marcarBotonActivo(btnInventario);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void abrirUsuarios() {
        abrirVistaConScroll(btnUsuarios, "/vistas/fx/UsuariosView.fxml");
    }

    private void abrirConciertos() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/vistas/fx/ConciertosView.fxml"));
            Parent vista = loader.load();
            BorderPane root = obtenerRoot(btnConciertos);
            guardarDashboardSiEsNecesario(root);
            detenerCamaraAnterior();
            root.setCenter(vista);
            marcarBotonActivo(btnConciertos);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void abrirLocalidades() {
        abrirVistaConScroll(btnLocalidades, "/vistas/fx/LocalidadesView.fxml");
    }

    private void abrirArtistas() {
        abrirVistaConScroll(btnArtistas, "/vistas/fx/ArtistasView.fxml");
    }

    private void abrirControlAcceso() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/vistas/fx/ControlAccesoView.fxml"));
            Parent vista = loader.load();
            ControlAccesoFXController nuevo = loader.getController();
            BorderPane root = obtenerRoot(btnControlAcceso);
            guardarDashboardSiEsNecesario(root);
            // El validador necesita al administrador autenticado para
            // registrar quién autorizó cada ingreso.
            nuevo.setUsuario(usuario);
            detenerCamaraAnterior();
            root.setCenter(vista);
            controladorAcceso = nuevo;
            marcarBotonActivo(btnControlAcceso);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void abrirAnulaciones() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/vistas/fx/AnulacionesView.fxml"));
            Parent vista = loader.load();
            AnulacionesFXController controlador = loader.getController();
            controlador.setUsuario(usuario);

            BorderPane root = obtenerRoot(btnAnulaciones);
            guardarDashboardSiEsNecesario(root);
            detenerCamaraAnterior();

            ScrollPane scroll = new ScrollPane(vista);
            scroll.setFitToWidth(true);
            scroll.setFitToHeight(false);
            scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
            scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
            root.setCenter(scroll);
            marcarBotonActivo(btnAnulaciones);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void abrirReportes() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/vistas/fx/ReportesView.fxml"));
            Parent vista = loader.load();
            BorderPane root = obtenerRoot(btnReportes);
            guardarDashboardSiEsNecesario(root);
            detenerCamaraAnterior();

            ScrollPane scroll = new ScrollPane(vista);
            scroll.setFitToWidth(true);
            scroll.setFitToHeight(false);
            scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
            scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
            scroll.setPannable(false);
            root.setCenter(scroll);
            marcarBotonActivo(btnReportes);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void abrirDashboard() {
        BorderPane root = obtenerRoot(btnDashboard);
        detenerCamaraAnterior();
        if (dashboardOriginal != null) {
            root.setCenter(dashboardOriginal);
        }
        cargarContadoresDashboard();
        marcarBotonActivo(btnDashboard);
    }

    private void marcarBotonActivo(Button botonActivo) {
        Button[] botones = {
            btnDashboard, btnArtistas, btnLocalidades,
            btnConciertos, btnInventario, btnUsuarios, btnControlAcceso,
            btnAnulaciones, btnReportes
        };
        for (Button boton : botones) {
            boton.getStyleClass().remove("menu-button-active");
        }
        if (!botonActivo.getStyleClass().contains("menu-button-active")) {
            botonActivo.getStyleClass().add("menu-button-active");
        }
    }

    @FXML
    private void cerrarSesion() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/vistas/fx/LoginView.fxml"));
            Parent root = loader.load();
            Stage loginStage = new Stage();
            loginStage.setScene(new Scene(root));
            loginStage.setTitle("SVB-GUA");
            loginStage.setResizable(false);
            loginStage.show();
            detenerCamaraAnterior();
            Stage menuStage = (Stage) btnCerrarSesion.getScene().getWindow();
            menuStage.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void cargarContadoresDashboard() {
        ArtistaDAO artistaDAO = new ArtistaDAO();
        ConciertoDAO conciertoDAO = new ConciertoDAO();
        LocalidadDAO localidadDAO = new LocalidadDAO();
        InventarioBoletoDAO inventarioDAO = new InventarioBoletoDAO();
        lblTotalArtistas.setText(String.valueOf(artistaDAO.listarTodos().size()));
        lblTotalConciertos.setText(String.valueOf(conciertoDAO.listarTodos().size()));
        lblTotalLocalidades.setText(String.valueOf(localidadDAO.listarTodos().size()));
        lblTotalInventario.setText(String.valueOf(inventarioDAO.listarTodos().size()));
    }

    private void abrirVistaConScroll(Button boton, String rutaFXML) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(rutaFXML));
            Parent vista = loader.load();
            BorderPane root = obtenerRoot(boton);
            guardarDashboardSiEsNecesario(root);
            ScrollPane scroll = vista instanceof ScrollPane
                    ? (ScrollPane) vista : new ScrollPane(vista);
            scroll.setFitToWidth(true);
            scroll.setFitToHeight(false);
            scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
            scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
            scroll.setPannable(false);
            detenerCamaraAnterior();
            root.setCenter(scroll);
            marcarBotonActivo(boton);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
