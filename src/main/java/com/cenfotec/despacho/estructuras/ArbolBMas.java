package com.cenfotec.despacho.estructuras;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Árbol B+ en memoria. Todos los datos viven en las hojas, que están enlazadas entre sí,
 * lo que permite consultas por rango recorriendo hojas consecutivas.
 *
 * <p>El {@code orden} es el máximo de hijos de un nodo interno; cada nodo guarda como máximo
 * {@code orden - 1} claves y, salvo la raíz, al menos {@code (orden - 1) / 2}.
 * En un nodo interno, {@code claves[i]} separa los hijos {@code i} (menores) e {@code i + 1} (mayores o iguales).</p>
 */
public class ArbolBMas<K extends Comparable<K>, V> {

    // ---------- Nodos ----------

    private abstract class Nodo {
        final List<K> claves = new ArrayList<>();

        abstract boolean esHoja();
    }

    private class Hoja extends Nodo {
        final List<V> valores = new ArrayList<>();
        Hoja siguiente;

        @Override
        boolean esHoja() { return true; }
    }

    private class Interno extends Nodo {
        final List<Nodo> hijos = new ArrayList<>();

        @Override
        boolean esHoja() { return false; }
    }

    /** Copia de solo lectura de un nodo, para que la interfaz pueda dibujar el árbol. */
    public record NodoVista<K>(List<K> claves, boolean hoja, List<NodoVista<K>> hijos) { }

    // ---------- Estado ----------

    private final int orden;
    private final int maxClaves;
    private final int minClaves;
    private Nodo raiz;
    private int tamano = 0;

    // Resultado de una división de nodo durante la inserción (null si no hubo)
    private K claveDivision;
    private Nodo nodoDivision;

    public ArbolBMas(int orden) {
        if (orden < 3) throw new IllegalArgumentException("El orden mínimo es 3");
        this.orden = orden;
        this.maxClaves = orden - 1;
        this.minClaves = (orden - 1) / 2;
        this.raiz = new Hoja();
    }

    // ---------- Consultas ----------

    public int orden() { return orden; }

    public int tamano() { return tamano; }

    public boolean estaVacio() { return tamano == 0; }

    public V buscar(K clave) {
        Hoja hoja = buscarHoja(clave);
        int pos = Collections.binarySearch(hoja.claves, clave);
        return pos >= 0 ? hoja.valores.get(pos) : null;
    }

    /** Valores con clave entre desde y hasta (ambas incluidas), en orden ascendente. */
    public List<V> buscarRango(K desde, K hasta) {
        List<V> resultado = new ArrayList<>();
        if (desde.compareTo(hasta) > 0) return resultado;
        Hoja hoja = buscarHoja(desde);
        while (hoja != null) {
            for (int i = 0; i < hoja.claves.size(); i++) {
                K k = hoja.claves.get(i);
                if (k.compareTo(desde) < 0) continue;
                if (k.compareTo(hasta) > 0) return resultado;
                resultado.add(hoja.valores.get(i));
            }
            hoja = hoja.siguiente;
        }
        return resultado;
    }

    /** Todos los valores en orden de clave, recorriendo las hojas enlazadas. */
    public List<V> todos() {
        List<V> resultado = new ArrayList<>();
        for (Hoja h = primeraHoja(); h != null; h = h.siguiente) resultado.addAll(h.valores);
        return resultado;
    }

    public int altura() {
        int niveles = 1;
        Nodo n = raiz;
        while (!n.esHoja()) {
            n = ((Interno) n).hijos.get(0);
            niveles++;
        }
        return niveles;
    }

    /** Copia de solo lectura de toda la estructura. */
    public NodoVista<K> vista() {
        return copiar(raiz);
    }

    private NodoVista<K> copiar(Nodo n) {
        List<NodoVista<K>> hijos = new ArrayList<>();
        if (!n.esHoja()) {
            for (Nodo h : ((Interno) n).hijos) hijos.add(copiar(h));
        }
        return new NodoVista<>(new ArrayList<>(n.claves), n.esHoja(), hijos);
    }

    // ---------- Inserción ----------

    /** Inserta la pareja. Si la clave ya existía reemplaza el valor y devuelve false. */
    public boolean insertar(K clave, V valor) {
        claveDivision = null;
        nodoDivision = null;
        boolean esNueva = insertar(raiz, clave, valor);
        if (nodoDivision != null) {                       // la raíz se dividió: el árbol crece un nivel
            Interno nuevaRaiz = new Interno();
            nuevaRaiz.claves.add(claveDivision);
            nuevaRaiz.hijos.add(raiz);
            nuevaRaiz.hijos.add(nodoDivision);
            raiz = nuevaRaiz;
            claveDivision = null;
            nodoDivision = null;
        }
        if (esNueva) tamano++;
        return esNueva;
    }

