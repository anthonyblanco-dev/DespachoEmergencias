package com.cenfotec.despacho.mensajeria;

import com.cenfotec.despacho.modelo.Incidente;
import com.cenfotec.despacho.modelo.Llamada;
import com.cenfotec.despacho.modelo.Unidad;

/**
 * Mensajes que viajan por el {@link BusMensajes}. Todos son inmutables y llevan copias de los
 * objetos del modelo, así el componente que los recibe puede procesarlos más tarde sin riesgo.
 */
public sealed interface Mensaje {

    /** Texto corto para el registro de eventos. */
    String resumen();

    record LlamadaRecibida(Llamada llamada) implements Mensaje {
        @Override public String resumen() { return "Llamada #" + llamada.getId() + " recibida de " + llamada.getTelefono(); }
    }

    record LlamadaAtendida(Llamada llamada) implements Mensaje {
        @Override public String resumen() { return "Llamada #" + llamada.getId() + " atendida (ya es incidente)"; }
    }

    record IncidenteCreado(Incidente incidente) implements Mensaje {
        @Override public String resumen() {
            return "Incidente #" + incidente.getId() + " creado (" + incidente.getTipo() + ", P" + incidente.getPrioridad() + ")";
        }
    }

    record IncidenteActualizado(Incidente incidente) implements Mensaje {
        @Override public String resumen() {
            String unidad = incidente.getPlacaAsignada() == null ? "" : ", unidad " + incidente.getPlacaAsignada();
            return "Incidente #" + incidente.getId() + " actualizado (P" + incidente.getPrioridad()
                    + ", " + incidente.getEstado() + unidad + ")";
        }
    }

    record IncidenteCerrado(Incidente incidente) implements Mensaje {
        @Override public String resumen() { return "Incidente #" + incidente.getId() + " cerrado y enviado al historial"; }
    }

    record UnidadActualizada(Unidad unidad) implements Mensaje {
        @Override public String resumen() { return "Unidad " + unidad.getPlaca() + " ahora " + unidad.getEstado(); }
    }

    record DespachoFallido(int idIncidente, String motivo) implements Mensaje {
        @Override public String resumen() { return "Despacho automático del #" + idIncidente + " no fue posible: " + motivo; }
    }
}
