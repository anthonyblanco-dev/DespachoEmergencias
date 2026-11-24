package com.cenfotec.despacho.servicio;

import com.cenfotec.despacho.datos.DatosIniciales;
import com.cenfotec.despacho.estructuras.ArbolBMas;
import com.cenfotec.despacho.estructuras.ArbolRojoNegro;
import com.cenfotec.despacho.estructuras.ColaLlamadas;
import com.cenfotec.despacho.estructuras.Grafo;
import com.cenfotec.despacho.estructuras.TablaHashDinamica;
import com.cenfotec.despacho.modelo.Calle;
import com.cenfotec.despacho.modelo.ClaveHistorial;
import com.cenfotec.despacho.modelo.Incidente;
import com.cenfotec.despacho.modelo.Interseccion;
import com.cenfotec.despacho.modelo.Llamada;
import com.cenfotec.despacho.modelo.Unidad;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Lógica del sistema de despacho. Combina las estructuras del curso:
 * árbol Rojo-Negro (incidentes activos por prioridad), tabla hash dinámica (búsqueda por id y por placa),
 * árbol B+ (historial de incidentes cerrados), grafo (mapa y rutas) y cola (llamadas entrantes).
 */
public class ServicioDespacho {

    /** Qué tipo de unidad atiende cada tipo de incidente. */
    private static final Map<String, String> UNIDAD_POR_INCIDENTE = Map.of(
            "INCENDIO", "BOMBEROS",
            "MEDICA", "AMBULANCIA",
            "ACCIDENTE", "AMBULANCIA",
            "ROBO", "POLICIA",
            "RUIDO", "POLICIA");

    /** Resultado de intentar despachar una unidad. Si no hubo éxito, mensaje explica por qué. */
    public record ResultadoDespacho(boolean exito, String mensaje, Incidente incidente,
                                    Unidad unidad, List<Integer> ruta, int minutos) {
        static ResultadoDespacho fallo(String mensaje) {
            return new ResultadoDespacho(false, mensaje, null, null, List.of(), 0);
        }
    }

    private final ColaLlamadas cola = new ColaLlamadas(8);
    private final ArbolRojoNegro<Incidente> activos = new ArbolRojoNegro<>();
    private final TablaHashDinamica<Integer, Incidente> indicePorId = new TablaHashDinamica<>();
    private final TablaHashDinamica<String, Unidad> unidadesPorPlaca = new TablaHashDinamica<>();
    private final ArbolBMas<ClaveHistorial, Incidente> historial = new ArbolBMas<>(4);
    private final List<Unidad> unidades = new ArrayList<>();
    private final Grafo mapa = new Grafo();
    private final Map<Integer, Interseccion> intersecciones = new TreeMap<>();
    private final Map<Integer, String> asignaciones = new HashMap<>();     // id incidente -> placa
    private int siguienteIdIncidente = 1;
    private int siguienteIdLlamada = 1;

    private static String ahora() {
        return LocalDateTime.now().withNano(0).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }

    // ----- Carga de datos -----

    public void cargar(DatosIniciales datos) {
        if (datos.getUnidades() != null) {
            for (Unidad u : datos.getUnidades()) {
                unidades.add(u);
                unidadesPorPlaca.insertar(u.getPlaca(), u);
            }
        }
        if (datos.getIncidentes() != null) {
            for (Incidente i : datos.getIncidentes()) {
                siguienteIdIncidente = Math.max(siguienteIdIncidente, i.getId() + 1);
                if ("CERRADO".equals(i.getEstado())) {
                    historial.insertar(new ClaveHistorial(i.getFechaHora(), i.getId()), i);
                } else {
                    registrarIncidente(i);
                }
            }
        }
        if (datos.getLlamadas() != null) {
            for (Llamada l : datos.getLlamadas()) {
                cola.encolar(l);
                siguienteIdLlamada = Math.max(siguienteIdLlamada, l.getId() + 1);
            }
        }
    }

