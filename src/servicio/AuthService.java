package servicio;

import dao.UsuarioDAO;
import modelo.Usuario;
import util.PasswordUtil;

public class AuthService {

    private final UsuarioDAO usuarioDAO;

    public AuthService() {
        this.usuarioDAO = new UsuarioDAO();
    }

    public Usuario iniciarSesion(String nombreUsuario, String contrasena) {

        if (nombreUsuario == null || nombreUsuario.trim().isEmpty()) {
            return null;
        }

        if (contrasena == null || contrasena.isEmpty()) {
            return null;
        }

        Usuario usuario =
                usuarioDAO.buscarPorNombreUsuario(nombreUsuario.trim());

        if (usuario == null) {
            return null;
        }

        boolean contrasenaCorrecta =
                PasswordUtil.verificar(
                        contrasena,
                        usuario.getContrasenaHash()
                );

        if (!contrasenaCorrecta) {
            return null;
        }

        return usuario;
    }
}