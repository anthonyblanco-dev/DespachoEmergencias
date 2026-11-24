package com.cenfotec.despacho.pruebas;

import com.cenfotec.despacho.estructuras.Grafo;

import java.util.List;

public class PruebaGrafo {

    public static void main(String[] args) {
        Grafo g = new Grafo();
        for (int i = 1; i <= 5; i++) g.agregarVertice(i);   // el 5 queda aislado
        g.agregarArista(1, 2, 4);
        g.agregarArista(1, 3, 1);
        g.agregarArista(3, 2, 2);
        g.agregarArista(2, 4, 1);
        g.agregarArista(3, 4, 5);

        Grafo.ResultadoDijkstra r = g.dijkstra(1);
        verificar(r.distanciaA(2) == 3, "distancia 1->2 debe ser 3");
        verificar(r.distanciaA(4) == 4, "distancia 1->4 debe ser 4");
        verificar(r.rutaA(4).equals(List.of(1, 3, 2, 4)), "ruta 1->4 debe ser 1-3-2-4");
        verificar(!r.alcanzable(5), "el vértice 5 no debe ser alcanzable");
        verificar(r.rutaA(5).isEmpty(), "ruta a vértice aislado debe ser vacía");

        verificar(g.bfs(1).equals(List.of(1, 2, 3, 4)), "orden BFS desde 1");
        verificar(g.dfs(1).equals(List.of(1, 2, 3, 4)), "orden DFS desde 1");
        verificar(g.conexiones().size() == 5, "deben existir 5 calles distintas");

        g.imprimirListaAdyacencia();
        System.out.println("PRUEBA SUPERADA: el grafo y Dijkstra funcionan.");
    }

    private static void verificar(boolean condicion, String mensaje) {
        if (!condicion) throw new IllegalStateException("FALLÓ: " + mensaje);
    }
}
