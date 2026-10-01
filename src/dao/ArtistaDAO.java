package dao;

import coneccion.CreateConnection;
import modelo.Artista;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import java.util.ArrayList;
import java.util.List;

public class ArtistaDAO {

    private final CreateConnection connFactory;

    public ArtistaDAO() {
        this.connFactory = new CreateConnection();
    }

    // INSERTAR
    public boolean insertar(Artista artista) {

        String sql = "INSERT INTO artistas "
                + "(nombre_artistico, genero_musical, pais_origen) "
                + "VALUES (?, ?, ?)";

        try (
            Connection conn = connFactory.getConection();
            PreparedStatement ps = conn.prepareStatement(
                    sql,
                    Statement.RETURN_GENERATED_KEYS
            )
        ) {

            ps.setString(1, artista.getNombreArtistico());
            ps.setString(2, artista.getGeneroMusical());
            ps.setString(3, artista.getPaisOrigen());

            int filas = ps.executeUpdate();

            if (filas > 0) {

                try (ResultSet rs = ps.getGeneratedKeys()) {

                    if (rs.next()) {
                        artista.setIdArtista(rs.getInt(1));
                    }
                }

                return true;
            }

        } catch (SQLException e) {
            System.err.println(
                    "Error al insertar artista: "
                    + e.getMessage()
            );
        }

        return false;
    }


    // LISTAR
    public List<Artista> listarTodos() {

        List<Artista> lista = new ArrayList<>();

        String sql = "SELECT id_artista, "
                + "nombre_artistico, "
                + "genero_musical, "
                + "pais_origen "
                + "FROM artistas "
                + "ORDER BY id_artista";

        try (
            Connection conn = connFactory.getConection();
            PreparedStatement ps = conn.prepareStatement(sql);
            ResultSet rs = ps.executeQuery()
        ) {

            while (rs.next()) {

                Artista artista = new Artista();

                artista.setIdArtista(
                        rs.getInt("id_artista")
                );

                artista.setNombreArtistico(
                        rs.getString("nombre_artistico")
                );

                artista.setGeneroMusical(
                        rs.getString("genero_musical")
                );

                artista.setPaisOrigen(
                        rs.getString("pais_origen")
                );

                lista.add(artista);
            }

        } catch (SQLException e) {
            System.err.println(
                    "Error al listar artistas: "
                    + e.getMessage()
            );
        }

        return lista;
    }


    // ACTUALIZAR
    public boolean actualizar(Artista artista) {

        String sql = "UPDATE artistas "
                + "SET nombre_artistico = ?, "
                + "genero_musical = ?, "
                + "pais_origen = ? "
                + "WHERE id_artista = ?";

        try (
            Connection conn = connFactory.getConection();
            PreparedStatement ps = conn.prepareStatement(sql)
        ) {

            ps.setString(1, artista.getNombreArtistico());
            ps.setString(2, artista.getGeneroMusical());
            ps.setString(3, artista.getPaisOrigen());
            ps.setInt(4, artista.getIdArtista());

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println(
                    "Error al actualizar artista: "
                    + e.getMessage()
            );
        }

        return false;
    }


    // ELIMINAR
    public boolean eliminar(int idArtista) {

        String sql = "DELETE FROM artistas "
                + "WHERE id_artista = ?";

        try (
            Connection conn = connFactory.getConection();
            PreparedStatement ps = conn.prepareStatement(sql)
        ) {

            ps.setInt(1, idArtista);

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println(
                    "Error al eliminar artista: "
                    + e.getMessage()
            );
        }

        return false;
    }
        
    
}