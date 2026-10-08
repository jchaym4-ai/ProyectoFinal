package TestPackages;
import dao.UsuarioDAO;
import modelo.Usuario;
import util.PasswordUtil;

public class CrearAdmin {

    public static void main(String[] args) {

        Usuario admin = new Usuario();

        admin.setNombreUsuario("daniel");
        admin.setNombreCompleto("Administrador SVB-GUA");
        admin.setRol("administrador");

        String hash = PasswordUtil.encriptar("1234");
        admin.setContrasenaHash(hash);

        UsuarioDAO usuarioDAO = new UsuarioDAO();

        if (usuarioDAO.insertar(admin)) {

            System.out.println("Administrador creado correctamente.");
            System.out.println("Usuario: daniel");
            System.out.println("Contraseña: 1234");
            System.out.println("ID: " + admin.getIdUsuario());

        } else {

            System.out.println("No se pudo crear el administrador.");
        }
    }
}