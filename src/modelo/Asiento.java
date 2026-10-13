
package modelo;

public class Asiento {

    private int idAsiento;
    private int idInventario;
    private String fila;
    private int numero;
    private String estado;

    public Asiento() {
    }

    public Asiento(
            int idAsiento,
            int idInventario,
            String fila,
            int numero,
            String estado) {

        this.idAsiento = idAsiento;
        this.idInventario = idInventario;
        this.fila = fila;
        this.numero = numero;
        this.estado = estado;
    }

    public int getIdAsiento() {
        return idAsiento;
    }

    public void setIdAsiento(int idAsiento) {
        this.idAsiento = idAsiento;
    }

    public int getIdInventario() {
        return idInventario;
    }

    public void setIdInventario(int idInventario) {
        this.idInventario = idInventario;
    }

    public String getFila() {
        return fila;
    }

    public void setFila(String fila) {
        this.fila = fila;
    }

    public int getNumero() {
        return numero;
    }

    public void setNumero(int numero) {
        this.numero = numero;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }
}
