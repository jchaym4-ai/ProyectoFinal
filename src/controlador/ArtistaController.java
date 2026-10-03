package controlador;

import dao.ArtistaDAO;
import java.util.List;
import javax.swing.JOptionPane;
import javax.swing.table.DefaultTableModel;
import modelo.Artista;
import vista.ArtistasView;

public class ArtistaController {

    private final ArtistasView vista;
    private final ArtistaDAO artistaDAO;

    private int idSeleccionado = -1;

    public ArtistaController(ArtistasView vista) {

        this.vista = vista;
        this.artistaDAO = new ArtistaDAO();

        vista.getBtnGuardar().addActionListener(e -> guardar());
        vista.getBtnActualizar().addActionListener(e -> actualizar());
        vista.getBtnEliminar().addActionListener(e -> eliminar());
        vista.getBtnLimpiar().addActionListener(e -> limpiar());

        vista.getTblArtistas().getSelectionModel()
                .addListSelectionListener(e -> {

                    if (!e.getValueIsAdjusting()) {
                        seleccionarFila();
                    }
                });

        cargarTabla();
    }

    private void guardar() {

        String nombre = vista.getNombreArtistico();
        String genero = vista.getGeneroMusical();
        String pais = vista.getPaisOrigen();

        if (nombre.isEmpty() || genero.isEmpty()) {

            JOptionPane.showMessageDialog(
                    vista,
                    "Nombre artístico y género son obligatorios."
            );

            return;
        }

        Artista artista = new Artista();

        artista.setNombreArtistico(nombre);
        artista.setGeneroMusical(genero);

        if (pais.isEmpty()) {
            artista.setPaisOrigen(null);
        } else {
            artista.setPaisOrigen(pais);
        }

        if (artistaDAO.insertar(artista)) {

            JOptionPane.showMessageDialog(
                    vista,
                    "Artista registrado correctamente."
            );

            limpiar();
            cargarTabla();

        } else {

            JOptionPane.showMessageDialog(
                    vista,
                    "No se pudo registrar el artista."
            );
        }
    }

    private void actualizar() {

        if (idSeleccionado == -1) {

            JOptionPane.showMessageDialog(
                    vista,
                    "Seleccione un artista."
            );

            return;
        }

        String nombre = vista.getNombreArtistico();
        String genero = vista.getGeneroMusical();
        String pais = vista.getPaisOrigen();

        if (nombre.isEmpty() || genero.isEmpty()) {

            JOptionPane.showMessageDialog(
                    vista,
                    "Nombre artístico y género son obligatorios."
            );

            return;
        }

        Artista artista = new Artista();

        artista.setIdArtista(idSeleccionado);
        artista.setNombreArtistico(nombre);
        artista.setGeneroMusical(genero);

        if (pais.isEmpty()) {
            artista.setPaisOrigen(null);
        } else {
            artista.setPaisOrigen(pais);
        }

        if (artistaDAO.actualizar(artista)) {

            JOptionPane.showMessageDialog(
                    vista,
                    "Artista actualizado correctamente."
            );

            limpiar();
            cargarTabla();

        } else {

            JOptionPane.showMessageDialog(
                    vista,
                    "No se pudo actualizar el artista."
            );
        }
    }

    private void eliminar() {

        if (idSeleccionado == -1) {

            JOptionPane.showMessageDialog(
                    vista,
                    "Seleccione un artista."
            );

            return;
        }

        int respuesta = JOptionPane.showConfirmDialog(
                vista,
                "¿Desea eliminar este artista?",
                "Confirmar",
                JOptionPane.YES_NO_OPTION
        );

        if (respuesta != JOptionPane.YES_OPTION) {
            return;
        }

        if (artistaDAO.eliminar(idSeleccionado)) {

            JOptionPane.showMessageDialog(
                    vista,
                    "Artista eliminado correctamente."
            );

            limpiar();
            cargarTabla();

        } else {

            JOptionPane.showMessageDialog(
                    vista,
                    "No se pudo eliminar el artista."
            );
        }
    }

    private void cargarTabla() {

        DefaultTableModel modelo = new DefaultTableModel(
                new Object[]{
                    "ID",
                    "Nombre artístico",
                    "Género musical",
                    "País"
                },
                0
        ) {
            @Override
            public boolean isCellEditable(
                    int row,
                    int column
            ) {
                return false;
            }
        };

        List<Artista> artistas =
                artistaDAO.listarTodos();

        for (Artista artista : artistas) {

            modelo.addRow(
                    new Object[]{
                        artista.getIdArtista(),
                        artista.getNombreArtistico(),
                        artista.getGeneroMusical(),
                        artista.getPaisOrigen()
                    }
            );
        }

        vista.getTblArtistas().setModel(modelo);
    }

    private void seleccionarFila() {

        int fila =
                vista.getTblArtistas().getSelectedRow();

        if (fila == -1) {
            return;
        }

        idSeleccionado =
                Integer.parseInt(
                        vista.getTblArtistas()
                                .getValueAt(fila, 0)
                                .toString()
                );

        vista.setNombreArtistico(
                vista.getTblArtistas()
                        .getValueAt(fila, 1)
                        .toString()
        );

        vista.setGeneroMusical(
                vista.getTblArtistas()
                        .getValueAt(fila, 2)
                        .toString()
        );

        Object pais =
                vista.getTblArtistas()
                        .getValueAt(fila, 3);

        vista.setPaisOrigen(
                pais == null ? "" : pais.toString()
        );
    }

    private void limpiar() {

        idSeleccionado = -1;

        vista.limpiarCampos();

        vista.getTblArtistas()
                .clearSelection();
    }
}