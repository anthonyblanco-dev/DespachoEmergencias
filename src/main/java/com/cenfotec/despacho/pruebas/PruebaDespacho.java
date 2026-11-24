package com.cenfotec.despacho.pruebas;

import com.cenfotec.despacho.datos.CargadorArchivo;
import com.cenfotec.despacho.datos.DatosIniciales;
import com.cenfotec.despacho.modelo.Incidente;
import com.cenfotec.despacho.servicio.ServicioDespacho;
import com.cenfotec.despacho.servicio.ServicioDespacho.ResultadoDespacho;

import java.util.List;

/** Prueba el flujo completo del servicio con los datos locales (no usa la nube). */
public class PruebaDespacho {

    public static void main(String[] args) throws Exception {
        DatosIniciales datos = CargadorArchivo.cargar("/datos_iniciales.json");
        ServicioDespacho s = new ServicioDespacho();
        s.cargarMapa(datos);
        s.cargar(datos);

        verificar(s.cantidadActivos() == 10, "deben cargarse 10 incidentes activos");
        verificar(s.getMapa().cantidadVertices() == 15, "el mapa debe tener 15 intersecciones");
        verificar(s.masUrgente().getId() == 2, "el más urgente es el #2 (P1 más antiguo)");

        // Incendio en la intersección 9: el único camión está en la 7 (9-8-7 = 3 + 2 minutos)
        ResultadoDespacho r = s.despachar(2);
        verificar(r.exito(), "despachar #2 debe funcionar: " + r.mensaje());
        verificar(r.unidad().getPlaca().equals("BOM-201"), "debe ir BOM-201");
        verificar(r.minutos() == 5, "el viaje a la 9 debe durar 5 min, fue " + r.minutos());
        verificar(r.ruta().equals(List.of(7, 8, 9)), "la ruta debe ser 7-8-9, fue " + r.ruta());
        verificar(r.unidad().getEstado().equals("OCUPADA"), "la unidad queda OCUPADA");
        verificar(s.buscarIncidente(2).getEstado().equals("ASIGNADO"), "el incidente queda ASIGNADO");
        verificar(!s.despachar(2).exito(), "no se puede despachar dos veces el mismo incidente");

        // Paro cardíaco en la 11: AMB-102 (en la 12) está a 5 min, AMB-101 (en la 3) mucho más lejos
        r = s.despachar(4);
        verificar(r.exito() && r.unidad().getPlaca().equals("AMB-102"), "debe ir la ambulancia más cercana");
        verificar(r.minutos() == 5, "11->12 son 5 min, fue " + r.minutos());

        // Persona inconsciente en la 14 usa la otra ambulancia; después ya no queda ninguna
        r = s.despachar(6);
        verificar(r.exito() && r.unidad().getPlaca().equals("AMB-101"), "debe ir AMB-101");
        r = s.despachar(1);
        verificar(!r.exito() && r.mensaje().contains("disponibles"), "sin ambulancias debe fallar: " + r.mensaje());
        verificar(!s.despachar(99).exito(), "incidente inexistente debe fallar");

        // Cerrar libera la unidad y manda el incidente al historial (B+)
        Incidente cerrado = s.cerrarIncidente(4);
        verificar(cerrado != null && cerrado.getEstado().equals("CERRADO"), "debe cerrarse el #4");
        verificar(s.buscarUnidad("AMB-102").getEstado().equals("DISPONIBLE"), "AMB-102 vuelve a estar disponible");
        verificar(s.buscarIncidente(4) == null, "el #4 ya no está activo");
        verificar(s.cantidadActivos() == 9, "quedan 9 activos");
        verificar(s.cantidadHistorial() == 1 && s.historialValido(), "el historial tiene 1 incidente");
        verificar(s.cerrarIncidente(4) == null, "no se puede cerrar dos veces");
        r = s.despachar(1);
        verificar(r.exito() && r.unidad().getPlaca().equals("AMB-102"), "la unidad liberada atiende el #1");

        // Consultas por rango en el B+
        verificar(s.consultarHistorial("2026-11-10", "2026-11-10").size() == 1, "rango del día 10");
        verificar(s.consultarHistorial("2026-11-11", "2026-11-12").isEmpty(), "rango sin incidentes");
        verificar(s.consultarHistorial("2026-11-10T08:26:00", "2026-11-10T09:00:00").isEmpty(),
                "el #4 es de las 08:25, queda fuera");
        boolean fallo = false;
        try {
            s.consultarHistorial("ayer", "hoy");
        } catch (IllegalArgumentException e) {
            fallo = true;
        }
        verificar(fallo, "una fecha inválida debe rechazarse");

        // Reclasificar mueve el nodo en el árbol Rojo-Negro
        verificar(s.reclasificar(5, 1), "reclasificar #5");
        verificar(s.buscarIncidente(5).getPrioridad() == 1, "el #5 ahora es P1");
        verificar(s.arbolValido(), "el árbol Rojo-Negro sigue válido");

        // Incidentes nuevos usan ids correlativos y quedan en el índice hash
        Incidente nuevo = s.crearIncidente("ROBO", "Prueba", 2, 3);
        verificar(nuevo.getId() == 11, "el siguiente id debe ser 11, fue " + nuevo.getId());
        verificar(s.buscarIncidente(11) != null, "el nuevo se encuentra por id");
        verificar(s.despachar(11).exito(), "la policía atiende el robo");

        System.out.println("PRUEBA SUPERADA: despacho, liberación e historial funcionan.");
    }

    private static void verificar(boolean condicion, String mensaje) {
        if (!condicion) throw new IllegalStateException("FALLÓ: " + mensaje);
    }
}
