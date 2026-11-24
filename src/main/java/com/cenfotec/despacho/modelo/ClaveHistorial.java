package com.cenfotec.despacho.modelo;

/**
 * Clave del índice del historial (árbol B+): ordena por fecha y hora y, si coinciden,
 * por id. Así dos incidentes de la misma hora no chocan en el índice.
 */
public record ClaveHistorial(String fechaHora, int id) implements Comparable<ClaveHistorial> {

    @Override
    public int compareTo(ClaveHistorial otra) {
        int c = this.fechaHora.compareTo(otra.fechaHora);   // ISO se ordena como texto
        return c != 0 ? c : Integer.compare(this.id, otra.id);
    }

    @Override
    public String toString() {
        return fechaHora + " #" + id;
    }
}
