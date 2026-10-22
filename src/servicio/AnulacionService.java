package servicio;

import dao.AnulacionDAO;
import java.sql.SQLException;
import java.util.List;

/**
 * Reglas de negocio de anulaciones. SOLO debe invocarse desde una sesión
 * autenticada de administrador; la verificación del rol en la base de datos
 * requiere confirmar primero el esquema exacto de usuarios/roles.
 */
public final class AnulacionService {

    private final AnulacionDAO dao = new AnulacionDAO();

    public List<AnulacionDAO.BoletoParaAnulacion> listarTodos() throws SQLException {
        return dao.listarTodos();
    }

    public AnulacionDAO.BoletoParaAnulacion consultarBoleto(int idBoleto)
            throws SQLException {
        return dao.consultarBoleto(idBoleto);
    }

    public List<AnulacionDAO.BoletoParaAnulacion> consultarVenta(int idVenta)
            throws SQLException {
        return dao.consultarVenta(idVenta);
    }

    /** Anula SOLO una entrada, sin tocar otras entradas de esa compra. */
    public AnulacionDAO.ResultadoAnulacion anularBoleto(
            int idBoleto, int idAdministrador, String motivo) throws SQLException {
        validarParametros(idBoleto, idAdministrador, motivo);
        return dao.anularBoleto(idBoleto, idAdministrador, motivo.trim());
    }

    /**
     * Anula la venta COMPLETA como unidad atómica: si un boleto fue usado,
     * cancelado o ingresó, la transacción no cambia ningún boleto de la venta.
     */
    public AnulacionDAO.ResultadoAnulacion anularVentaCompleta(
            int idVenta, int idAdministrador, String motivo) throws SQLException {
        validarParametros(idVenta, idAdministrador, motivo);
        return dao.anularVentaCompleta(idVenta, idAdministrador, motivo.trim());
    }

    private void validarParametros(int identificador, int idAdministrador, String motivo) {
        if (identificador <= 0) {
            throw new IllegalArgumentException("Debes indicar un identificador válido.");
        }
        if (idAdministrador <= 0) {
            throw new IllegalArgumentException("No hay un administrador autenticado.");
        }
        if (motivo == null || motivo.trim().length() < 5 || motivo.trim().length() > 500) {
            throw new IllegalArgumentException(
                    "El motivo debe tener entre 5 y 500 caracteres.");
        }
    }
}
