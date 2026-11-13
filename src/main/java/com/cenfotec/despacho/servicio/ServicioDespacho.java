package com.cenfotec.despacho.servicio;

import com.cenfotec.despacho.datos.DatosIniciales;
import com.cenfotec.despacho.estructuras.ArbolRojoNegro;
import com.cenfotec.despacho.estructuras.ColaLlamadas;
import com.cenfotec.despacho.modelo.Incidente;
import com.cenfotec.despacho.modelo.Llamada;
import com.cenfotec.despacho.modelo.Unidad;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ServicioDespacho {

    private final ColaLlamadas cola = new ColaLlamadas(8);
    private final ArbolRojoNegro<Incidente> activos = new ArbolRojoNegro<>();
    private final Map<Integer, Incidente> indicePorId = new HashMap<>();   // temporal (avance 3: hashing dinámico)
    private final List<Unidad> unidades = new ArrayList<>();
    private int siguienteIdIncidente = 1;
    private int siguienteIdLlamada = 1;

    private static String ahora() {
        return LocalDateTime.now().withNano(0).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }

    public void cargar(DatosIniciales datos) {
        if (datos.getUnidades() != null) unidades.addAll(datos.getUnidades());
        if (datos.getIncidentes() != null) {
            for (Incidente i : datos.getIncidentes()) registrarIncidente(i);
        }
        if (datos.getLlamadas() != null) {
            for (Llamada l : datos.getLlamadas()) {
                cola.encolar(l);
                siguienteIdLlamada = Math.max(siguienteIdLlamada, l.getId() + 1);
            }
        }
    }

    // ----- Incidentes (árbol Rojo-Negro) -----

    public boolean registrarIncidente(Incidente incidente) {
        if (indicePorId.containsKey(incidente.getId())) return false;
        if (!activos.insertar(incidente)) return false;
        indicePorId.put(incidente.getId(), incidente);
        siguienteIdIncidente = Math.max(siguienteIdIncidente, incidente.getId() + 1);
        return true;
    }

    public Incidente crearIncidente(String tipo, String descripcion, int prioridad, int idInterseccion) {
        Incidente i = new Incidente(siguienteIdIncidente, tipo, descripcion, prioridad,
                ahora(), "ACTIVO", idInterseccion);
        registrarIncidente(i);
        return i;
    }

    public Incidente masUrgente() { return activos.minimo(); }

    public Incidente buscarIncidente(int id) {
        Incidente referencia = indicePorId.get(id);
        return referencia == null ? null : activos.buscar(referencia);   // búsqueda en el árbol
    }

    public boolean reclasificar(int id, int nuevaPrioridad) {
        Incidente i = indicePorId.get(id);
        if (i == null) return false;
        activos.eliminar(i);              // sale con su clave anterior
        i.setPrioridad(nuevaPrioridad);
        activos.insertar(i);              // entra con la clave nueva
        return true;
    }

    public Incidente cerrarIncidente(int id) {
        Incidente i = indicePorId.remove(id);
        if (i == null) return null;
        activos.eliminar(i);
        i.setEstado("CERRADO");
        return i;
    }

    public List<Incidente> listarActivos() { return activos.inOrden(); }

    public void mostrarArbol() { activos.imprimirArbol(); }

    public boolean arbolValido() { return activos.esValido(); }

    public int cantidadActivos() { return activos.tamano(); }

    public int alturaArbol() { return activos.altura(); }

    // ----- Llamadas (cola) -----

    public Llamada recibirLlamada(String telefono, String ubicacion) {
        Llamada l = new Llamada(siguienteIdLlamada++, telefono, ahora(), ubicacion);
        cola.encolar(l);
        return l;
    }

    public Incidente atenderSiguienteLlamada(String tipo, int prioridad) {
        Llamada l = cola.desencolar();
        if (l == null) return null;
        String descripcion = "Llamada de " + l.getTelefono() + ": " + l.getUbicacionReferida();
        return crearIncidente(tipo, descripcion, prioridad, 0);
    }

    public int llamadasPendientes() { return cola.tamano(); }

    public List<Unidad> getUnidades() { return unidades; }
}
