package com.cenfotec.despacho.modelo;

public class Incidente implements Comparable<Incidente> {
    private int id;
    private String tipo;
    private String descripcion;
    private int prioridad;          // 1 = crítica ... 5 = baja
    private String fechaHora;       // ISO: 2026-11-10T14:30:00
    private volatile String estado; // ACTIVO, ASIGNADO, CERRADO (volatile: lo lee la interfaz desde otro hilo)
    private int idInterseccion;
    private String placaAsignada;   // unidad que lo atiende (null si aún no tiene)

    public Incidente(int id, String tipo, String descripcion, int prioridad,
                     String fechaHora, String estado, int idInterseccion) {
        this.id = id;
        this.tipo = tipo;
        this.descripcion = descripcion;
        this.prioridad = prioridad;
        this.fechaHora = fechaHora;
        this.estado = estado;
        this.idInterseccion = idInterseccion;
    }

    public int getId() { return id; }
    public String getTipo() { return tipo; }
    public String getDescripcion() { return descripcion; }
    public int getPrioridad() { return prioridad; }
    public void setPrioridad(int prioridad) { this.prioridad = prioridad; }
    public String getFechaHora() { return fechaHora; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
    public int getIdInterseccion() { return idInterseccion; }
    public String getPlacaAsignada() { return placaAsignada; }
    public void setPlacaAsignada(String placaAsignada) { this.placaAsignada = placaAsignada; }

    /** Copia independiente; los mensajes del bus llevan copias para que nadie los modifique después. */
    public Incidente copia() {
        Incidente c = new Incidente(id, tipo, descripcion, prioridad, fechaHora, estado, idInterseccion);
        c.placaAsignada = placaAsignada;
        return c;
    }

    @Override
    public int compareTo(Incidente otro) {
        int c = Integer.compare(this.prioridad, otro.prioridad);
        if (c != 0) return c;
        c = this.fechaHora.compareTo(otro.fechaHora);   // ISO se ordena como texto
        if (c != 0) return c;
        return Integer.compare(this.id, otro.id);
    }

    @Override
    public String toString() {
        return String.format("#%d [P%d] %s - %s (%s) %s",
                id, prioridad, tipo, descripcion, fechaHora, estado);
    }
}
