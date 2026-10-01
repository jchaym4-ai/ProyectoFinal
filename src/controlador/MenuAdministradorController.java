package controlador;

import vista.ArtistasView;
import vista.MenuAdministradorView;

public class MenuAdministradorController {

    private final MenuAdministradorView vista;

    public MenuAdministradorController(MenuAdministradorView vista) {
        this.vista = vista;

        this.vista.getBtnArtistas().addActionListener(e -> abrirArtistas());
    }

    private void abrirArtistas() {

        ArtistasView artistasView = new ArtistasView();

        new ArtistaController(artistasView);

        artistasView.setLocationRelativeTo(null);
        artistasView.setVisible(true);
    }
}