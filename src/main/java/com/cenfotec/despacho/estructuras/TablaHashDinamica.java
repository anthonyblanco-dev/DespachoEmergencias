package com.cenfotec.despacho.estructuras;

import java.util.ArrayList;
import java.util.List;

/**
 * Tabla hash dinámica con encadenamiento separado.
 * Cuando el factor de carga supera {@value #FACTOR_MAXIMO} la capacidad se duplica
 * y todas las entradas se redistribuyen (rehash).
 */
public class TablaHashDinamica<K, V> {

    public static final double FACTOR_MAXIMO = 0.75;
    private static final int CAPACIDAD_MINIMA = 4;

    private static class Entrada<K, V> {
        final K clave;
        V valor;
        Entrada<K, V> siguiente;

        Entrada(K clave, V valor, Entrada<K, V> siguiente) {
            this.clave = clave;
            this.valor = valor;
            this.siguiente = siguiente;
        }
    }

    private Entrada<K, V>[] cubetas;
    private int tamano = 0;
    private int redimensionamientos = 0;

    public TablaHashDinamica() {
        this(CAPACIDAD_MINIMA);
    }

    public TablaHashDinamica(int capacidadInicial) {
        cubetas = crearArreglo(Math.max(CAPACIDAD_MINIMA, capacidadInicial));
    }

    @SuppressWarnings("unchecked")
    private Entrada<K, V>[] crearArreglo(int capacidad) {
        return (Entrada<K, V>[]) new Entrada[capacidad];
    }

    // ---------- Operaciones ----------

    /** Inserta o reemplaza. Devuelve true si la clave era nueva. */
    public boolean insertar(K clave, V valor) {
        int i = indice(clave, cubetas.length);
        for (Entrada<K, V> e = cubetas[i]; e != null; e = e.siguiente) {
            if (e.clave.equals(clave)) {
                e.valor = valor;
                return false;
            }
        }
        cubetas[i] = new Entrada<>(clave, valor, cubetas[i]);
        tamano++;
        if ((double) tamano / cubetas.length > FACTOR_MAXIMO) redimensionar();
        return true;
    }

    public V buscar(K clave) {
        for (Entrada<K, V> e = cubetas[indice(clave, cubetas.length)]; e != null; e = e.siguiente) {
            if (e.clave.equals(clave)) return e.valor;
        }
        return null;
    }

    public boolean contiene(K clave) {
        for (Entrada<K, V> e = cubetas[indice(clave, cubetas.length)]; e != null; e = e.siguiente) {
            if (e.clave.equals(clave)) return true;
        }
        return false;
    }

    /** Elimina la clave y devuelve el valor que tenía (null si no existía). */
    public V eliminar(K clave) {
        int i = indice(clave, cubetas.length);
        Entrada<K, V> anterior = null;
        for (Entrada<K, V> e = cubetas[i]; e != null; anterior = e, e = e.siguiente) {
            if (e.clave.equals(clave)) {
                if (anterior == null) cubetas[i] = e.siguiente;
                else anterior.siguiente = e.siguiente;
                tamano--;
                return e.valor;
            }
        }
        return null;
    }

    // ---------- Consultas ----------

    public int tamano() { return tamano; }

    public boolean estaVacia() { return tamano == 0; }

    public int capacidad() { return cubetas.length; }

    public double factorCarga() { return (double) tamano / cubetas.length; }

    public int redimensionamientos() { return redimensionamientos; }

    public List<V> valores() {
        List<V> resultado = new ArrayList<>();
        for (Entrada<K, V> cabeza : cubetas) {
            for (Entrada<K, V> e = cabeza; e != null; e = e.siguiente) resultado.add(e.valor);
        }
        return resultado;
    }

    /** Vista de solo lectura para la interfaz: las claves de cada cubeta, en orden de cadena. */
    public List<List<K>> distribucion() {
        List<List<K>> resultado = new ArrayList<>(cubetas.length);
        for (Entrada<K, V> cabeza : cubetas) {
            List<K> cadena = new ArrayList<>();
            for (Entrada<K, V> e = cabeza; e != null; e = e.siguiente) cadena.add(e.clave);
            resultado.add(cadena);
        }
        return resultado;
    }

    /** Muestra cada cubeta con su cadena de claves. */
    public void imprimirDistribucion() {
        System.out.printf("Capacidad: %d | Elementos: %d | Factor de carga: %.2f | Redimensionamientos: %d%n",
                cubetas.length, tamano, factorCarga(), redimensionamientos);
        for (int i = 0; i < cubetas.length; i++) {
            StringBuilder sb = new StringBuilder();
            sb.append(String.format("[%2d] ", i));
            for (Entrada<K, V> e = cubetas[i]; e != null; e = e.siguiente) {
                sb.append(e.clave);
                if (e.siguiente != null) sb.append(" -> ");
            }
            System.out.println(sb);
        }
    }

    // ---------- Internos ----------

    private int indice(K clave, int capacidad) {
        int h = clave.hashCode();
        h ^= (h >>> 16);                          // mezcla bits altos para repartir mejor
        return (h & 0x7fffffff) % capacidad;
    }

    private void redimensionar() {
        Entrada<K, V>[] viejas = cubetas;
        cubetas = crearArreglo(viejas.length * 2);
        for (Entrada<K, V> cabeza : viejas) {
            Entrada<K, V> e = cabeza;
            while (e != null) {
                Entrada<K, V> siguiente = e.siguiente;
                int i = indice(e.clave, cubetas.length);
                e.siguiente = cubetas[i];
                cubetas[i] = e;
                e = siguiente;
            }
        }
        redimensionamientos++;
    }
}
