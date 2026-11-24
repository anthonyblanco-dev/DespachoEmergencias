package com.cenfotec.despacho.componentes;

import com.cenfotec.despacho.mensajeria.BusMensajes;
import com.cenfotec.despacho.mensajeria.Mensaje;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * Escucha todos los mensajes, guarda los más recientes para mostrarlos y avisa a la interfaz
 * (con {@code alRecibir}) para que se redibuje.
 */
public class ComponenteRegistroEventos implements Componente {

    private static final int MAXIMO = 300;
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final Deque<String> eventos = new ArrayDeque<>();
    private volatile Runnable alRecibir = () -> { };
    private volatile int total = 0;

    @Override
    public String nombre() { return "Registro de eventos"; }

    @Override
    public void iniciar(BusMensajes bus) {
        bus.suscribir(nombre(), this::registrar);
    }

    @Override
    public String estado() { return total + " eventos registrados"; }

    /** Qué hacer cada vez que llega un evento (la interfaz lo usa para refrescarse). */
    public void setAlRecibir(Runnable accion) { this.alRecibir = accion == null ? () -> { } : accion; }

    /** Eventos más recientes primero. */
    public List<String> recientes() {
        synchronized (eventos) {
            return new ArrayList<>(eventos);
        }
    }

    public int total() { return total; }

    private void registrar(Mensaje m) {
        String linea = LocalTime.now().format(HORA) + "  " + m.resumen();
        synchronized (eventos) {
            eventos.addFirst(linea);
            if (eventos.size() > MAXIMO) eventos.removeLast();
        }
        total++;
        alRecibir.run();
    }
}
