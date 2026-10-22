package dao;

import coneccion.CreateConnection;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Reportes administrativos, SOLO LECTURA. Las vistas se crean en Paso 75.1.
 * No se suman importes desde joins de varias tablas: las vistas ya contienen
 * una fila por boleto, una por concierto o una por localidad/inventario.
 */
public final class ReportesDAO {

    private final CreateConnection conexiones = new CreateConnection();

    public record Concierto(
            int idConcierto, String nombre, String fecha, String recinto,
            long capacidad, long disponibles, long emitidos, long sinUtilizar,
            long ingresos, long cancelados, long ventas,
            BigDecimal historicos, BigDecimal devoluciones, BigDecimal netos) { }

    public record Localidad(
            int idInventario, int idConcierto, String concierto, String nombre,
            String tipoVenta, BigDecimal precio, long capacidad, long disponibles,
            long emitidos, long sinUtilizar, long ingresos, long cancelados,
            BigDecimal historicos, BigDecimal devoluciones, BigDecimal netos) { }

    public record Datos(List<Concierto> conciertos, List<Localidad> localidades,
                        long ventasDistintasGlobales) { }

    public record Resumen(long conciertos, long capacidad, long disponibles,
                         long emitidos, long sinUtilizar, long ingresos,
                         long cancelados, long ventas,
                         BigDecimal historicos, BigDecimal devoluciones,
                         BigDecimal netos) { }

    /** Recupera todos los conciertos, incluso los que aún no tienen ventas. */
    public Datos cargar() throws SQLException {
        List<Concierto> conciertos = new ArrayList<>();
        List<Localidad> localidades = new ArrayList<>();
        long ventasDistintas = 0;

        try (Connection conexion = conexiones.getConection()) {
            if (conexion == null) {
                throw new SQLException("No se pudo abrir la conexión con Neon.");
            }
            try (PreparedStatement ps = conexion.prepareStatement("""
                    SELECT id_concierto, concierto, fecha_concierto, recinto,
                           capacidad_total, disponibles, boletos_emitidos,
                           sin_utilizar, ingresos_registrados, boletos_cancelados,
                           ventas_con_boletos, ingresos_historicos,
                           devoluciones_simuladas, ingresos_netos_estimados
                    FROM public.vw_reporte_conciertos
                    ORDER BY fecha_concierto DESC NULLS LAST, id_concierto DESC
                    """);
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    conciertos.add(new Concierto(
                            rs.getInt("id_concierto"), texto(rs, "concierto"),
                            texto(rs, "fecha_concierto"), texto(rs, "recinto"),
                            rs.getLong("capacidad_total"), rs.getLong("disponibles"),
                            rs.getLong("boletos_emitidos"), rs.getLong("sin_utilizar"),
                            rs.getLong("ingresos_registrados"), rs.getLong("boletos_cancelados"),
                            rs.getLong("ventas_con_boletos"),
                            dinero(rs, "ingresos_historicos"),
                            dinero(rs, "devoluciones_simuladas"),
                            dinero(rs, "ingresos_netos_estimados")));
                }
            }
            try (PreparedStatement ps = conexion.prepareStatement("""
                    SELECT id_inventario, id_concierto, concierto, localidad,
                           tipo_venta, precio, cantidad_total,
                           cantidad_disponible, boletos_emitidos,
                           sin_utilizar, ingresos_registrados, cancelados,
                           ingresos_historicos, devoluciones_simuladas,
                           ingresos_netos_estimados
                    FROM public.vw_reporte_localidades
                    ORDER BY id_concierto, localidad, id_inventario
                    """);
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    localidades.add(new Localidad(
                            rs.getInt("id_inventario"), rs.getInt("id_concierto"),
                            texto(rs, "concierto"), texto(rs, "localidad"),
                            texto(rs, "tipo_venta"), dinero(rs, "precio"),
                            rs.getLong("cantidad_total"), rs.getLong("cantidad_disponible"),
                            rs.getLong("boletos_emitidos"), rs.getLong("sin_utilizar"),
                            rs.getLong("ingresos_registrados"), rs.getLong("cancelados"),
                            dinero(rs, "ingresos_historicos"),
                            dinero(rs, "devoluciones_simuladas"),
                            dinero(rs, "ingresos_netos_estimados")));
                }
            }
            // Importante: una venta que contenga entradas de dos conciertos
            // debe contar como UNA venta cuando el filtro está en "Todos".
            try (PreparedStatement ps = conexion.prepareStatement(
                    "SELECT COUNT(DISTINCT id_venta) FROM public.vw_reporte_boletos");
                 ResultSet rs = ps.executeQuery()) {
                if (rs.next()) ventasDistintas = rs.getLong(1);
            }
        }
        return new Datos(List.copyOf(conciertos), List.copyOf(localidades), ventasDistintas);
    }

    public static Resumen resumir(Datos datos, int idConcierto) {
        if (datos == null) {
            return new Resumen(0, 0, 0, 0, 0, 0, 0, 0,
                    BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
        }
        long cantidad = 0, capacidad = 0, disponibles = 0, emitidos = 0;
        long sinUtilizar = 0, ingresos = 0, cancelados = 0, ventas = 0;
        BigDecimal historicos = BigDecimal.ZERO;
        BigDecimal devoluciones = BigDecimal.ZERO;
        BigDecimal netos = BigDecimal.ZERO;
        for (Concierto c : datos.conciertos()) {
            if (idConcierto > 0 && c.idConcierto() != idConcierto) continue;
            cantidad++;
            capacidad += c.capacidad();
            disponibles += c.disponibles();
            emitidos += c.emitidos();
            sinUtilizar += c.sinUtilizar();
            ingresos += c.ingresos();
            cancelados += c.cancelados();
            ventas += c.ventas();
            historicos = historicos.add(c.historicos());
            devoluciones = devoluciones.add(c.devoluciones());
            netos = netos.add(c.netos());
        }
        if (idConcierto <= 0) ventas = datos.ventasDistintasGlobales();
        return new Resumen(cantidad, capacidad, disponibles, emitidos,
                sinUtilizar, ingresos, cancelados, ventas,
                historicos, devoluciones, netos);
    }

    private static String texto(ResultSet rs, String columna) throws SQLException {
        String valor = rs.getString(columna);
        return valor == null ? "" : valor;
    }

    private static BigDecimal dinero(ResultSet rs, String columna) throws SQLException {
        BigDecimal valor = rs.getBigDecimal(columna);
        return valor == null ? BigDecimal.ZERO : valor;
    }
}
