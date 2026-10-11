package controladorFx;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import modelo.Usuario;

public class MenuVendedorFXController {

    @FXML
    private Label lblNombreUsuario;

    private Usuario usuario;

    public void setUsuario(Usuario usuario) {

        if (usuario == null) {
            throw new IllegalArgumentException(
                    "El usuario no puede ser nulo."
            );
        }

        this.usuario = usuario;

        lblNombreUsuario.setText(
                usuario.getNombreCompleto()
        );
    }
}