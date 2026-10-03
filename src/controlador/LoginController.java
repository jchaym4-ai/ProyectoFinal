package controlador;

import javax.swing.JOptionPane;
import modelo.Usuario;
import servicio.AuthService;
import vista.LoginView;
import vista.MenuAdministradorView;

public class LoginController {

    private final LoginView vista;
    private final AuthService authService;

    public LoginController(LoginView vista) {

        this.vista = vista;
        this.authService = new AuthService();

        this.vista.getBtnIniciarSesion().addActionListener(e -> {
            iniciarSesion();
        });
    }

    private void iniciarSesion() {

        String nombreUsuario = vista.getUsuario();
        String contrasena = vista.getContrasena();

        if (nombreUsuario.isEmpty() || contrasena.isEmpty()) {

            JOptionPane.showMessageDialog(
                    vista,
                    "Debe ingresar usuario y contraseña."
            );

            return;
        }

        Usuario usuario =
                authService.iniciarSesion(
                        nombreUsuario,
                        contrasena
                );

        if (usuario == null) {

            JOptionPane.showMessageDialog(
                    vista,
                    "Usuario o contraseña incorrectos."
            );

            vista.limpiarContrasena();
            return;
        }

        JOptionPane.showMessageDialog(
                vista,
                "Bienvenido " + usuario.getNombreCompleto()
        );

        if (usuario.getRol().equals("administrador")) {

            MenuAdministradorView menu =
                    new MenuAdministradorView(usuario);

            new MenuAdministradorController(menu);

            menu.setLocationRelativeTo(null);
            menu.setVisible(true);

            vista.dispose();

        } else if (usuario.getRol().equals("vendedor")) {

            JOptionPane.showMessageDialog(
                    vista,
                    "Acceso de vendedor"
            );
        }
    }
}