package com.cenfotec.despacho.estructuras;

import java.util.ArrayList;
import java.util.List;

public class ArbolRojoNegro<T extends Comparable<T>> {

    private static final boolean ROJO = true;
    private static final boolean NEGRO = false;

    private class Nodo {
        T dato;
        Nodo izq, der, padre;
        boolean color;

        Nodo(T dato, boolean color) {
            this.dato = dato;
            this.color = color;
        }
    }

    private final Nodo NIL = new Nodo(null, NEGRO);   // centinela: todas las hojas vacías
    private Nodo raiz;
    private int tamano = 0;

    public ArbolRojoNegro() {
        NIL.izq = NIL;
        NIL.der = NIL;
        NIL.padre = NIL;
        raiz = NIL;
    }

    // ---------- Consultas ----------

    public int tamano() { return tamano; }

    public boolean estaVacio() { return tamano == 0; }

    public T buscar(T clave) {
        Nodo n = buscarNodo(clave);
        return n == NIL ? null : n.dato;
    }

    public T minimo() {
        return raiz == NIL ? null : minimo(raiz).dato;
    }

    public List<T> inOrden() {
        List<T> resultado = new ArrayList<>();
        inOrden(raiz, resultado);
        return resultado;
    }

    public int altura() { return altura(raiz); }

    public void imprimirArbol() {
        if (raiz == NIL) {
            System.out.println("(árbol vacío)");
            return;
        }
        imprimir(raiz, 0);
    }

    /** Verifica las propiedades: raíz negra, sin rojo-rojo y misma altura negra. */
    public boolean esValido() {
        if (raiz == NIL) return true;
        if (raiz.color != NEGRO) return false;
        return alturaNegra(raiz) != -1;
    }

    // ---------- Inserción ----------

    public boolean insertar(T dato) {
        Nodo y = NIL;
        Nodo x = raiz;
        while (x != NIL) {
            y = x;
            int cmp = dato.compareTo(x.dato);
            if (cmp == 0) return false;               // duplicado
            x = (cmp < 0) ? x.izq : x.der;
        }
        Nodo z = new Nodo(dato, ROJO);
        z.izq = NIL;
        z.der = NIL;
        z.padre = y;
        if (y == NIL) raiz = z;
        else if (dato.compareTo(y.dato) < 0) y.izq = z;
        else y.der = z;
        tamano++;
        arreglarInsercion(z);
        return true;
    }

    private void arreglarInsercion(Nodo z) {
        while (z.padre.color == ROJO) {
            if (z.padre == z.padre.padre.izq) {
                Nodo tio = z.padre.padre.der;
                if (tio.color == ROJO) {
                    z.padre.color = NEGRO;
                    tio.color = NEGRO;
                    z.padre.padre.color = ROJO;
                    z = z.padre.padre;
                } else {
                    if (z == z.padre.der) {
                        z = z.padre;
                        rotarIzquierda(z);
                    }
                    z.padre.color = NEGRO;
                    z.padre.padre.color = ROJO;
                    rotarDerecha(z.padre.padre);
                }
            } else {
                Nodo tio = z.padre.padre.izq;
                if (tio.color == ROJO) {
                    z.padre.color = NEGRO;
                    tio.color = NEGRO;
                    z.padre.padre.color = ROJO;
                    z = z.padre.padre;
                } else {
                    if (z == z.padre.izq) {
                        z = z.padre;
                        rotarDerecha(z);
                    }
                    z.padre.color = NEGRO;
                    z.padre.padre.color = ROJO;
                    rotarIzquierda(z.padre.padre);
                }
            }
        }
        raiz.color = NEGRO;
    }

    // ---------- Borrado ----------

    public boolean eliminar(T dato) {
        Nodo z = buscarNodo(dato);
        if (z == NIL) return false;

        Nodo y = z;
        boolean colorOriginal = y.color;
        Nodo x;

        if (z.izq == NIL) {
            x = z.der;
            trasplantar(z, z.der);
        } else if (z.der == NIL) {
            x = z.izq;
            trasplantar(z, z.izq);
        } else {
            y = minimo(z.der);                         // sucesor in-orden
            colorOriginal = y.color;
            x = y.der;
            if (y.padre == z) {
                x.padre = y;
            } else {
                trasplantar(y, y.der);
                y.der = z.der;
                y.der.padre = y;
            }
            trasplantar(z, y);
            y.izq = z.izq;
            y.izq.padre = y;
            y.color = z.color;
        }
        tamano--;
        if (colorOriginal == NEGRO) arreglarEliminacion(x);
        return true;
    }

