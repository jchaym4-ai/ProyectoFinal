package controlador;

import dao.ArtistaDAO;
import dao.ConciertoDAO;

import java.io.File;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;

import modelo.Artista;
import modelo.Concierto;
import vista.ConciertosView;

public class ConciertoController {

    private final ConciertosView vista;
    private final ConciertoDAO conciertoDAO;
    private final ArtistaDAO artistaDAO;

    private List<Artista> artistas;
    private List<Concierto> conciertos;

    private int idSeleccionado = -1;

    private final DateTimeFormatter formatoFecha =
            DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final DateTimeFormatter formatoHora =
            DateTimeFormatter.ofPattern("HH:mm");


    public ConciertoController(ConciertosView vista) {

        this.vista = vista;
        this.conciertoDAO = new ConciertoDAO();
        this.artistaDAO = new ArtistaDAO();

        this.artistas = new ArrayList<>();
        this.conciertos = new ArrayList<>();

        // Botones
        vista.getBtnGuardar()
                .addActionListener(e -> guardar());

        vista.getBtnActualizar()
                .addActionListener(e -> actualizar());

        vista.getBtnEliminar()
                .addActionListener(e -> eliminar());

        vista.getBtnLimpiar()
                .addActionListener(e -> limpiar());

        vista.getBtnSeleccionarImagen()
                .addActionListener(e -> seleccionarImagen());

        // Tabla
        vista.getTblConciertos()
                .getSelectionModel()
                .addListSelectionListener(e -> {

                    if (!e.getValueIsAdjusting()) {
                        seleccionarFila();
                    }
                });

        cargarArtistas();
        cargarTabla();
    }


    // =====================================================
    // CARGAR ARTISTAS EN EL COMBO
    // =====================================================
    private void cargarArtistas() {

        artistas = artistaDAO.listarTodos();

        vista.getCmbArtista().removeAllItems();

        for (Artista artista : artistas) {

            vista.getCmbArtista().addItem(
                    artista.getNombreArtistico()
            );
        }
    }


    // =====================================================
    // GUARDAR
    // =====================================================
    private void guardar() {

        if (!validarCampos()) {
            return;
        }

        try {

            Concierto concierto =
                    obtenerConciertoFormulario();

            if (conciertoDAO.insertar(concierto)) {

                JOptionPane.showMessageDialog(
                        vista,
                        "Concierto registrado correctamente."
                );

                limpiar();
                cargarTabla();

            } else {

                JOptionPane.showMessageDialog(
                        vista,
                        "No se pudo registrar el concierto."
                );
            }

        } catch (DateTimeParseException e) {

            mostrarErrorFechaHora();
        }
    }


    // =====================================================
    // ACTUALIZAR
    // =====================================================
    private void actualizar() {

        if (idSeleccionado == -1) {

            JOptionPane.showMessageDialog(
                    vista,
                    "Seleccione un concierto de la tabla."
            );

            return;
        }

        if (!validarCampos()) {
            return;
        }

        try {

            Concierto concierto =
                    obtenerConciertoFormulario();

            concierto.setIdConcierto(
                    idSeleccionado
            );

            if (conciertoDAO.actualizar(concierto)) {

                JOptionPane.showMessageDialog(
                        vista,
                        "Concierto actualizado correctamente."
                );

                limpiar();
                cargarTabla();

            } else {

                JOptionPane.showMessageDialog(
                        vista,
                        "No se pudo actualizar el concierto."
                );
            }

        } catch (DateTimeParseException e) {

            mostrarErrorFechaHora();
        }
    }


    // =====================================================
    // ELIMINAR
    // =====================================================
    private void eliminar() {

        if (idSeleccionado == -1) {

            JOptionPane.showMessageDialog(
                    vista,
                    "Seleccione un concierto de la tabla."
            );

            return;
        }

        int respuesta =
                JOptionPane.showConfirmDialog(
                        vista,
                        "¿Está seguro de eliminar este concierto?",
                        "Confirmar eliminación",
                        JOptionPane.YES_NO_OPTION
                );

        if (respuesta != JOptionPane.YES_OPTION) {
            return;
        }

        if (conciertoDAO.eliminar(idSeleccionado)) {

            JOptionPane.showMessageDialog(
                    vista,
                    "Concierto eliminado correctamente."
            );

            limpiar();
            cargarTabla();

        } else {

            JOptionPane.showMessageDialog(
                    vista,
                    "No se pudo eliminar el concierto."
            );
        }
    }


