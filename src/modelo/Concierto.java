package modelo;

import java.time.LocalDateTime;
import java.time.LocalTime;

public class Concierto {

    private int idConcierto;
    private int idArtista;
    private String tituloEvento;
    private LocalDateTime fechaConcierto;
    private String recinto;
    private String estado;

    private String descripcion;
    private String informacionAdicional;
    private String imagenEvento;
    private LocalTime horaApertura;

    public Concierto() {
    }

    public Concierto(
            int idConcierto,
            int idArtista,
            String tituloEvento,
            LocalDateTime fechaConcierto,
            String recinto,
            String estado,
            String descripcion,
            String informacionAdicional,
            String imagenEvento,
            LocalTime horaApertura) {

        this.idConcierto = idConcierto;
        this.idArtista = idArtista;
        this.tituloEvento = tituloEvento;
        this.fechaConcierto = fechaConcierto;
        this.recinto = recinto;
        this.estado = estado;
        this.descripcion = descripcion;
        this.informacionAdicional = informacionAdicional;
        this.imagenEvento = imagenEvento;
        this.horaApertura = horaApertura;
    }

    public int getIdConcierto() {
        return idConcierto;
    }

    public void setIdConcierto(int idConcierto) {
        this.idConcierto = idConcierto;
    }

    public int getIdArtista() {
        return idArtista;
    }

    public void setIdArtista(int idArtista) {
        this.idArtista = idArtista;
    }

    public String getTituloEvento() {
        return tituloEvento;
    }

    public void setTituloEvento(String tituloEvento) {
        this.tituloEvento = tituloEvento;
    }

    public LocalDateTime getFechaConcierto() {
        return fechaConcierto;
    }

    public void setFechaConcierto(LocalDateTime fechaConcierto) {
        this.fechaConcierto = fechaConcierto;
    }

    public String getRecinto() {
        return recinto;
    }

    public void setRecinto(String recinto) {
        this.recinto = recinto;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getInformacionAdicional() {
        return informacionAdicional;
    }

    public void setInformacionAdicional(String informacionAdicional) {
        this.informacionAdicional = informacionAdicional;
    }

    public String getImagenEvento() {
        return imagenEvento;
    }

    public void setImagenEvento(String imagenEvento) {
        this.imagenEvento = imagenEvento;
    }

    public LocalTime getHoraApertura() {
        return horaApertura;
    }

    public void setHoraApertura(LocalTime horaApertura) {
        this.horaApertura = horaApertura;
    }
}