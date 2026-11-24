package com.cenfotec.despacho.modelo;

public class Llamada {
    private int id;
    private String telefono;
    private String horaRecepcion;
    private String ubicacionReferida;
    private boolean atendida;       // true cuando ya se convirtió en incidente (queda guardada como registro)

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
    public boolean isAtendida() { return atendida; }
    public void marcarAtendida() { this.atendida = true; }

    /** Copia independiente para los mensajes del bus. */
    public Llamada copia() {
        Llamada c = new Llamada(id, telefono, horaRecepcion, ubicacionReferida);
        c.atendida = atendida;
        return c;
    }

    @Override
    public String toString() {
        return "Llamada #" + id + " de " + telefono + " desde " + ubicacionReferida
                + " (" + horaRecepcion + ")";
    }
}