    /** Construye el grafo de la ciudad. Siempre viene del archivo local (es configuración fija). */
    public void cargarMapa(DatosIniciales datos) {
        if (datos.getIntersecciones() != null) {
            for (Interseccion i : datos.getIntersecciones()) {
                intersecciones.put(i.getId(), i);
                mapa.agregarVertice(i.getId());
            }
        }
        if (datos.getCalles() != null) {
            for (Calle c : datos.getCalles()) {
                mapa.agregarArista(c.getOrigen(), c.getDestino(), c.getTiempo());
            }
        }
    }

    // ----- Incidentes (árbol Rojo-Negro + tabla hash) -----

    public boolean registrarIncidente(Incidente incidente) {
        if (indicePorId.contiene(incidente.getId())) return false;
        if (!activos.insertar(incidente)) return false;
        indicePorId.insertar(incidente.getId(), incidente);
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
        Incidente referencia = indicePorId.buscar(id);
        return referencia == null ? null : activos.buscar(referencia);   // búsqueda en el árbol
    }

    public boolean reclasificar(int id, int nuevaPrioridad) {
        Incidente i = indicePorId.buscar(id);
        if (i == null) return false;
        activos.eliminar(i);              // sale con su clave anterior
        i.setPrioridad(nuevaPrioridad);
        activos.insertar(i);              // entra con la clave nueva
        return true;
    }

    /** Cierra el incidente: sale del árbol activo, libera su unidad y pasa al historial (B+). */
    public Incidente cerrarIncidente(int id) {
        Incidente i = indicePorId.eliminar(id);
        if (i == null) return null;
        activos.eliminar(i);
        liberarUnidad(id);
        i.setEstado("CERRADO");
        historial.insertar(new ClaveHistorial(i.getFechaHora(), i.getId()), i);
        return i;
    }

    public List<Incidente> listarActivos() { return activos.inOrden(); }

    public void mostrarArbol() { activos.imprimirArbol(); }

    public boolean arbolValido() { return activos.esValido(); }

    public int cantidadActivos() { return activos.tamano(); }

    public int alturaArbol() { return activos.altura(); }

    public ArbolRojoNegro.NodoVista<Incidente> vistaArbol() { return activos.vista(); }

    // ----- Despacho de unidades (grafo) -----

    /** Tipo de unidad que atiende un tipo de incidente, o null si no hay regla. */
    public static String tipoUnidadPara(String tipoIncidente) {
        return UNIDAD_POR_INCIDENTE.get(tipoIncidente);
    }

    /**
     * Asigna al incidente la unidad disponible del tipo adecuado que esté más cerca
     * (menor tiempo de viaje por Dijkstra). Marca la unidad OCUPADA y el incidente ASIGNADO.
     */
    public ResultadoDespacho despachar(int idIncidente) {
        Incidente incidente = indicePorId.buscar(idIncidente);
        if (incidente == null) return ResultadoDespacho.fallo("No existe el incidente " + idIncidente + ".");
        if ("ASIGNADO".equals(incidente.getEstado())) {
            return ResultadoDespacho.fallo("El incidente " + idIncidente + " ya tiene una unidad asignada.");
        }
        String tipoUnidad = UNIDAD_POR_INCIDENTE.get(incidente.getTipo());
        if (tipoUnidad == null) {
            return ResultadoDespacho.fallo("No hay un tipo de unidad definido para incidentes " + incidente.getTipo() + ".");
        }
        if (!mapa.contieneVertice(incidente.getIdInterseccion())) {
            return ResultadoDespacho.fallo("El incidente no está en una intersección válida del mapa ("
                    + incidente.getIdInterseccion() + ").");
        }

        Grafo.ResultadoDijkstra desdeIncidente = mapa.dijkstra(incidente.getIdInterseccion());
        Unidad mejor = null;
        int mejorTiempo = Grafo.INFINITO;
        for (Unidad u : unidades) {
            if (!u.getTipo().equals(tipoUnidad) || !"DISPONIBLE".equals(u.getEstado())) continue;
            if (!mapa.contieneVertice(u.getIdInterseccion())) continue;
            int tiempo = desdeIncidente.distanciaA(u.getIdInterseccion());   // el mapa es no dirigido
            if (tiempo < mejorTiempo) {
                mejorTiempo = tiempo;
                mejor = u;
            }
        }
        if (mejor == null) {
            return ResultadoDespacho.fallo("No hay unidades " + tipoUnidad + " disponibles con ruta al incidente.");
        }

        List<Integer> ruta = new ArrayList<>(desdeIncidente.rutaA(mejor.getIdInterseccion()));
        Collections.reverse(ruta);                                            // de la unidad al incidente
        mejor.setEstado("OCUPADA");
        incidente.setEstado("ASIGNADO");
        asignaciones.put(idIncidente, mejor.getPlaca());
        return new ResultadoDespacho(true, "Unidad " + mejor.getPlaca() + " despachada.",
                incidente, mejor, ruta, mejorTiempo);
    }

