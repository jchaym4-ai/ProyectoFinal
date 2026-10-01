package dao;

import coneccion.CreateConnection;
import modelo.Usuario;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import java.util.ArrayList;
import java.util.List;

public class UsuarioDAO {

    private final CreateConnection connFactory;

    public UsuarioDAO() {
        this.connFactory = new CreateConnection();
    }

    // Buscar usuario por nombre de usuario
    public Usuario buscarPorNombreUsuario(String nombreUsuario) {

        String sql = "SELECT id_usuario, nombre_usuario, contrasena_hash, "
                + "nombre_completo, rol, fecha_creacion "
                + "FROM usuarios "
                + "WHERE nombre_usuario = ?";

        try (
            Connection conn = connFactory.getConection();
            PreparedStatement ps = conn.prepareStatement(sql)
        ) {

            ps.setString(1, nombreUsuario);

            try (ResultSet rs = ps.executeQuery()) {

                if (rs.next()) {

                    Usuario usuario = new Usuario();

                    usuario.setIdUsuario(
                            rs.getInt("id_usuario")
                    );

                    usuario.setNombreUsuario(
                            rs.getString("nombre_usuario")
                    );

                    usuario.setContrasenaHash(
                            rs.getString("contrasena_hash")
                    );

                    usuario.setNombreCompleto(
                            rs.getString("nombre_completo")
                    );

                    usuario.setRol(
                            rs.getString("rol")
                    );

                    if (rs.getTimestamp("fecha_creacion") != null) {
                        usuario.setFechaCreacion(
                                rs.getTimestamp("fecha_creacion")
                                        .toLocalDateTime()
                        );
                    }

                    return usuario;
                }
            }

        } catch (SQLException e) {
            System.err.println(
                    "Error al buscar usuario: " + e.getMessage()
            );
        }

        return null;
    }


    // Insertar usuario
    public boolean insertar(Usuario usuario) {

        String sql = "INSERT INTO usuarios "
                + "(nombre_usuario, contrasena_hash, nombre_completo, rol) "
                + "VALUES (?, ?, ?, ?)";

        try (
            Connection conn = connFactory.getConection();

            PreparedStatement ps = conn.prepareStatement(
                    sql,
                    Statement.RETURN_GENERATED_KEYS
            )
        ) {

            ps.setString(
                    1,
                    usuario.getNombreUsuario()
            );

            ps.setString(
                    2,
                    usuario.getContrasenaHash()
            );

            ps.setString(
                    3,
                    usuario.getNombreCompleto()
            );

            ps.setString(
                    4,
                    usuario.getRol()
            );

            int filas = ps.executeUpdate();

            if (filas > 0) {

                try (ResultSet rs = ps.getGeneratedKeys()) {

                    if (rs.next()) {
                        usuario.setIdUsuario(
                                rs.getInt(1)
                        );
                    }
                }

                return true;
            }

        } catch (SQLException e) {
            System.err.println(
                    "Error al insertar usuario: " + e.getMessage()
            );
        }

        return false;
    }


    // Listar usuarios
    public List<Usuario> listarTodos() {

        List<Usuario> lista = new ArrayList<>();

        String sql = "SELECT id_usuario, nombre_usuario, contrasena_hash, "
                + "nombre_completo, rol, fecha_creacion "
                + "FROM usuarios "
                + "ORDER BY id_usuario";

        try (
            Connection conn = connFactory.getConection();
            PreparedStatement ps = conn.prepareStatement(sql);
            ResultSet rs = ps.executeQuery()
        ) {

            while (rs.next()) {

                Usuario usuario = new Usuario();

                usuario.setIdUsuario(
                        rs.getInt("id_usuario")
                );

                usuario.setNombreUsuario(
                        rs.getString("nombre_usuario")
                );

                usuario.setContrasenaHash(
                        rs.getString("contrasena_hash")
                );

                usuario.setNombreCompleto(
                        rs.getString("nombre_completo")
                );

                usuario.setRol(
                        rs.getString("rol")
                );

                if (rs.getTimestamp("fecha_creacion") != null) {
                    usuario.setFechaCreacion(
                            rs.getTimestamp("fecha_creacion")
                                    .toLocalDateTime()
                    );
                }

                lista.add(usuario);
            }

        } catch (SQLException e) {
            System.err.println(
                    "Error al listar usuarios: " + e.getMessage()
            );
        }

        return lista;
    }


    // Actualizar usuario
    public boolean actualizar(Usuario usuario) {

        String sql = "UPDATE usuarios "
                + "SET nombre_usuario = ?, "
                + "nombre_completo = ?, "
                + "rol = ? "
                + "WHERE id_usuario = ?";

        try (
            Connection conn = connFactory.getConection();
            PreparedStatement ps = conn.prepareStatement(sql)
        ) {

            ps.setString(
                    1,
                    usuario.getNombreUsuario()
            );

            ps.setString(
                    2,
                    usuario.getNombreCompleto()
            );

            ps.setString(
                    3,
                    usuario.getRol()
            );

            ps.setInt(
                    4,
                    usuario.getIdUsuario()
            );

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println(
                    "Error al actualizar usuario: " + e.getMessage()
            );
        }

        return false;
    }


    // Actualizar contraseña
    public boolean actualizarContrasena(
            int idUsuario,
            String nuevaContrasenaHash
    ) {

        String sql = "UPDATE usuarios "
                + "SET contrasena_hash = ? "
                + "WHERE id_usuario = ?";

        try (
            Connection conn = connFactory.getConection();
            PreparedStatement ps = conn.prepareStatement(sql)
        ) {

            ps.setString(
                    1,
                    nuevaContrasenaHash
            );

            ps.setInt(
                    2,
                    idUsuario
            );

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println(
                    "Error al actualizar contraseña: " + e.getMessage()
            );
        }

        return false;
    }
}