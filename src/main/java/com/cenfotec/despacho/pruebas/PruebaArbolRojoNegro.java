package com.cenfotec.despacho.pruebas;

import com.cenfotec.despacho.estructuras.ArbolRojoNegro;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class PruebaArbolRojoNegro {

    public static void main(String[] args) {
        ArbolRojoNegro<Integer> arbol = new ArbolRojoNegro<>();
        Random azar = new Random(42);
        List<Integer> claves = new ArrayList<>();

        for (int i = 0; i < 2000; i++) {
            int k = azar.nextInt(100000);
            if (arbol.insertar(k)) claves.add(k);
            if (i % 100 == 0) verificar(arbol, "inserción " + i);
        }
        System.out.println("Insertados: " + arbol.tamano() + " | altura: " + arbol.altura());
        verificar(arbol, "fin de inserciones");

        for (int k : claves) {
            if (arbol.buscar(k) == null) throw new IllegalStateException("No se encontró " + k);
        }
        System.out.println("Búsqueda: todas las claves insertadas se encuentran.");

        int borradas = 0;
        for (int i = 0; i < claves.size(); i += 2) {
            if (!arbol.eliminar(claves.get(i))) throw new IllegalStateException("No se pudo borrar");
            borradas++;
            if (borradas % 100 == 0) verificar(arbol, "borrado " + borradas);
        }
        verificar(arbol, "fin de borrados");
        System.out.println("Borrados: " + borradas + " | quedan: " + arbol.tamano()
                + " | altura: " + arbol.altura());

        for (int i = 0; i < claves.size(); i += 2) {
            if (arbol.buscar(claves.get(i)) != null) throw new IllegalStateException("Quedó una clave borrada");
        }
        System.out.println("PRUEBA SUPERADA: el árbol se mantuvo válido en todo momento.");
    }

    private static void verificar(ArbolRojoNegro<Integer> arbol, String momento) {
        if (!arbol.esValido()) {
            throw new IllegalStateException("Árbol inválido en: " + momento);
        }
    }
}
