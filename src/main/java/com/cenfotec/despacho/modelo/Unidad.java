package com.cenfotec.despacho.modelo;

public class Unidad {
    private String placa;
    private String tipo;            // AMBULANCIA, BOMBEROS, POLICIA
    private String estado;          // DISPONIBLE, OCUPADA
    private int idInterseccion;

    public Unidad(String placa, String tipo, String estado, int idInterseccion) {
        this.placa = placa;
        this.tipo = tipo;
        this.estado = estado;
        this.idInterseccion = idInterseccion;
    }

    public String getPlaca() { return placa; }
    public String getTipo() { return tipo; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
    public int getIdInterseccion() { return idInterseccion; }

    @Override
    public String toString() {
        return placa + " (" + tipo + ", " + estado + ", intersección " + idInterseccion + ")";
    }
}
