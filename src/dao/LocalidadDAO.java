package dao;

import coneccion.CreateConnection;
import modelo.Localidad;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import java.util.ArrayList;
import java.util.List;

public class LocalidadDAO {

    private final CreateConnection connFactory;

    public LocalidadDAO() {
        this.connFactory = new CreateConnection();
    }

    // INSERTAR
    public boolean insertar(Localidad localidad) {

        String sql = "INSERT INTO localidades (nombre) VALUES (?)";

        try (
            Connection conn = connFactory.getConection();
            PreparedStatement ps = conn.prepareStatement(
                    sql,
                    Statement.RETURN_GENERATED_KEYS
            )
        ) {

            ps.setString(1, localidad.getNombre());

            int filas = ps.executeUpdate();

            if (filas > 0) {

                try (ResultSet rs = ps.getGeneratedKeys()) {

                    if (rs.next()) {
                        localidad.setIdLocalidad(rs.getInt(1));
                    }
                }

                return true;
            }

        } catch (SQLException e) {
            System.err.println(
                    "Error al insertar localidad: "
                    + e.getMessage()
            );
        }

        return false;
    }

    // LISTAR
    public List<Localidad> listarTodos() {

        List<Localidad> lista = new ArrayList<>();

        String sql = "SELECT id_localidad, nombre "
                + "FROM localidades "
                + "ORDER BY id_localidad";

        try (
            Connection conn = connFactory.getConection();
            PreparedStatement ps = conn.prepareStatement(sql);
            ResultSet rs = ps.executeQuery()
        ) {

            while (rs.next()) {

                Localidad localidad = new Localidad();

                localidad.setIdLocalidad(
                        rs.getInt("id_localidad")
                );

                localidad.setNombre(
                        rs.getString("nombre")
                );

                lista.add(localidad);
            }

        } catch (SQLException e) {
            System.err.println(
                    "Error al listar localidades: "
                    + e.getMessage()
            );
        }

        return lista;
    }

    // ACTUALIZAR
    public boolean actualizar(Localidad localidad) {

        String sql = "UPDATE localidades "
                + "SET nombre = ? "
                + "WHERE id_localidad = ?";

        try (
            Connection conn = connFactory.getConection();
            PreparedStatement ps = conn.prepareStatement(sql)
        ) {

            ps.setString(1, localidad.getNombre());
            ps.setInt(2, localidad.getIdLocalidad());

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println(
                    "Error al actualizar localidad: "
                    + e.getMessage()
            );
        }

        return false;
    }

    // ELIMINAR
    public boolean eliminar(int idLocalidad) {

        String sql = "DELETE FROM localidades "
                + "WHERE id_localidad = ?";

        try (
            Connection conn = connFactory.getConection();
            PreparedStatement ps = conn.prepareStatement(sql)
        ) {

            ps.setInt(1, idLocalidad);

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println(
                    "Error al eliminar localidad: "
                    + e.getMessage()
            );
        }

        return false;
    }
}