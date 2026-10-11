package controladorFx;

import java.io.IOException;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import modelo.Usuario;
import dao.ArtistaDAO;
import dao.ConciertoDAO;
import dao.LocalidadDAO;
import dao.InventarioBoletoDAO;
import javafx.scene.control.ScrollPane;

public class MenuAdministradorFXController {

    @FXML
    private Label lblNombreUsuario;
    
    @FXML
    private Label lblTotalArtistas;

    @FXML
    private Label lblTotalConciertos;

    @FXML
    private Label lblTotalLocalidades;

    @FXML
    private Label lblTotalInventario;

    @FXML
    private Button btnDashboard;

    @FXML
    private Button btnArtistas;

    @FXML
    private Button btnLocalidades;

    @FXML
    private Button btnConciertos;

    @FXML
    private Button btnInventario;

    @FXML
    private Button btnUsuarios;

    @FXML
    private Button btnCerrarSesion;

    private Usuario usuario;

    private Node dashboardOriginal;

    @FXML
    private void initialize() {
        
        cargarContadoresDashboard();

        btnLocalidades.setOnAction(
                e -> abrirLocalidades()
        );

        btnDashboard.setOnAction(
                e -> abrirDashboard()
        );
        
        btnArtistas.setOnAction(
                e -> abrirArtistas()
        );
        
        btnUsuarios.setOnAction(
                e -> abrirUsuarios()        
        );
        
        btnConciertos.setOnAction(
                e -> abrirConciertos()
        );
        btnInventario.setOnAction(
                e -> abrirInventario()
        );
    }

    public void setUsuario(Usuario usuario) {

        this.usuario = usuario;

        if (usuario != null) {

            lblNombreUsuario.setText(
                    usuario.getNombreCompleto()
            );
        }
    }

    public Usuario getUsuario() {
        return usuario;
    }
    


    public void abrirInventario() {

        try {

            BorderPane rootAdmin =
                    (BorderPane) btnInventario
                            .getScene()
                            .getRoot();

            if (dashboardOriginal == null) {
                dashboardOriginal = rootAdmin.getCenter();
            }

            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource(
                            "/vistas/fx/InventarioView.fxml"
                    )
            );

            Parent vistaInventario = loader.load();

            ScrollPane scroll = new ScrollPane();

            scroll.setContent(vistaInventario);
            scroll.setFitToWidth(true);
            scroll.setFitToHeight(false);

            scroll.setHbarPolicy(
                    ScrollPane.ScrollBarPolicy.NEVER
            );

            scroll.setVbarPolicy(
                    ScrollPane.ScrollBarPolicy.AS_NEEDED
            );

            scroll.setPannable(false);

            rootAdmin.setCenter(scroll);

            marcarBotonActivo(btnInventario);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void abrirUsuarios() {

        abrirVistaConScroll(
                btnUsuarios,
                "/vistas/fx/UsuariosView.fxml"
        );
    }

    private void abrirConciertos() {

    try {

        BorderPane rootAdmin =
                (BorderPane) btnConciertos
                        .getScene()
                        .getRoot();

        if (dashboardOriginal == null) {
            dashboardOriginal = rootAdmin.getCenter();
        }

        FXMLLoader loader =
                new FXMLLoader(
                        getClass().getResource(
                                "/vistas/fx/ConciertosView.fxml"
                        )
                );

        Parent vistaConciertos =
                loader.load();

        rootAdmin.setCenter(
                vistaConciertos
        );

        marcarBotonActivo(
                btnConciertos
        );

    } catch (IOException e) {

        e.printStackTrace();
    }
}

    private void abrirLocalidades() {

        abrirVistaConScroll(
                btnLocalidades,
                "/vistas/fx/LocalidadesView.fxml"
        );
    }

    

    private void abrirArtistas() {

        abrirVistaConScroll(
                btnArtistas,
                "/vistas/fx/ArtistasView.fxml"
        );
    }



    private void abrirDashboard() {

        BorderPane rootAdmin =
                (BorderPane) btnDashboard
                        .getScene()
                        .getRoot();

        if (dashboardOriginal != null) {

            rootAdmin.setCenter(
                    dashboardOriginal
            );
        }

        cargarContadoresDashboard();

        marcarBotonActivo(
                btnDashboard
        );
    }


    private void marcarBotonActivo(
            Button botonActivo
    ) {

        Button[] botones = {
            btnDashboard,
            btnArtistas,
            btnLocalidades,
            btnConciertos,
            btnInventario,
            btnUsuarios
        };

        for (Button boton : botones) {

            boton.getStyleClass()
                    .remove(
                            "menu-button-active"
                    );
        }

        if (!botonActivo
                .getStyleClass()
                .contains(
                        "menu-button-active"
                )) {

            botonActivo
                    .getStyleClass()
                    .add(
                            "menu-button-active"
                    );
        }
    }

    @FXML
    private void cerrarSesion() {

        try {

            FXMLLoader loader =
                    new FXMLLoader(
                            getClass().getResource(
                                    "/vistas/fx/LoginView.fxml"
                            )
                    );

            Parent root =
                    loader.load();

            Stage loginStage =
                    new Stage();

            loginStage.setScene(
                    new Scene(root)
            );

            loginStage.setTitle(
                    "SVB-GUA"
            );

            loginStage.setResizable(
                    false
            );

            loginStage.show();

            Stage menuStage =
                    (Stage) btnCerrarSesion
                            .getScene()
                            .getWindow();

            menuStage.close();

        } catch (IOException e) {

            e.printStackTrace();
        }
    }
    

    private void cargarContadoresDashboard() {

        ArtistaDAO artistaDAO = new ArtistaDAO();
        ConciertoDAO conciertoDAO = new ConciertoDAO();
        LocalidadDAO localidadDAO = new LocalidadDAO();
        InventarioBoletoDAO inventarioDAO =
                new InventarioBoletoDAO();

        int totalArtistas =
                artistaDAO.listarTodos().size();

        int totalConciertos =
                conciertoDAO.listarTodos().size();

        int totalLocalidades =
                localidadDAO.listarTodos().size();

        int totalInventario =
                inventarioDAO.listarTodos().size();

        lblTotalArtistas.setText(
                String.valueOf(totalArtistas)
        );

        lblTotalConciertos.setText(
                String.valueOf(totalConciertos)
        );

        lblTotalLocalidades.setText(
                String.valueOf(totalLocalidades)
        );

        lblTotalInventario.setText(
                String.valueOf(totalInventario)
        );
    }
    
    private void abrirVistaConScroll(
            Button boton,
            String rutaFXML) {

        try {

            BorderPane rootAdmin =
                    (BorderPane) boton
                            .getScene()
                            .getRoot();

            if (dashboardOriginal == null) {
                dashboardOriginal = rootAdmin.getCenter();
            }

            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource(rutaFXML)
            );

            Parent vista = loader.load();

            ScrollPane scroll;

            if (vista instanceof ScrollPane) {
                scroll = (ScrollPane) vista;
            } else {
                scroll = new ScrollPane(vista);
            }

            scroll.setFitToWidth(true);
            scroll.setFitToHeight(false);

            scroll.setHbarPolicy(
                    ScrollPane.ScrollBarPolicy.NEVER
            );

            scroll.setVbarPolicy(
                    ScrollPane.ScrollBarPolicy.AS_NEEDED
            );

            scroll.setPannable(false);

            rootAdmin.setCenter(scroll);

            marcarBotonActivo(boton);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    
}