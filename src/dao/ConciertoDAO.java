package dao;

import coneccion.CreateConnection;
import modelo.Concierto;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Time;
import java.sql.Types;

import java.util.ArrayList;
import java.util.List;

public class ConciertoDAO {

    private final CreateConnection connFactory;

    public ConciertoDAO() {
        this.connFactory = new CreateConnection();
    }

    // =====================================================
    // INSERTAR
    // =====================================================
    public boolean insertar(Concierto concierto) {

        String sql = "INSERT INTO conciertos "
                + "(id_artista, titulo_evento, fecha_concierto, recinto, "
                + "estado, descripcion, informacion_adicional, "
                + "imagen_evento, hora_apertura) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (
            Connection conn = connFactory.getConection();
            PreparedStatement ps = conn.prepareStatement(
                    sql,
                    Statement.RETURN_GENERATED_KEYS
            )
        ) {

            ps.setInt(
                    1,
                    concierto.getIdArtista()
            );

            ps.setString(
                    2,
                    concierto.getTituloEvento()
            );

            ps.setTimestamp(
                    3,
                    Timestamp.valueOf(
                            concierto.getFechaConcierto()
                    )
            );

            ps.setString(
                    4,
                    concierto.getRecinto()
            );

            ps.setString(
                    5,
                    concierto.getEstado()
            );

            // Descripción
            if (concierto.getDescripcion() == null) {

                ps.setNull(
                        6,
                        Types.VARCHAR
                );

            } else {

                ps.setString(
                        6,
                        concierto.getDescripcion()
                );
            }

            // Información adicional
            if (concierto.getInformacionAdicional() == null) {

                ps.setNull(
                        7,
                        Types.VARCHAR
                );

            } else {

                ps.setString(
                        7,
                        concierto.getInformacionAdicional()
                );
            }

            // Imagen
            if (concierto.getImagenEvento() == null) {

                ps.setNull(
                        8,
                        Types.VARCHAR
                );

            } else {

                ps.setString(
                        8,
                        concierto.getImagenEvento()
                );
            }

            // Hora de apertura
            if (concierto.getHoraApertura() == null) {

                ps.setNull(
                        9,
                        Types.TIME
                );

            } else {

                ps.setTime(
                        9,
                        Time.valueOf(
                                concierto.getHoraApertura()
                        )
                );
            }

            int filas = ps.executeUpdate();

            if (filas > 0) {

                try (ResultSet rs = ps.getGeneratedKeys()) {

                    if (rs.next()) {

                        concierto.setIdConcierto(
                                rs.getInt(1)
                        );
                    }
                }

                return true;
            }

        } catch (SQLException e) {

            System.err.println(
                    "Error al insertar concierto: "
                    + e.getMessage()
            );
        }

        return false;
    }


    // =====================================================
    // LISTAR
    // =====================================================
    public List<Concierto> listarTodos() {

        List<Concierto> lista =
                new ArrayList<>();

        String sql = "SELECT "
                + "id_concierto, "
                + "id_artista, "
                + "titulo_evento, "
                + "fecha_concierto, "
                + "recinto, "
                + "estado, "
                + "descripcion, "
                + "informacion_adicional, "
                + "imagen_evento, "
                + "hora_apertura "
                + "FROM conciertos "
                + "ORDER BY fecha_concierto";

        try (
            Connection conn = connFactory.getConection();
            PreparedStatement ps = conn.prepareStatement(sql);
            ResultSet rs = ps.executeQuery()
        ) {

            while (rs.next()) {

                Concierto concierto =
                        new Concierto();

                concierto.setIdConcierto(
                        rs.getInt("id_concierto")
                );

                concierto.setIdArtista(
                        rs.getInt("id_artista")
                );

                concierto.setTituloEvento(
                        rs.getString("titulo_evento")
                );

                Timestamp fecha =
                        rs.getTimestamp("fecha_concierto");

                if (fecha != null) {

                    concierto.setFechaConcierto(
                            fecha.toLocalDateTime()
                    );
                }

                concierto.setRecinto(
                        rs.getString("recinto")
                );

                concierto.setEstado(
                        rs.getString("estado")
                );

                concierto.setDescripcion(
                        rs.getString("descripcion")
                );

                concierto.setInformacionAdicional(
                        rs.getString(
                                "informacion_adicional"
                        )
                );

                concierto.setImagenEvento(
                        rs.getString("imagen_evento")
                );

                Time hora =
                        rs.getTime("hora_apertura");

                if (hora != null) {

                    concierto.setHoraApertura(
                            hora.toLocalTime()
                    );
                }

                lista.add(concierto);
            }

        } catch (SQLException e) {

            System.err.println(
                    "Error al listar conciertos: "
                    + e.getMessage()
            );
        }

        return lista;
    }


    // =====================================================
    // ACTUALIZAR
    // =====================================================
    public boolean actualizar(Concierto concierto) {

        String sql = "UPDATE conciertos SET "
                + "id_artista = ?, "
                + "titulo_evento = ?, "
                + "fecha_concierto = ?, "
                + "recinto = ?, "
                + "estado = ?, "
                + "descripcion = ?, "
                + "informacion_adicional = ?, "
                + "imagen_evento = ?, "
                + "hora_apertura = ? "
                + "WHERE id_concierto = ?";

        try (
            Connection conn = connFactory.getConection();
            PreparedStatement ps = conn.prepareStatement(sql)
        ) {

            ps.setInt(
                    1,
                    concierto.getIdArtista()
            );

            ps.setString(
                    2,
                    concierto.getTituloEvento()
            );

            ps.setTimestamp(
                    3,
                    Timestamp.valueOf(
                            concierto.getFechaConcierto()
                    )
            );

            ps.setString(
                    4,
                    concierto.getRecinto()
            );

            ps.setString(
                    5,
                    concierto.getEstado()
            );

            if (concierto.getDescripcion() == null) {

                ps.setNull(
                        6,
                        Types.VARCHAR
                );

            } else {

                ps.setString(
                        6,
                        concierto.getDescripcion()
                );
            }

            if (concierto.getInformacionAdicional() == null) {

                ps.setNull(
                        7,
                        Types.VARCHAR
                );

            } else {

                ps.setString(
                        7,
                        concierto.getInformacionAdicional()
                );
            }

            if (concierto.getImagenEvento() == null) {

                ps.setNull(
                        8,
                        Types.VARCHAR
                );

            } else {

                ps.setString(
                        8,
                        concierto.getImagenEvento()
                );
            }

            if (concierto.getHoraApertura() == null) {

                ps.setNull(
                        9,
                        Types.TIME
                );

            } else {

                ps.setTime(
                        9,
                        Time.valueOf(
                                concierto.getHoraApertura()
                        )
                );
            }

            ps.setInt(
                    10,
                    concierto.getIdConcierto()
            );

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {

            System.err.println(
                    "Error al actualizar concierto: "
                    + e.getMessage()
            );
        }

        return false;
    }


    // =====================================================
    // ELIMINAR
    // =====================================================
    public boolean eliminar(int idConcierto) {

        String sql = "DELETE FROM conciertos "
                + "WHERE id_concierto = ?";

        try (
            Connection conn = connFactory.getConection();
            PreparedStatement ps = conn.prepareStatement(sql)
        ) {

            ps.setInt(
                    1,
                    idConcierto
            );

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {

            System.err.println(
                    "Error al eliminar concierto: "
                    + e.getMessage()
            );
        }

        return false;
    }
}