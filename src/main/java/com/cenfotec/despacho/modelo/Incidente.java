package com.cenfotec.despacho.modelo;

public class Incidente implements Comparable<Incidente> {
    private int id;
    private String tipo;
    private String descripcion;
    private int prioridad;          // 1 = crítica ... 5 = baja
    private String fechaHora;       // ISO: 2026-11-10T14:30:00
    private String estado;          // ACTIVO, ASIGNADO, CERRADO
    private int idInterseccion;

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
