package com.cenfotec.despacho.servicio;

import com.cenfotec.despacho.datos.DatosIniciales;
import com.cenfotec.despacho.estructuras.ArbolBMas;
import com.cenfotec.despacho.estructuras.ArbolRojoNegro;
import com.cenfotec.despacho.estructuras.ColaLlamadas;
import com.cenfotec.despacho.estructuras.Grafo;
import com.cenfotec.despacho.estructuras.TablaHashDinamica;
import com.cenfotec.despacho.mensajeria.Mensaje;
import com.cenfotec.despacho.mensajeria.Mensaje.IncidenteActualizado;
import com.cenfotec.despacho.mensajeria.Mensaje.IncidenteCerrado;
import com.cenfotec.despacho.mensajeria.Mensaje.IncidenteCreado;
import com.cenfotec.despacho.mensajeria.Mensaje.LlamadaAtendida;
import com.cenfotec.despacho.mensajeria.Mensaje.LlamadaRecibida;
import com.cenfotec.despacho.mensajeria.Mensaje.UnidadActualizada;
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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Consumer;

/**
 * Lógica del sistema de despacho. Combina las estructuras del curso:
 * árbol Rojo-Negro (incidentes activos por prioridad), tabla hash dinámica (búsqueda por id y por placa),
 * árbol B+ (historial de incidentes cerrados), grafo (mapa y rutas) y cola (llamadas entrantes).
 *
 * <p>Cada cambio importante se anuncia por el publicador (el {@code BusMensajes}) para que otros
 * componentes reaccionen sin que este servicio los conozca. Los métodos públicos son
 * {@code synchronized} porque los usan a la vez la interfaz y los hilos de los componentes.</p>
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
    private Consumer<Mensaje> publicador = m -> { };
    private int siguienteIdIncidente = 1;
    private int siguienteIdLlamada = 1;

    private static String ahora() {
        return LocalDateTime.now().withNano(0).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }

    /** Quién recibe los anuncios de cambios (normalmente {@code bus::publicar}). */
    public synchronized void setPublicador(Consumer<Mensaje> publicador) {
        this.publicador = publicador == null ? m -> { } : publicador;
    }

    private void publicar(Mensaje mensaje) {
        publicador.accept(mensaje);
    }

    // ----- Carga de datos -----

    public synchronized void cargar(DatosIniciales datos) {
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
                siguienteIdLlamada = Math.max(siguienteIdLlamada, l.getId() + 1);
                if (!l.isAtendida()) cola.encolar(l);
            }
        }
        reconciliar();
    }

    /**
     * Deja consistentes los datos recién cargados: un incidente ASIGNADO debe tener una unidad
     * OCUPADA, y una unidad OCUPADA debe estar atendiendo un incidente. Corrige lo que no cuadre
     * (por ejemplo si la app se cerró en medio de un guardado).
     */
    private void reconciliar() {
        Set<String> ocupadasPorIncidente = new HashSet<>();
        for (Incidente i : activos.inOrden()) {
            if (!"ASIGNADO".equals(i.getEstado())) continue;
            Unidad u = i.getPlacaAsignada() == null ? null : unidadesPorPlaca.buscar(i.getPlacaAsignada());
            if (u == null) {
                i.setEstado("ACTIVO");
                i.setPlacaAsignada(null);
            } else {
                u.setEstado("OCUPADA");
                ocupadasPorIncidente.add(u.getPlaca());
            }
        }
        for (Unidad u : unidades) {
            if ("OCUPADA".equals(u.getEstado()) && !ocupadasPorIncidente.contains(u.getPlaca())) {
                u.setEstado("DISPONIBLE");
            }
        }
    }

    /** Construye el grafo de la ciudad. Siempre viene del archivo local (es configuración fija). */
    public synchronized void cargarMapa(DatosIniciales datos) {
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

    public synchronized boolean registrarIncidente(Incidente incidente) {
        if (indicePorId.contiene(incidente.getId())) return false;
        if (!activos.insertar(incidente)) return false;
        indicePorId.insertar(incidente.getId(), incidente);
        siguienteIdIncidente = Math.max(siguienteIdIncidente, incidente.getId() + 1);
        return true;
    }

    public synchronized Incidente crearIncidente(String tipo, String descripcion, int prioridad, int idInterseccion) {
        Incidente i = new Incidente(siguienteIdIncidente, tipo, descripcion, prioridad,
                ahora(), "ACTIVO", idInterseccion);
        registrarIncidente(i);
        publicar(new IncidenteCreado(i.copia()));
        return i;
    }

    public synchronized Incidente masUrgente() { return activos.minimo(); }

    public synchronized Incidente buscarIncidente(int id) {
        Incidente referencia = indicePorId.buscar(id);
        return referencia == null ? null : activos.buscar(referencia);   // búsqueda en el árbol
    }

    public synchronized boolean reclasificar(int id, int nuevaPrioridad) {
        Incidente i = indicePorId.buscar(id);
        if (i == null) return false;
        activos.eliminar(i);              // sale con su clave anterior
        i.setPrioridad(nuevaPrioridad);
        activos.insertar(i);              // entra con la clave nueva
        publicar(new IncidenteActualizado(i.copia()));
        return true;
    }

    /** Cierra el incidente: sale del árbol activo, libera su unidad y pasa al historial (B+). */
    public synchronized Incidente cerrarIncidente(int id) {
        Incidente i = indicePorId.eliminar(id);
        if (i == null) return null;
        activos.eliminar(i);
        Unidad liberada = liberarUnidad(i);
        i.setEstado("CERRADO");
        historial.insertar(new ClaveHistorial(i.getFechaHora(), i.getId()), i);
        if (liberada != null) publicar(new UnidadActualizada(liberada.copia()));
        publicar(new IncidenteCerrado(i.copia()));
        return i;
    }

    public synchronized List<Incidente> listarActivos() { return activos.inOrden(); }

    public synchronized void mostrarArbol() { activos.imprimirArbol(); }

    public synchronized boolean arbolValido() { return activos.esValido(); }

    public synchronized int cantidadActivos() { return activos.tamano(); }

    public synchronized int alturaArbol() { return activos.altura(); }

    public synchronized ArbolRojoNegro.NodoVista<Incidente> vistaArbol() { return activos.vista(); }

    // ----- Despacho de unidades (grafo) -----

    /** Tipo de unidad que atiende un tipo de incidente, o null si no hay regla. */
    public static String tipoUnidadPara(String tipoIncidente) {
        return UNIDAD_POR_INCIDENTE.get(tipoIncidente);
    }

    /**
     * Asigna al incidente la unidad disponible del tipo adecuado que esté más cerca
     * (menor tiempo de viaje por Dijkstra). Marca la unidad OCUPADA y el incidente ASIGNADO.
     */
    public synchronized ResultadoDespacho despachar(int idIncidente) {
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
        incidente.setPlacaAsignada(mejor.getPlaca());
        publicar(new UnidadActualizada(mejor.copia()));
        publicar(new IncidenteActualizado(incidente.copia()));
        return new ResultadoDespacho(true, "Unidad " + mejor.getPlaca() + " despachada.",
                incidente, mejor, ruta, mejorTiempo);
    }

    /** Deja libre la unidad del incidente (si tenía una) y la devuelve; null si no había nada que liberar. */
    private Unidad liberarUnidad(Incidente incidente) {
        String placa = incidente.getPlacaAsignada();
        if (placa == null) return null;
        Unidad u = unidadesPorPlaca.buscar(placa);
        if (u == null || !"OCUPADA".equals(u.getEstado())) return null;
        u.setEstado("DISPONIBLE");
        return u;
    }

    /** Placa de la unidad que atiende un incidente activo, o null. */
    public synchronized String unidadAsignada(int idIncidente) {
        Incidente i = indicePorId.buscar(idIncidente);
        return i == null || !"ASIGNADO".equals(i.getEstado()) ? null : i.getPlacaAsignada();
    }

    // ----- Historial (árbol B+) -----

    /** Incidentes cerrados cuya fecha de registro está entre desde y hasta (ISO: 2026-11-10 o 2026-11-10T08:30:00). */
    public synchronized List<Incidente> consultarHistorial(String desde, String hasta) {
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

    public synchronized List<Incidente> listarHistorial() { return historial.todos(); }

    public synchronized int cantidadHistorial() { return historial.tamano(); }

    public synchronized void mostrarHistorial() { historial.imprimirArbol(); }

    public synchronized boolean historialValido() { return historial.esValido(); }

    public synchronized int alturaHistorial() { return historial.altura(); }

    public synchronized int ordenHistorial() { return historial.orden(); }

    public synchronized ArbolBMas.NodoVista<ClaveHistorial> vistaHistorial() { return historial.vista(); }

    // ----- Índices (tabla hash dinámica) -----

    public synchronized void mostrarIndice() { indicePorId.imprimirDistribucion(); }

    public synchronized List<List<Integer>> distribucionIndice() { return indicePorId.distribucion(); }

    public synchronized int capacidadIndice() { return indicePorId.capacidad(); }

    public synchronized int elementosIndice() { return indicePorId.tamano(); }

    public synchronized double factorCargaIndice() { return indicePorId.factorCarga(); }

    public synchronized int redimensionamientosIndice() { return indicePorId.redimensionamientos(); }

    // ----- Llamadas (cola) -----

    public synchronized Llamada recibirLlamada(String telefono, String ubicacion) {
        Llamada l = new Llamada(siguienteIdLlamada++, telefono, ahora(), ubicacion);
        cola.encolar(l);
        publicar(new LlamadaRecibida(l.copia()));
        return l;
    }

    public synchronized Incidente atenderSiguienteLlamada(String tipo, int prioridad, int idInterseccion) {
        Llamada l = cola.desencolar();
        if (l == null) return null;
        l.marcarAtendida();
        publicar(new LlamadaAtendida(l.copia()));
        String descripcion = "Llamada de " + l.getTelefono() + ": " + l.getUbicacionReferida();
        return crearIncidente(tipo, descripcion, prioridad, idInterseccion);
    }

    public synchronized int llamadasPendientes() { return cola.tamano(); }

    /** Próxima llamada de la cola sin sacarla (null si no hay). */
    public synchronized Llamada verSiguienteLlamada() { return cola.ver(); }

    // ----- Unidades y mapa -----

    /** Copia de la lista de unidades (los objetos son los reales: su estado se ve al instante). */
    public synchronized List<Unidad> getUnidades() { return new ArrayList<>(unidades); }

    public synchronized Unidad buscarUnidad(String placa) { return unidadesPorPlaca.buscar(placa); }

    public Grafo getMapa() { return mapa; }

    public Map<Integer, Interseccion> getIntersecciones() { return intersecciones; }
}
