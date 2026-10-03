package controlador;

import dao.LocalidadDAO;
import java.util.List;
import javax.swing.JOptionPane;
import javax.swing.table.DefaultTableModel;
import modelo.Localidad;
import vista.LocalidadesView;

public class LocalidadController {

    private final LocalidadesView vista;
    private final LocalidadDAO localidadDAO;

    private int idSeleccionado = -1;

    public LocalidadController(LocalidadesView vista) {

        this.vista = vista;
        this.localidadDAO = new LocalidadDAO();

        vista.getBtnGuardar()
                .addActionListener(e -> guardar());

        vista.getBtnActualizar()
                .addActionListener(e -> actualizar());

        vista.getBtnEliminar()
                .addActionListener(e -> eliminar());

        vista.getBtnLimpiar()
                .addActionListener(e -> limpiar());

        vista.getTblLocalidades()
                .getSelectionModel()
                .addListSelectionListener(e -> {

                    if (!e.getValueIsAdjusting()) {
                        seleccionarFila();
                    }
                });

        cargarTabla();
    }

    private void guardar() {

        String nombre = vista.getNombre();

        if (nombre.isEmpty()) {

            JOptionPane.showMessageDialog(
                    vista,
                    "El nombre de la localidad es obligatorio."
            );

            return;
        }

        Localidad localidad = new Localidad();

        localidad.setNombre(nombre);

        if (localidadDAO.insertar(localidad)) {

            JOptionPane.showMessageDialog(
                    vista,
                    "Localidad registrada correctamente."
            );

            limpiar();
            cargarTabla();

        } else {

            JOptionPane.showMessageDialog(
                    vista,
                    "No se pudo registrar la localidad."
            );
        }
    }

    private void actualizar() {

        if (idSeleccionado == -1) {

            JOptionPane.showMessageDialog(
                    vista,
                    "Seleccione una localidad."
            );

            return;
        }

        String nombre = vista.getNombre();

        if (nombre.isEmpty()) {

            JOptionPane.showMessageDialog(
                    vista,
                    "El nombre de la localidad es obligatorio."
            );

            return;
        }

        Localidad localidad = new Localidad();

        localidad.setIdLocalidad(idSeleccionado);
        localidad.setNombre(nombre);

        if (localidadDAO.actualizar(localidad)) {

            JOptionPane.showMessageDialog(
                    vista,
                    "Localidad actualizada correctamente."
            );

            limpiar();
            cargarTabla();

        } else {

            JOptionPane.showMessageDialog(
                    vista,
                    "No se pudo actualizar la localidad."
            );
        }
    }

    private void eliminar() {

        if (idSeleccionado == -1) {

            JOptionPane.showMessageDialog(
                    vista,
                    "Seleccione una localidad."
            );

            return;
        }

        int respuesta = JOptionPane.showConfirmDialog(
                vista,
                "¿Desea eliminar esta localidad?",
                "Confirmar eliminación",
                JOptionPane.YES_NO_OPTION
        );

        if (respuesta != JOptionPane.YES_OPTION) {
            return;
        }

        if (localidadDAO.eliminar(idSeleccionado)) {

            JOptionPane.showMessageDialog(
                    vista,
                    "Localidad eliminada correctamente."
            );

            limpiar();
            cargarTabla();

        } else {

            JOptionPane.showMessageDialog(
                    vista,
                    "No se pudo eliminar la localidad."
            );
        }
    }

    private void cargarTabla() {

        DefaultTableModel modelo =
                new DefaultTableModel(
                        new Object[]{
                            "ID",
                            "Nombre"
                        },
                        0
                ) {

                    @Override
                    public boolean isCellEditable(
                            int fila,
                            int columna
                    ) {
                        return false;
                    }
                };

        List<Localidad> localidades =
                localidadDAO.listarTodos();

        for (Localidad localidad : localidades) {

            modelo.addRow(
                    new Object[]{
                        localidad.getIdLocalidad(),
                        localidad.getNombre()
                    }
            );
        }

        vista.getTblLocalidades()
                .setModel(modelo);
    }

    private void seleccionarFila() {

        int fila =
                vista.getTblLocalidades()
                        .getSelectedRow();

        if (fila == -1) {
            return;
        }

        idSeleccionado =
                Integer.parseInt(
                        vista.getTblLocalidades()
                                .getValueAt(fila, 0)
                                .toString()
                );

        vista.setNombre(
                vista.getTblLocalidades()
                        .getValueAt(fila, 1)
                        .toString()
        );
    }

    private void limpiar() {

        idSeleccionado = -1;

        vista.limpiarCampos();

        vista.getTblLocalidades()
                .clearSelection();
    }
}