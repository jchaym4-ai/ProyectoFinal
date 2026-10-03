package controlador;

import vista.ArtistasView;
import vista.LocalidadesView;
import vista.MenuAdministradorView;
import vista.ConciertosView;

public class MenuAdministradorController {

    private final MenuAdministradorView vista;

    public MenuAdministradorController(MenuAdministradorView vista) {
        this.vista = vista;

        this.vista.getBtnArtistas().addActionListener(e -> abrirArtistas());
        vista.getBtnLocalidades()
        .addActionListener(e -> abrirLocalidades());
        vista.getBtnConciertos()
        .addActionListener(e -> abrirConciertos());
    }

    private void abrirArtistas() {

        ArtistasView artistasView = new ArtistasView();

        new ArtistaController(artistasView);

        artistasView.setLocationRelativeTo(null);
        artistasView.setVisible(true);
    }
    private void abrirLocalidades() {

        LocalidadesView localidadesView =
                new LocalidadesView();

        new LocalidadController(localidadesView);

        localidadesView.setLocationRelativeTo(null);
        localidadesView.setVisible(true);
    }
    private void abrirConciertos() {

        ConciertosView conciertosView =
                new ConciertosView();

        new ConciertoController(
                conciertosView
        );

        conciertosView.setLocationRelativeTo(null);
        conciertosView.setVisible(true);
    }    
}