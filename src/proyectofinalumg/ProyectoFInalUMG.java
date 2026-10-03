package proyectofinalumg;

import controlador.LoginController;
import vista.LoginView;

public class ProyectoFInalUMG {

    public static void main(String[] args) {

        java.awt.EventQueue.invokeLater(() -> {

            LoginView login = new LoginView();

            new LoginController(login);

            login.setLocationRelativeTo(null);
            login.setVisible(true);
        });
    }
}