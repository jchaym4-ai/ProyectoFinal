package dao;

import coneccion.CreateConnection;
import modelo.InventarioBoleto;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import java.util.ArrayList;
import java.util.List;

public class InventarioBoletoDAO {

    private final CreateConnection connFactory;

    public InventarioBoletoDAO() {
        this.connFactory = new CreateConnection();
    }

    // =====================================================
    // INSERTAR
    // =====================================================
    public boolean insertar(InventarioBoleto inventario) {

        String sql = "INSERT INTO inventario_boletos "
                + "(id_concierto, id_localidad, precio, "
                + "cantidad_total, cantidad_disponible, tipo_venta) "
                + "VALUES (?, ?, ?, ?, ?, ?)";

        try (
            Connection conn = connFactory.getConection();
            PreparedStatement ps = conn.prepareStatement(
                    sql,
                    Statement.RETURN_GENERATED_KEYS
            )
        ) {

            ps.setInt(
                    1,
                    inventario.getIdConcierto()
            );

            ps.setInt(
                    2,
                    inventario.getIdLocalidad()
            );

            ps.setBigDecimal(
                    3,
                    inventario.getPrecio()
            );

            ps.setInt(
                    4,
                    inventario.getCantidadTotal()
            );

            ps.setInt(
                    5,
                    inventario.getCantidadDisponible()
            );

            ps.setString(
                    6,
                    inventario.getTipoVenta()
            );

            int filas = ps.executeUpdate();

            if (filas > 0) {

                try (ResultSet rs = ps.getGeneratedKeys()) {

                    if (rs.next()) {

                        inventario.setIdInventario(
                                rs.getInt(1)
                        );
                    }
                }

                return true;
            }

        } catch (SQLException e) {

            System.err.println(
                    "Error al insertar inventario: "
                    + e.getMessage()
            );
        }

        return false;
    }


    // =====================================================
    // LISTAR TODOS
    // =====================================================
    public List<InventarioBoleto> listarTodos() {

        List<InventarioBoleto> lista =
                new ArrayList<>();

        String sql = "SELECT "
                + "id_inventario, "
                + "id_concierto, "
                + "id_localidad, "
                + "precio, "
                + "cantidad_total, "
                + "cantidad_disponible, "
                + "tipo_venta "
                + "FROM inventario_boletos "
                + "ORDER BY id_inventario";

        try (
            Connection conn = connFactory.getConection();
            PreparedStatement ps = conn.prepareStatement(sql);
            ResultSet rs = ps.executeQuery()
        ) {

            while (rs.next()) {

                InventarioBoleto inventario =
                        new InventarioBoleto();

                inventario.setIdInventario(
                        rs.getInt("id_inventario")
                );

                inventario.setIdConcierto(
                        rs.getInt("id_concierto")
                );

                inventario.setIdLocalidad(
                        rs.getInt("id_localidad")
                );

                inventario.setPrecio(
                        rs.getBigDecimal("precio")
                );

                inventario.setCantidadTotal(
                        rs.getInt("cantidad_total")
                );

                inventario.setCantidadDisponible(
                        rs.getInt("cantidad_disponible")
                );

                inventario.setTipoVenta(
                        rs.getString("tipo_venta")
                );

                lista.add(inventario);
            }

        } catch (SQLException e) {

            System.err.println(
                    "Error al listar inventario: "
                    + e.getMessage()
            );
        }

        return lista;
    }


    // =====================================================
    // ACTUALIZAR
    // =====================================================
    public boolean actualizar(
            InventarioBoleto inventario) {

        String sql = "UPDATE inventario_boletos SET "
                + "id_concierto = ?, "
                + "id_localidad = ?, "
                + "precio = ?, "
                + "cantidad_total = ?, "
                + "cantidad_disponible = ?, "
                + "tipo_venta = ? "
                + "WHERE id_inventario = ?";

        try (
            Connection conn = connFactory.getConection();
            PreparedStatement ps = conn.prepareStatement(sql)
        ) {

            ps.setInt(
                    1,
                    inventario.getIdConcierto()
            );

            ps.setInt(
                    2,
                    inventario.getIdLocalidad()
            );

            ps.setBigDecimal(
                    3,
                    inventario.getPrecio()
            );

            ps.setInt(
                    4,
                    inventario.getCantidadTotal()
            );

            ps.setInt(
                    5,
                    inventario.getCantidadDisponible()
            );

            ps.setString(
                    6,
                    inventario.getTipoVenta()
            );

            ps.setInt(
                    7,
                    inventario.getIdInventario()
            );

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {

            System.err.println(
                    "Error al actualizar inventario: "
                    + e.getMessage()
            );
        }

        return false;
    }


    // =====================================================
    // ELIMINAR
    // =====================================================
    public boolean eliminar(int idInventario) {

        String sql =
                "DELETE FROM inventario_boletos "
                + "WHERE id_inventario = ?";

        try (
            Connection conn = connFactory.getConection();
            PreparedStatement ps = conn.prepareStatement(sql)
        ) {

            ps.setInt(
                    1,
                    idInventario
            );

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {

            System.err.println(
                    "Error al eliminar inventario: "
                    + e.getMessage()
            );
        }

        return false;
    }
}