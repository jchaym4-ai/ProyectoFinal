package modelo;

public class Artista {

    private int idArtista;
    private String nombreArtistico;
    private String generoMusical;
    private String paisOrigen;

    public Artista() {
    }

    public Artista(int idArtista, String nombreArtistico,
                   String generoMusical, String paisOrigen) {
        this.idArtista = idArtista;
        this.nombreArtistico = nombreArtistico;
        this.generoMusical = generoMusical;
        this.paisOrigen = paisOrigen;
    }

    public int getIdArtista() {
        return idArtista;
    }

    public void setIdArtista(int idArtista) {
        this.idArtista = idArtista;
    }

    public String getNombreArtistico() {
        return nombreArtistico;
    }

    public void setNombreArtistico(String nombreArtistico) {
        this.nombreArtistico = nombreArtistico;
    }

    public String getGeneroMusical() {
        return generoMusical;
    }

    public void setGeneroMusical(String generoMusical) {
        this.generoMusical = generoMusical;
    }

    public String getPaisOrigen() {
        return paisOrigen;
    }

    public void setPaisOrigen(String paisOrigen) {
        this.paisOrigen = paisOrigen;
    }
}