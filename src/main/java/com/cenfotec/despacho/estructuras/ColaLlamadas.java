package com.cenfotec.despacho.estructuras;

import com.cenfotec.despacho.modelo.Llamada;

public class ColaLlamadas {
    private Llamada[] datos;
    private int frente = 0;
    private int tamano = 0;

    public ColaLlamadas(int capacidadInicial) {
        datos = new Llamada[Math.max(1, capacidadInicial)];
    }

    public void encolar(Llamada llamada) {
        if (tamano == datos.length) crecer();
        datos[(frente + tamano) % datos.length] = llamada;
        tamano++;
    }

    public Llamada desencolar() {
        if (tamano == 0) return null;
        Llamada llamada = datos[frente];
        datos[frente] = null;
        frente = (frente + 1) % datos.length;
        tamano--;
        return llamada;
    }

    public Llamada ver() { return tamano == 0 ? null : datos[frente]; }

    public boolean estaVacia() { return tamano == 0; }

    public int tamano() { return tamano; }

    private void crecer() {
        Llamada[] nuevo = new Llamada[datos.length * 2];
        for (int i = 0; i < tamano; i++) {
            nuevo[i] = datos[(frente + i) % datos.length];
        }
        datos = nuevo;
        frente = 0;
    }
}
