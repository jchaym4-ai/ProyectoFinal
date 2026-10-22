package servicio;

import dao.ControlAccesoDAO;
import java.net.URI;
import java.net.URISyntaxException;
import java.sql.SQLException;
import modelo.ResultadoValidacionAcceso;

/**
 * Reglas de entrada: acepta exclusivamente los enlaces QR emitidos por SVB-GUA.
 * La lectura de la camara no debe llamar este metodo automaticamente hasta
 * que el operador seleccione el concierto y confirme la validacion.
 */
public class ValidadorBoletoService {

    private static final String HOST_PERMITIDO =
            "svb-gua-boletos.jchaym4.workers.dev";

    private final ControlAccesoDAO controlAccesoDAO;

    public ValidadorBoletoService() {
        this.controlAccesoDAO = new ControlAccesoDAO();
    }

    public ResultadoValidacionAcceso validarIngreso(
            String contenidoQr, int idConcierto, int idUsuario)
            throws SQLException {

        if (idConcierto <= 0) {
            throw new IllegalArgumentException(
                    "Selecciona primero el concierto donde se validara el ingreso.");
        }
        if (idUsuario <= 0) {
            throw new IllegalArgumentException(
                    "No hay un usuario autenticado para registrar el ingreso.");
        }

        String urlVerificada = verificarUrlQr(contenidoQr);
        return controlAccesoDAO.registrarIngreso(
                urlVerificada, idConcierto, idUsuario);
    }

    private String verificarUrlQr(String contenidoQr) {
        if (contenidoQr == null || contenidoQr.isBlank()) {
            throw new IllegalArgumentException("El QR esta vacio.");
        }
        String texto = contenidoQr.trim();
        final URI uri;
        try {
            uri = new URI(texto);
        } catch (URISyntaxException ex) {
            throw new IllegalArgumentException(
                    "El QR no contiene una URL valida de SVB-GUA.", ex);
        }

        if (!"https".equalsIgnoreCase(uri.getScheme())
                || !HOST_PERMITIDO.equalsIgnoreCase(uri.getHost())
                || uri.getPort() != -1
                || uri.getRawUserInfo() != null
                || uri.getRawQuery() != null
                || uri.getRawFragment() != null
                || uri.getRawPath() == null
                || !uri.getRawPath().matches("/b/[0-9a-f]{32}")) {
            throw new IllegalArgumentException(
                    "QR no reconocido: se esperaba un enlace de boleto SVB-GUA.");
        }
        return texto;
    }
}
