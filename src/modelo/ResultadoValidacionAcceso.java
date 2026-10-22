package modelo;

/** Resultado de un intento de validacion de ingreso. */
public record ResultadoValidacionAcceso(
        String codigo,
        String mensaje,
        int idBoleto,
        int idVenta,
        int idConcierto,
        String concierto,
        String localidad,
        String asiento,
        String fechaIngreso) {

    public boolean autorizado() {
        return "AUTORIZADO".equals(codigo);
    }
}
