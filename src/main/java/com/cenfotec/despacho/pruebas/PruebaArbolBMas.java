package com.cenfotec.despacho.pruebas;

import com.cenfotec.despacho.estructuras.ArbolBMas;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.TreeMap;

/** Compara el árbol B+ contra un TreeMap con varios órdenes y operaciones aleatorias. */
public class PruebaArbolBMas {

    public static void main(String[] args) {
        for (int orden : new int[]{3, 4, 5, 6, 8}) {
            probarOrden(orden);
        }
        System.out.println("PRUEBA SUPERADA: el árbol B+ coincide con la referencia en todos los órdenes.");
    }

    private static void probarOrden(int orden) {
        ArbolBMas<Integer, String> arbol = new ArbolBMas<>(orden);
        TreeMap<Integer, String> referencia = new TreeMap<>();
        Random azar = new Random(100 + orden);

        for (int i = 0; i < 3000; i++) {
            int k = azar.nextInt(1500);
            boolean nueva = arbol.insertar(k, "v" + i);
            verificar(nueva == !referencia.containsKey(k), orden, "insertar nueva/duplicada " + k);
            referencia.put(k, "v" + i);
            if (i % 50 == 0) verificar(arbol.esValido(), orden, "inválido tras insertar " + i);
        }
        verificar(arbol.esValido(), orden, "inválido tras las inserciones");
        verificar(arbol.tamano() == referencia.size(), orden, "tamaño tras insertar");

        for (var e : referencia.entrySet()) {
            verificar(e.getValue().equals(arbol.buscar(e.getKey())), orden, "buscar " + e.getKey());
        }
        verificar(arbol.buscar(-5) == null, orden, "clave inexistente");

        for (int p = 0; p < 200; p++) {
            int a = azar.nextInt(1600) - 50;
            int b = a + azar.nextInt(400);
            List<String> esperado = new ArrayList<>(referencia.subMap(a, true, b, true).values());
            verificar(esperado.equals(arbol.buscarRango(a, b)), orden, "rango " + a + ".." + b);
        }
        verificar(new ArrayList<>(referencia.values()).equals(arbol.todos()), orden, "recorrido completo");
        int alturaLlena = arbol.altura();

        List<Integer> claves = new ArrayList<>(referencia.keySet());
        java.util.Collections.shuffle(claves, azar);
        int borradas = 0;
        for (int k : claves) {
            verificar(arbol.eliminar(k), orden, "eliminar existente " + k);
            referencia.remove(k);
            verificar(!arbol.eliminar(k), orden, "eliminar dos veces " + k);
            borradas++;
            if (borradas % 25 == 0) {
                verificar(arbol.esValido(), orden, "inválido tras borrar " + borradas);
                verificar(arbol.tamano() == referencia.size(), orden, "tamaño tras borrar");
            }
            if (borradas == claves.size() / 2) {
                for (var e : referencia.entrySet()) {
                    verificar(e.getValue().equals(arbol.buscar(e.getKey())), orden, "buscar a mitad de borrados");
                }
            }
        }
        verificar(arbol.estaVacio() && arbol.esValido(), orden, "debe quedar vacío y válido");
        verificar(arbol.altura() == 1, orden, "un árbol vacío vuelve a altura 1");

        arbol.insertar(1, "a");                                  // sigue siendo usable tras vaciarse
        verificar("a".equals(arbol.buscar(1)) && arbol.esValido(), orden, "reutilizable tras vaciarse");

        System.out.println("Orden " + orden + ": OK (altura máxima " + alturaLlena
                + ", " + borradas + " borrados)");
    }

    private static void verificar(boolean condicion, int orden, String mensaje) {
        if (!condicion) throw new IllegalStateException("FALLÓ (orden " + orden + "): " + mensaje);
    }
}