    private boolean insertar(Nodo n, K clave, V valor) {
        if (n.esHoja()) {
            Hoja hoja = (Hoja) n;
            int pos = Collections.binarySearch(hoja.claves, clave);
            if (pos >= 0) {
                hoja.valores.set(pos, valor);
                return false;
            }
            pos = -pos - 1;
            hoja.claves.add(pos, clave);
            hoja.valores.add(pos, valor);
            if (hoja.claves.size() > maxClaves) dividirHoja(hoja);
            return true;
        }

        Interno interno = (Interno) n;
        int i = indiceHijo(interno, clave);
        boolean esNueva = insertar(interno.hijos.get(i), clave, valor);
        if (nodoDivision != null) {                       // un hijo se dividió: subir su clave
            interno.claves.add(i, claveDivision);
            interno.hijos.add(i + 1, nodoDivision);
            nodoDivision = null;
            claveDivision = null;
            if (interno.claves.size() > maxClaves) dividirInterno(interno);
        }
        return esNueva;
    }

    private void dividirHoja(Hoja hoja) {
        int medio = hoja.claves.size() / 2;
        Hoja derecha = new Hoja();
        derecha.claves.addAll(hoja.claves.subList(medio, hoja.claves.size()));
        derecha.valores.addAll(hoja.valores.subList(medio, hoja.valores.size()));
        hoja.claves.subList(medio, hoja.claves.size()).clear();
        hoja.valores.subList(medio, hoja.valores.size()).clear();
        derecha.siguiente = hoja.siguiente;
        hoja.siguiente = derecha;
        claveDivision = derecha.claves.get(0);            // la clave se copia hacia arriba
        nodoDivision = derecha;
    }

    private void dividirInterno(Interno interno) {
        int medio = interno.claves.size() / 2;
        K promovida = interno.claves.get(medio);          // la clave del medio sube (no se copia)
        Interno derecha = new Interno();
        derecha.claves.addAll(interno.claves.subList(medio + 1, interno.claves.size()));
        derecha.hijos.addAll(interno.hijos.subList(medio + 1, interno.hijos.size()));
        interno.claves.subList(medio, interno.claves.size()).clear();
        interno.hijos.subList(medio + 1, interno.hijos.size()).clear();
        claveDivision = promovida;
        nodoDivision = derecha;
    }

    // ---------- Borrado ----------

    public boolean eliminar(K clave) {
        if (!eliminar(raiz, clave)) return false;
        tamano--;
        if (!raiz.esHoja() && raiz.claves.isEmpty()) {    // la raíz quedó sin claves: el árbol baja un nivel
            raiz = ((Interno) raiz).hijos.get(0);
        }
        return true;
    }

    private boolean eliminar(Nodo n, K clave) {
        if (n.esHoja()) {
            Hoja hoja = (Hoja) n;
            int pos = Collections.binarySearch(hoja.claves, clave);
            if (pos < 0) return false;
            hoja.claves.remove(pos);
            hoja.valores.remove(pos);
            return true;
        }
        Interno interno = (Interno) n;
        int i = indiceHijo(interno, clave);
        Nodo hijo = interno.hijos.get(i);
        boolean eliminada = eliminar(hijo, clave);
        if (eliminada && hijo.claves.size() < minClaves) arreglarHijo(interno, i);
        return eliminada;
    }

    /** El hijo i quedó con pocas claves: pedir prestado a un hermano o fusionar con él. */
    private void arreglarHijo(Interno padre, int i) {
        Nodo izq = i > 0 ? padre.hijos.get(i - 1) : null;
        Nodo der = i < padre.hijos.size() - 1 ? padre.hijos.get(i + 1) : null;
        if (izq != null && izq.claves.size() > minClaves) {
            prestarDeIzquierda(padre, i);
        } else if (der != null && der.claves.size() > minClaves) {
            prestarDeDerecha(padre, i);
        } else if (izq != null) {
            fusionar(padre, i - 1);
        } else {
            fusionar(padre, i);
        }
    }

    private void prestarDeIzquierda(Interno padre, int i) {
        Nodo hijo = padre.hijos.get(i);
        Nodo hermano = padre.hijos.get(i - 1);
        if (hijo.esHoja()) {
            Hoja h = (Hoja) hijo;
            Hoja l = (Hoja) hermano;
            int ultimo = l.claves.size() - 1;
            h.claves.add(0, l.claves.remove(ultimo));
            h.valores.add(0, l.valores.remove(ultimo));
            padre.claves.set(i - 1, h.claves.get(0));
        } else {
            Interno h = (Interno) hijo;
            Interno l = (Interno) hermano;
            h.claves.add(0, padre.claves.get(i - 1));
            h.hijos.add(0, l.hijos.remove(l.hijos.size() - 1));
            padre.claves.set(i - 1, l.claves.remove(l.claves.size() - 1));
        }
    }

