package com.cenfotec.despacho.modelo;

public class Llamada {
    private int id;
    private String telefono;
    private String horaRecepcion;
    private String ubicacionReferida;

    public Llamada(int id, String telefono, String horaRecepcion, String ubicacionReferida) {
        this.id = id;
        this.telefono = telefono;
        this.horaRecepcion = horaRecepcion;
        this.ubicacionReferida = ubicacionReferida;
    }

    public int getId() { return id; }
    public String getTelefono() { return telefono; }
    public String getHoraRecepcion() { return horaRecepcion; }
    public String getUbicacionReferida() { return ubicacionReferida; }

    @Override
    public String toString() {
        return "Llamada #" + id + " de " + telefono + " desde " + ubicacionReferida
                + " (" + horaRecepcion + ")";
    }
}
