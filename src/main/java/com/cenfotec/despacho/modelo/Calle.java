package com.cenfotec.despacho.modelo;

/** Tramo entre dos intersecciones, con su tiempo de recorrido en minutos. */
public class Calle {
    private int origen;
    private int destino;
    private int tiempo;

    public Calle(int origen, int destino, int tiempo) {
        this.origen = origen;
        this.destino = destino;
        this.tiempo = tiempo;
    }

    public int getOrigen() { return origen; }
    public int getDestino() { return destino; }
    public int getTiempo() { return tiempo; }
}