    private void liberarUnidad(int idIncidente) {
        String placa = asignaciones.remove(idIncidente);
        if (placa == null) return;
        Unidad u = unidadesPorPlaca.buscar(placa);
        if (u != null) u.setEstado("DISPONIBLE");
    }

    /** Placa de la unidad asignada a un incidente, o null. */
    public String unidadAsignada(int idIncidente) { return asignaciones.get(idIncidente); }

    // ----- Historial (árbol B+) -----

    /** Incidentes cerrados cuya fecha de registro está entre desde y hasta (ISO: 2026-11-10 o 2026-11-10T08:30:00). */
    public List<Incidente> consultarHistorial(String desde, String hasta) {
        ClaveHistorial inicio = new ClaveHistorial(normalizarFecha(desde, true), Integer.MIN_VALUE);
        ClaveHistorial fin = new ClaveHistorial(normalizarFecha(hasta, false), Integer.MAX_VALUE);
        return historial.buscarRango(inicio, fin);
    }

    private static String normalizarFecha(String texto, boolean inicioDelDia) {
        String t = texto.trim();
        try {
            if (t.length() <= 10) {
                LocalDate.parse(t);
                return t + (inicioDelDia ? "T00:00:00" : "T23:59:59");
            }
            LocalDateTime.parse(t);
            return t;
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Fecha inválida: \"" + texto
                    + "\". Usa 2026-11-10 o 2026-11-10T08:30:00.");
        }
    }

    public List<Incidente> listarHistorial() { return historial.todos(); }

    public int cantidadHistorial() { return historial.tamano(); }

    public void mostrarHistorial() { historial.imprimirArbol(); }

    public boolean historialValido() { return historial.esValido(); }

    public int alturaHistorial() { return historial.altura(); }

    public int ordenHistorial() { return historial.orden(); }

    public ArbolBMas.NodoVista<ClaveHistorial> vistaHistorial() { return historial.vista(); }

    // ----- Índices (tabla hash dinámica) -----

    public void mostrarIndice() { indicePorId.imprimirDistribucion(); }

    public List<List<Integer>> distribucionIndice() { return indicePorId.distribucion(); }

    public int capacidadIndice() { return indicePorId.capacidad(); }

    public int elementosIndice() { return indicePorId.tamano(); }

    public double factorCargaIndice() { return indicePorId.factorCarga(); }

    public int redimensionamientosIndice() { return indicePorId.redimensionamientos(); }

    // ----- Llamadas (cola) -----

    public Llamada recibirLlamada(String telefono, String ubicacion) {
        Llamada l = new Llamada(siguienteIdLlamada++, telefono, ahora(), ubicacion);
        cola.encolar(l);
        return l;
    }

    public Incidente atenderSiguienteLlamada(String tipo, int prioridad, int idInterseccion) {
        Llamada l = cola.desencolar();
        if (l == null) return null;
        String descripcion = "Llamada de " + l.getTelefono() + ": " + l.getUbicacionReferida();
        return crearIncidente(tipo, descripcion, prioridad, idInterseccion);
    }

    public int llamadasPendientes() { return cola.tamano(); }

    /** Próxima llamada de la cola sin sacarla (null si no hay). */
    public Llamada verSiguienteLlamada() { return cola.ver(); }

    // ----- Unidades y mapa -----

    public List<Unidad> getUnidades() { return unidades; }

    public Unidad buscarUnidad(String placa) { return unidadesPorPlaca.buscar(placa); }

    public Grafo getMapa() { return mapa; }

    public Map<Integer, Interseccion> getIntersecciones() { return intersecciones; }
}