    private void arreglarEliminacion(Nodo x) {
        while (x != raiz && x.color == NEGRO) {
            if (x == x.padre.izq) {
                Nodo w = x.padre.der;
                if (w.color == ROJO) {
                    w.color = NEGRO;
                    x.padre.color = ROJO;
                    rotarIzquierda(x.padre);
                    w = x.padre.der;
                }
                if (w.izq.color == NEGRO && w.der.color == NEGRO) {
                    w.color = ROJO;
                    x = x.padre;
                } else {
                    if (w.der.color == NEGRO) {
                        w.izq.color = NEGRO;
                        w.color = ROJO;
                        rotarDerecha(w);
                        w = x.padre.der;
                    }
                    w.color = x.padre.color;
                    x.padre.color = NEGRO;
                    w.der.color = NEGRO;
                    rotarIzquierda(x.padre);
                    x = raiz;
                }
            } else {
                Nodo w = x.padre.izq;
                if (w.color == ROJO) {
                    w.color = NEGRO;
                    x.padre.color = ROJO;
                    rotarDerecha(x.padre);
                    w = x.padre.izq;
                }
                if (w.der.color == NEGRO && w.izq.color == NEGRO) {
                    w.color = ROJO;
                    x = x.padre;
                } else {
                    if (w.izq.color == NEGRO) {
                        w.der.color = NEGRO;
                        w.color = ROJO;
                        rotarIzquierda(w);
                        w = x.padre.izq;
                    }
                    w.color = x.padre.color;
                    x.padre.color = NEGRO;
                    w.izq.color = NEGRO;
                    rotarDerecha(x.padre);
                    x = raiz;
                }
            }
        }
        x.color = NEGRO;
    }

    // ---------- Auxiliares ----------

    private Nodo buscarNodo(T clave) {
        Nodo n = raiz;
        while (n != NIL) {
            int cmp = clave.compareTo(n.dato);
            if (cmp == 0) return n;
            n = (cmp < 0) ? n.izq : n.der;
        }
        return NIL;
    }

    private Nodo minimo(Nodo n) {
        while (n.izq != NIL) n = n.izq;
        return n;
    }

    private void trasplantar(Nodo u, Nodo v) {
        if (u.padre == NIL) raiz = v;
        else if (u == u.padre.izq) u.padre.izq = v;
        else u.padre.der = v;
        v.padre = u.padre;
    }

    private void rotarIzquierda(Nodo x) {
        Nodo y = x.der;
        x.der = y.izq;
        if (y.izq != NIL) y.izq.padre = x;
        y.padre = x.padre;
        if (x.padre == NIL) raiz = y;
        else if (x == x.padre.izq) x.padre.izq = y;
        else x.padre.der = y;
        y.izq = x;
        x.padre = y;
    }

    private void rotarDerecha(Nodo x) {
        Nodo y = x.izq;
        x.izq = y.der;
        if (y.der != NIL) y.der.padre = x;
        y.padre = x.padre;
        if (x.padre == NIL) raiz = y;
        else if (x == x.padre.der) x.padre.der = y;
        else x.padre.izq = y;
        y.der = x;
        x.padre = y;
    }

    private void inOrden(Nodo n, List<T> resultado) {
        if (n == NIL) return;
        inOrden(n.izq, resultado);
        resultado.add(n.dato);
        inOrden(n.der, resultado);
    }

    private int altura(Nodo n) {
        return n == NIL ? 0 : 1 + Math.max(altura(n.izq), altura(n.der));
    }

    private int alturaNegra(Nodo n) {
        if (n == NIL) return 1;
        if (n.color == ROJO && (n.izq.color == ROJO || n.der.color == ROJO)) return -1;
        int i = alturaNegra(n.izq);
        int d = alturaNegra(n.der);
        if (i == -1 || d == -1 || i != d) return -1;
        return i + (n.color == NEGRO ? 1 : 0);
    }

    private void imprimir(Nodo n, int nivel) {              // el árbol se ve girado 90°
        if (n == NIL) return;
        imprimir(n.der, nivel + 1);
        System.out.println("      ".repeat(nivel) + (n.color == ROJO ? "(R) " : "(N) ") + n.dato);
        imprimir(n.izq, nivel + 1);
    }
}