    // =====================================================
    // OBTENER DATOS DEL FORMULARIO
    // =====================================================
    private Concierto obtenerConciertoFormulario()
            throws DateTimeParseException {

        Concierto concierto =
                new Concierto();

        int indiceArtista =
                vista.getCmbArtista().getSelectedIndex();

        Artista artista =
                artistas.get(indiceArtista);

        // Fecha
        LocalDate fecha =
                LocalDate.parse(
                        vista.getFechaConcierto(),
                        formatoFecha
                );

        // Hora concierto
        LocalTime horaConcierto =
                LocalTime.parse(
                        vista.getHoraConcierto(),
                        formatoHora
                );

        LocalDateTime fechaHoraConcierto =
                LocalDateTime.of(
                        fecha,
                        horaConcierto
                );

        // Hora apertura puede ser opcional
        LocalTime horaApertura = null;

        if (!vista.getHoraApertura().isEmpty()) {

            horaApertura =
                    LocalTime.parse(
                            vista.getHoraApertura(),
                            formatoHora
                    );
        }

        concierto.setIdArtista(
                artista.getIdArtista()
        );

        concierto.setTituloEvento(
                vista.getTituloEvento()
        );

        concierto.setFechaConcierto(
                fechaHoraConcierto
        );

        concierto.setRecinto(
                vista.getRecinto()
        );

        concierto.setEstado(
                vista.getCmbEstado()
                        .getSelectedItem()
                        .toString()
        );

        concierto.setDescripcion(
                convertirNull(
                        vista.getDescripcion()
                )
        );

        concierto.setInformacionAdicional(
                convertirNull(
                        vista.getInformacionAdicional()
                )
        );

        concierto.setImagenEvento(
                convertirNull(
                        vista.getImagenEvento()
                )
        );

        concierto.setHoraApertura(
                horaApertura
        );

        return concierto;
    }


    // =====================================================
    // VALIDAR CAMPOS
    // =====================================================
    private boolean validarCampos() {

        if (artistas.isEmpty()) {

            JOptionPane.showMessageDialog(
                    vista,
                    "Debe registrar al menos un artista antes de crear un concierto."
            );

            return false;
        }

        if (vista.getCmbArtista()
                .getSelectedIndex() == -1) {

            JOptionPane.showMessageDialog(
                    vista,
                    "Seleccione un artista."
            );

            return false;
        }

        if (vista.getTituloEvento().isEmpty()) {

            JOptionPane.showMessageDialog(
                    vista,
                    "El título del evento es obligatorio."
            );

            return false;
        }

        if (vista.getFechaConcierto().isEmpty()) {

            JOptionPane.showMessageDialog(
                    vista,
                    "La fecha del concierto es obligatoria."
            );

            return false;
        }

        if (vista.getHoraConcierto().isEmpty()) {

            JOptionPane.showMessageDialog(
                    vista,
                    "La hora del concierto es obligatoria."
            );

            return false;
        }

        if (vista.getRecinto().isEmpty()) {

            JOptionPane.showMessageDialog(
                    vista,
                    "El recinto es obligatorio."
            );

            return false;
        }

        return true;
    }


    // =====================================================
    // CARGAR TABLA
    // =====================================================
    private void cargarTabla() {

        DefaultTableModel modelo =
                new DefaultTableModel(
                        new Object[]{
                            "ID",
                            "Artista",
                            "Evento",
                            "Fecha",
                            "Hora",
                            "Recinto",
                            "Estado"
                        },
                        0
                ) {

                    @Override
                    public boolean isCellEditable(
                            int fila,
                            int columna) {

                        return false;
                    }
                };

        conciertos =
                conciertoDAO.listarTodos();

        for (Concierto concierto : conciertos) {

            String artista =
                    obtenerNombreArtista(
                            concierto.getIdArtista()
                    );

            String fecha = "";

            String hora = "";

            if (concierto.getFechaConcierto()
                    != null) {

                fecha =
                        concierto
                                .getFechaConcierto()
                                .toLocalDate()
                                .format(formatoFecha);

                hora =
                        concierto
                                .getFechaConcierto()
                                .toLocalTime()
                                .format(formatoHora);
            }

            modelo.addRow(
                    new Object[]{
                        concierto.getIdConcierto(),
                        artista,
                        concierto.getTituloEvento(),
                        fecha,
                        hora,
                        concierto.getRecinto(),
                        concierto.getEstado()
                    }
            );
        }

        vista.getTblConciertos()
                .setModel(modelo);
    }


