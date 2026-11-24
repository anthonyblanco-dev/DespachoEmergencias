package com.cenfotec.despacho.pruebas;

import com.cenfotec.despacho.estructuras.TablaHashDinamica;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class PruebaTablaHash {

    public static void main(String[] args) {
        TablaHashDinamica<Integer, String> tabla = new TablaHashDinamica<>();
        Map<Integer, String> referencia = new HashMap<>();
        Random azar = new Random(7);
        int capacidadInicial = tabla.capacidad();

        for (int i = 0; i < 5000; i++) {
            int k = azar.nextInt(3000);
            boolean nueva = tabla.insertar(k, "v" + i);
            verificar(nueva == !referencia.containsKey(k), "insertar debe indicar si la clave era nueva");
            referencia.put(k, "v" + i);
            verificar(tabla.factorCarga() <= TablaHashDinamica.FACTOR_MAXIMO, "factor de carga excedido");
        }
        verificar(tabla.tamano() == referencia.size(), "tamaño distinto al de referencia");
        verificar(tabla.capacidad() > capacidadInicial, "la tabla debe haber crecido");
        verificar(tabla.redimensionamientos() > 0, "debe haber redimensionamientos");

        for (Map.Entry<Integer, String> e : referencia.entrySet()) {
            verificar(e.getValue().equals(tabla.buscar(e.getKey())), "búsqueda incorrecta de " + e.getKey());
        }
        verificar(tabla.buscar(-1) == null, "clave inexistente debe dar null");

        int borradas = 0;
        for (int k = 0; k < 3000; k += 2) {
            String quitado = tabla.eliminar(k);
            verificar((quitado != null) == referencia.containsKey(k), "eliminar devolvió un valor incorrecto");
            if (referencia.remove(k) != null) borradas++;
        }
        verificar(tabla.tamano() == referencia.size(), "tamaño tras borrar");
        for (int k = 0; k < 3000; k += 2) {
            verificar(!tabla.contiene(k), "quedó una clave borrada: " + k);
        }

        System.out.println("Capacidad inicial: " + capacidadInicial + " | final: " + tabla.capacidad()
                + " | redimensionamientos: " + tabla.redimensionamientos()
                + " | elementos: " + tabla.tamano() + " | borrados: " + borradas);
        System.out.printf("Factor de carga final: %.2f%n", tabla.factorCarga());
        System.out.println("PRUEBA SUPERADA: la tabla hash crece y se comporta como la referencia.");
    }

    private static void verificar(boolean condicion, String mensaje) {
        if (!condicion) throw new IllegalStateException("FALLÓ: " + mensaje);
    }
}
