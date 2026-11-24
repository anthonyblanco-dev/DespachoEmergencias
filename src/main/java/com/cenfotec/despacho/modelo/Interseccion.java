package com.cenfotec.despacho.modelo;

/** Punto del mapa de la ciudad. Las coordenadas x, y solo sirven para dibujarlo. */
public class Interseccion {
    private int id;
    private String nombre;
    private int x;
    private int y;

    public Interseccion(int id, String nombre, int x, int y) {
        this.id = id;
        this.nombre = nombre;
        this.x = x;
        this.y = y;
    }

    public int getId() { return id; }
    public String getNombre() { return nombre; }
    public int getX() { return x; }
    public int getY() { return y; }

    @Override
    public String toString() {
        return id + " (" + nombre + ")";
    }
}