    // =====================================================
    // SELECCIONAR FILA
    // =====================================================
    private void seleccionarFila() {

        int fila =
                vista.getTblConciertos()
                        .getSelectedRow();

        if (fila == -1) {
            return;
        }

        if (fila >= conciertos.size()) {
            return;
        }

        Concierto concierto =
                conciertos.get(fila);

        idSeleccionado =
                concierto.getIdConcierto();

        // Seleccionar artista
        seleccionarArtista(
                concierto.getIdArtista()
        );

        vista.setTituloEvento(
                concierto.getTituloEvento()
        );

        if (concierto.getFechaConcierto()
                != null) {

            vista.setFechaConcierto(
                    concierto
                            .getFechaConcierto()
                            .toLocalDate()
                            .format(formatoFecha)
            );

            vista.setHoraConcierto(
                    concierto
                            .getFechaConcierto()
                            .toLocalTime()
                            .format(formatoHora)
            );
        }

        vista.setRecinto(
                concierto.getRecinto()
        );

        vista.getCmbEstado()
                .setSelectedItem(
                        concierto.getEstado()
                );

        vista.setDescripcion(
                concierto.getDescripcion() == null
                        ? ""
                        : concierto.getDescripcion()
        );

        vista.setInformacionAdicional(
                concierto.getInformacionAdicional() == null
                        ? ""
                        : concierto.getInformacionAdicional()
        );

        vista.setImagenEvento(
                concierto.getImagenEvento() == null
                        ? ""
                        : concierto.getImagenEvento()
        );

        if (concierto.getHoraApertura()
                != null) {

            vista.setHoraApertura(
                    concierto
                            .getHoraApertura()
                            .format(formatoHora)
            );

        } else {

            vista.setHoraApertura("");
        }
    }


    // =====================================================
    // SELECCIONAR ARTISTA POR ID
    // =====================================================
    private void seleccionarArtista(
            int idArtista) {

        for (int i = 0;
                i < artistas.size();
                i++) {

            if (artistas.get(i)
                    .getIdArtista()
                    == idArtista) {

                vista.getCmbArtista()
                        .setSelectedIndex(i);

                break;
            }
        }
    }


    // =====================================================
    // BUSCAR NOMBRE DEL ARTISTA
    // =====================================================
    private String obtenerNombreArtista(
            int idArtista) {

        for (Artista artista : artistas) {

            if (artista.getIdArtista()
                    == idArtista) {

                return artista
                        .getNombreArtistico();
            }
        }

        return "Desconocido";
    }


    // =====================================================
    // SELECCIONAR IMAGEN
    // =====================================================
    private void seleccionarImagen() {

        JFileChooser chooser =
                new JFileChooser();

        FileNameExtensionFilter filtro =
                new FileNameExtensionFilter(
                        "Imágenes JPG, JPEG y PNG",
                        "jpg",
                        "jpeg",
                        "png"
                );

        chooser.setFileFilter(filtro);

        int resultado =
                chooser.showOpenDialog(vista);

        if (resultado
                == JFileChooser.APPROVE_OPTION) {

            File archivo =
                    chooser.getSelectedFile();

            vista.setImagenEvento(
                    archivo.getAbsolutePath()
            );
        }
    }


    // =====================================================
    // LIMPIAR
    // =====================================================
    private void limpiar() {

        idSeleccionado = -1;

        vista.limpiarCampos();

        vista.getTblConciertos()
                .clearSelection();
    }


    // =====================================================
    // CONVERTIR TEXTO VACÍO A NULL
    // =====================================================
    private String convertirNull(
            String texto) {

        if (texto == null
                || texto.trim().isEmpty()) {

            return null;
        }

        return texto.trim();
    }


    // =====================================================
    // ERROR DE FECHA / HORA
    // =====================================================
    private void mostrarErrorFechaHora() {

        JOptionPane.showMessageDialog(
                vista,
                "Formato de fecha u hora incorrecto.\n\n"
                + "Fecha: dd/MM/yyyy\n"
                + "Hora: HH:mm\n\n"
                + "Ejemplo:\n"
                + "25/11/2026\n"
                + "20:30",
                "Formato incorrecto",
                JOptionPane.ERROR_MESSAGE
        );
    }
}