    private void prestarDeDerecha(Interno padre, int i) {
        Nodo hijo = padre.hijos.get(i);
        Nodo hermano = padre.hijos.get(i + 1);
        if (hijo.esHoja()) {
            Hoja h = (Hoja) hijo;
            Hoja r = (Hoja) hermano;
            h.claves.add(r.claves.remove(0));
            h.valores.add(r.valores.remove(0));
            padre.claves.set(i, r.claves.get(0));
        } else {
            Interno h = (Interno) hijo;
            Interno r = (Interno) hermano;
            h.claves.add(padre.claves.get(i));
            h.hijos.add(r.hijos.remove(0));
            padre.claves.set(i, r.claves.remove(0));
        }
    }

    /** Une los hijos i e i+1 del padre en el hijo i. */
    private void fusionar(Interno padre, int i) {
        Nodo izq = padre.hijos.get(i);
        Nodo der = padre.hijos.get(i + 1);
        if (izq.esHoja()) {
            Hoja l = (Hoja) izq;
            Hoja r = (Hoja) der;
            l.claves.addAll(r.claves);
            l.valores.addAll(r.valores);
            l.siguiente = r.siguiente;
        } else {
            Interno l = (Interno) izq;
            Interno r = (Interno) der;
            l.claves.add(padre.claves.get(i));             // la clave separadora baja
            l.claves.addAll(r.claves);
            l.hijos.addAll(r.hijos);
        }
        padre.claves.remove(i);
        padre.hijos.remove(i + 1);
    }

    // ---------- Auxiliares ----------

    /** Posición del hijo por el que hay que bajar para encontrar la clave. */
    private int indiceHijo(Interno n, K clave) {
        int i = 0;
        while (i < n.claves.size() && clave.compareTo(n.claves.get(i)) >= 0) i++;
        return i;
    }

    private Hoja buscarHoja(K clave) {
        Nodo n = raiz;
        while (!n.esHoja()) {
            Interno interno = (Interno) n;
            n = interno.hijos.get(indiceHijo(interno, clave));
        }
        return (Hoja) n;
    }

    private Hoja primeraHoja() {
        Nodo n = raiz;
        while (!n.esHoja()) n = ((Interno) n).hijos.get(0);
        return (Hoja) n;
    }

    // ---------- Verificación ----------

    /** Comprueba límites de claves, orden, separadores, misma profundidad y cadena de hojas. */
    public boolean esValido() {
        if (validar(raiz, null, null, true) == -1) return false;
        int contadas = 0;
        K anterior = null;
        for (Hoja h = primeraHoja(); h != null; h = h.siguiente) {
            for (K k : h.claves) {
                if (anterior != null && anterior.compareTo(k) >= 0) return false;
                anterior = k;
                contadas++;
            }
        }
        return contadas == tamano;
    }

    /** Devuelve la altura del subárbol o -1 si algo está mal. lo es inclusivo y hi exclusivo. */
    private int validar(Nodo n, K lo, K hi, boolean esRaiz) {
        int cantidad = n.claves.size();
        if (cantidad > maxClaves) return -1;
        if (!esRaiz && cantidad < minClaves) return -1;
        if (esRaiz && !n.esHoja() && cantidad == 0) return -1;
        for (int i = 0; i < cantidad; i++) {
            K k = n.claves.get(i);
            if (i > 0 && n.claves.get(i - 1).compareTo(k) >= 0) return -1;
            if (lo != null && k.compareTo(lo) < 0) return -1;
            if (hi != null && k.compareTo(hi) >= 0) return -1;
        }
        if (n.esHoja()) {
            return ((Hoja) n).valores.size() == cantidad ? 1 : -1;
        }
        Interno interno = (Interno) n;
        if (interno.hijos.size() != cantidad + 1) return -1;
        int alturaHijos = -2;
        for (int i = 0; i <= cantidad; i++) {
            K desde = (i == 0) ? lo : n.claves.get(i - 1);
            K hasta = (i == cantidad) ? hi : n.claves.get(i);
            int h = validar(interno.hijos.get(i), desde, hasta, false);
            if (h == -1) return -1;
            if (alturaHijos == -2) alturaHijos = h;
            else if (alturaHijos != h) return -1;
        }
        return alturaHijos + 1;
    }

    // ---------- Visualización textual ----------

    /** Imprime el árbol nivel por nivel y luego la cadena de hojas. */
    public void imprimirArbol() {
        if (tamano == 0) {
            System.out.println("(árbol B+ vacío)");
            return;
        }
        List<Nodo> nivel = new ArrayList<>();
        nivel.add(raiz);
        int numero = 0;
        while (!nivel.isEmpty()) {
            StringBuilder sb = new StringBuilder("Nivel " + numero + ": ");
            List<Nodo> siguiente = new ArrayList<>();
            for (Nodo n : nivel) {
                sb.append(n.claves).append(' ');
                if (!n.esHoja()) siguiente.addAll(((Interno) n).hijos);
            }
            System.out.println(sb);
            nivel = siguiente;
            numero++;
        }
        StringBuilder cadena = new StringBuilder("Hojas enlazadas: ");
        for (Hoja h = primeraHoja(); h != null; h = h.siguiente) {
            cadena.append(h.claves);
            if (h.siguiente != null) cadena.append(" -> ");
        }
        System.out.println(cadena);
    }
}
