package util;

import org.mindrot.jbcrypt.BCrypt;

public class PasswordUtil {

    private PasswordUtil() {
    }

    public static String encriptar(String contrasena) {
        return BCrypt.hashpw(
                contrasena,
                BCrypt.gensalt(12)
        );
    }

    public static boolean verificar(
            String contrasena,
            String hash
    ) {

        if (contrasena == null || hash == null) {
            return false;
        }   

        return BCrypt.checkpw(contrasena, hash);
    }
}