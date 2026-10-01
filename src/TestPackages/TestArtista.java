package TestPackages;
        
import dao.ArtistaDAO;
import modelo.Artista;

public class TestArtista {

    public static void main(String[] args) {

        Artista artista = new Artista();

        artista.setNombreArtistico("Artista prueba");
        artista.setGeneroMusical("Pop");
        artista.setPaisOrigen("Guatemala");

        ArtistaDAO dao = new ArtistaDAO();

        if (dao.insertar(artista)) {
            System.out.println("Artista registrado correctamente");
            System.out.println("ID generado: " + artista.getIdArtista());
        } else {
            System.out.println("Error al registrar artista");
        }
    }
}