package com.cenfotec.despacho.estructuras;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;
import java.util.TreeMap;

/**
 * Grafo no dirigido y ponderado (lista de adyacencia).
 * Los vértices son enteros (ids de intersección) y el peso es el tiempo en minutos.
 */
public class Grafo {

    public static final int INFINITO = Integer.MAX_VALUE;

    /** Arista saliente de un vértice. */
    public record Arista(int destino, int peso) { }

    /** Calle completa (se usa para dibujar el grafo sin repetir aristas). */
    public record Conexion(int origen, int destino, int peso) { }

    /** Resultado de Dijkstra desde un origen: distancias y ruta hacia cualquier destino. */
    public static class ResultadoDijkstra {
        private final int origen;
        private final Map<Integer, Integer> distancia;
        private final Map<Integer, Integer> previo;

        ResultadoDijkstra(int origen, Map<Integer, Integer> distancia, Map<Integer, Integer> previo) {
            this.origen = origen;
            this.distancia = distancia;
            this.previo = previo;
        }

        public int getOrigen() { return origen; }

        public int distanciaA(int destino) {
            return distancia.getOrDefault(destino, INFINITO);
        }

        public boolean alcanzable(int destino) {
            return distanciaA(destino) != INFINITO;
        }

        /** Ruta de vértices desde el origen hasta el destino (vacía si no hay camino). */
        public List<Integer> rutaA(int destino) {
            List<Integer> ruta = new ArrayList<>();
            if (!alcanzable(destino)) return ruta;
            for (Integer v = destino; v != null; v = previo.get(v)) {
                ruta.add(v);
            }
            Collections.reverse(ruta);
            return ruta;
        }
    }

    private final Map<Integer, List<Arista>> adyacencia = new TreeMap<>();

    // ---------- Construcción ----------

    public void agregarVertice(int id) {
        adyacencia.putIfAbsent(id, new ArrayList<>());
    }

    public void agregarArista(int a, int b, int peso) {
        if (!adyacencia.containsKey(a) || !adyacencia.containsKey(b)) {
            throw new IllegalArgumentException("Vértice inexistente: " + a + " o " + b);
        }
        if (peso < 0) {
            throw new IllegalArgumentException("El peso no puede ser negativo");
        }
        adyacencia.get(a).add(new Arista(b, peso));
        adyacencia.get(b).add(new Arista(a, peso));
    }

    // ---------- Consultas ----------

    public boolean contieneVertice(int id) { return adyacencia.containsKey(id); }

    public int cantidadVertices() { return adyacencia.size(); }

    public List<Integer> vertices() { return new ArrayList<>(adyacencia.keySet()); }

    public List<Arista> vecinos(int id) {
        List<Arista> lista = adyacencia.get(id);
        return lista == null ? List.of() : Collections.unmodifiableList(lista);
    }

    /** Cada calle una sola vez (origen menor que destino). Útil para dibujar. */
    public List<Conexion> conexiones() {
        List<Conexion> resultado = new ArrayList<>();
        for (Map.Entry<Integer, List<Arista>> e : adyacencia.entrySet()) {
            for (Arista a : e.getValue()) {
                if (e.getKey() < a.destino()) {
                    resultado.add(new Conexion(e.getKey(), a.destino(), a.peso()));
                }
            }
        }
        return resultado;
    }

    // ---------- Recorridos ----------

    public List<Integer> bfs(int origen) {
        List<Integer> orden = new ArrayList<>();
        if (!adyacencia.containsKey(origen)) return orden;
        Set<Integer> visitados = new HashSet<>();
        Queue<Integer> cola = new ArrayDeque<>();
        cola.add(origen);
        visitados.add(origen);
        while (!cola.isEmpty()) {
            int v = cola.poll();
            orden.add(v);
            for (Arista a : adyacencia.get(v)) {
                if (visitados.add(a.destino())) cola.add(a.destino());
            }
        }
        return orden;
    }

    public List<Integer> dfs(int origen) {
        List<Integer> orden = new ArrayList<>();
        if (!adyacencia.containsKey(origen)) return orden;
        dfs(origen, new HashSet<>(), orden);
        return orden;
    }

    private void dfs(int v, Set<Integer> visitados, List<Integer> orden) {
        visitados.add(v);
        orden.add(v);
        for (Arista a : adyacencia.get(v)) {
            if (!visitados.contains(a.destino())) dfs(a.destino(), visitados, orden);
        }
    }

    // ---------- Camino más corto ----------

    public ResultadoDijkstra dijkstra(int origen) {
        if (!adyacencia.containsKey(origen)) {
            throw new IllegalArgumentException("Vértice inexistente: " + origen);
        }
        Map<Integer, Integer> distancia = new HashMap<>();
        Map<Integer, Integer> previo = new HashMap<>();
        for (int v : adyacencia.keySet()) distancia.put(v, INFINITO);
        distancia.put(origen, 0);

        PriorityQueue<int[]> cola = new PriorityQueue<>((p, q) -> Integer.compare(p[1], q[1]));
        cola.add(new int[]{origen, 0});

        while (!cola.isEmpty()) {
            int[] actual = cola.poll();
            int u = actual[0];
            if (actual[1] > distancia.get(u)) continue;      // entrada vieja de la cola
            for (Arista a : adyacencia.get(u)) {
                int nueva = actual[1] + a.peso();
                if (nueva < distancia.get(a.destino())) {
                    distancia.put(a.destino(), nueva);
                    previo.put(a.destino(), u);
                    cola.add(new int[]{a.destino(), nueva});
                }
            }
        }
        return new ResultadoDijkstra(origen, distancia, previo);
    }

    // ---------- Visualización textual ----------

    public void imprimirListaAdyacencia() {
        for (Map.Entry<Integer, List<Arista>> e : adyacencia.entrySet()) {
            StringBuilder sb = new StringBuilder();
            sb.append(String.format("%2d -> ", e.getKey()));
            for (int i = 0; i < e.getValue().size(); i++) {
                Arista a = e.getValue().get(i);
                if (i > 0) sb.append(", ");
                sb.append(a.destino()).append("(").append(a.peso()).append("m)");
            }
            System.out.println(sb);
        }
    }
}
