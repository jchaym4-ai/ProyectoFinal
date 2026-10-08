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

public class MenuAdministradorFXController {

    @FXML
    private Label lblNombreUsuario;

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
    
    private void abrirUsuarios (){
        try{
            
            BorderPane rootAdmin = 
                    (BorderPane) btnUsuarios
                            .getScene()
                            .getRoot();
            if (dashboardOriginal == null){
                dashboardOriginal = 
                        rootAdmin.getCenter();
            }
            
            FXMLLoader loader = 
                    new FXMLLoader (
                            getClass().getResource(
                                    "/vistas/fx/UsuariosView.fxml"
                            )
                    );
            Parent vistaUsuarios =
                    loader.load();
            rootAdmin.setCenter(vistaUsuarios);
            
            marcarBotonActivo(btnArtistas);
        }catch(IOException e) {
            e.printStackTrace();
            
        }
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

        try {

            BorderPane rootAdmin =
                    (BorderPane) btnLocalidades
                            .getScene()
                            .getRoot();

            if (dashboardOriginal == null) {

                dashboardOriginal =
                        rootAdmin.getCenter();
            }

            FXMLLoader loader =
                    new FXMLLoader(
                            getClass().getResource(
                                    "/vistas/fx/LocalidadesView.fxml"
                            )
                    );

            Parent vistaLocalidades =
                    loader.load();

            rootAdmin.setCenter(
                    vistaLocalidades
            );

            marcarBotonActivo(
                    btnLocalidades
            );

        } catch (IOException e) {

            e.printStackTrace();
        }
    }
    
    private void abrirArtistas (){
        
        try{
            
            BorderPane rootAdmin =
                    (BorderPane) btnArtistas
                            .getScene()
                            .getRoot();
            
            if (dashboardOriginal == null) {
                dashboardOriginal =
                        rootAdmin.getCenter();
            }
            FXMLLoader loader =
                    new FXMLLoader (
                                getClass().getResource(
                                        "/vistas/fx/ArtistasView.fxml"
                                        
                                )
                    );
            Parent vistaArtistas =
                    loader.load();
            
            rootAdmin.setCenter(
                    vistaArtistas
            );
            
            marcarBotonActivo(
                    btnArtistas
            );
            
        }catch(IOException e){
            e.printStackTrace();
        }
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
}