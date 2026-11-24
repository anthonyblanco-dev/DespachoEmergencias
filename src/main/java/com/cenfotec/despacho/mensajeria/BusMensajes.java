package com.cenfotec.despacho.mensajeria;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * Bus de mensajes asincrónico (publicación/suscripción) hecho con colas bloqueantes.
 *
 * <p>Cada suscriptor tiene su propia cola y su propio hilo. Quien publica nunca espera: el mensaje
 * se deja en la cola de cada suscriptor interesado y cada uno lo procesa a su ritmo y en orden.
 * Si un manejador falla, el error se cuenta y el hilo sigue con el siguiente mensaje.</p>
 */
public class BusMensajes {

    /** Foto del estado de un suscriptor, para mostrarla en la interfaz. */
    public record Estadistica(String nombre, int recibidos, int procesados, int errores, int pendientes,
                              String ultimoError) { }

    private static final Object PARADA = new Object();

    private final List<Suscripcion> suscripciones = new CopyOnWriteArrayList<>();
    private final AtomicInteger publicados = new AtomicInteger();
    private final AtomicInteger enVuelo = new AtomicInteger();      // entregas aún sin procesar
    private volatile boolean cerrado = false;

    private class Suscripcion implements Runnable {
        final String nombre;
        final Class<?>[] tipos;                       // vacío = recibe todo
        final Consumer<Mensaje> manejador;
        final BlockingQueue<Object> cola = new LinkedBlockingQueue<>();
        final AtomicInteger recibidos = new AtomicInteger();
        final AtomicInteger procesados = new AtomicInteger();
        final AtomicInteger errores = new AtomicInteger();
        volatile String ultimoError = "";
        final Thread hilo;

        Suscripcion(String nombre, Consumer<Mensaje> manejador, Class<?>[] tipos) {
            this.nombre = nombre;
            this.manejador = manejador;
            this.tipos = tipos;
            this.hilo = new Thread(this, "bus-" + nombre);
            this.hilo.setDaemon(true);
        }

        boolean interesa(Mensaje m) {
            if (tipos.length == 0) return true;
            for (Class<?> t : tipos) if (t.isInstance(m)) return true;
            return false;
        }

        @Override
        public void run() {
            try {
                while (true) {
                    Object elemento = cola.take();
                    if (elemento == PARADA) return;
                    try {
                        manejador.accept((Mensaje) elemento);
                        procesados.incrementAndGet();
                    } catch (RuntimeException e) {
                        errores.incrementAndGet();
                        ultimoError = e.getClass().getSimpleName() + ": " + e.getMessage();
                    } finally {
                        enVuelo.decrementAndGet();
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * Registra un suscriptor y arranca su hilo.
     *
     * @param nombre    nombre del componente (se ve en las estadísticas)
     * @param manejador qué hacer con cada mensaje; corre siempre en el hilo del suscriptor
     * @param tipos     clases de mensaje que le interesan; sin ninguna recibe todos
     */
    public void suscribir(String nombre, Consumer<Mensaje> manejador, Class<?>... tipos) {
        Suscripcion s = new Suscripcion(nombre, manejador, tipos);
        suscripciones.add(s);
        s.hilo.start();
    }

    /** Entrega el mensaje a la cola de cada suscriptor interesado. No bloquea. */
    public void publicar(Mensaje mensaje) {
        if (cerrado) return;
        publicados.incrementAndGet();
        for (Suscripcion s : suscripciones) {
            if (s.interesa(mensaje)) {
                enVuelo.incrementAndGet();         // se suma antes de encolar para no dar un "vacío" falso
                s.recibidos.incrementAndGet();
                s.cola.add(mensaje);
            }
        }
    }

    /** Espera a que todas las colas se vacíen (incluidos mensajes que los manejadores publiquen). */
    public boolean esperarInactivo(long milisegundos) {
        long limite = System.nanoTime() + milisegundos * 1_000_000L;
        while (enVuelo.get() > 0) {
            if (System.nanoTime() > limite) return false;
            try {
                Thread.sleep(5);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        return true;
    }

    /** Termina los hilos después de procesar lo que ya estaba en cola. */
    public void cerrar() {
        if (cerrado) return;
        cerrado = true;
        for (Suscripcion s : suscripciones) s.cola.add(PARADA);
        for (Suscripcion s : suscripciones) {
            try {
                s.hilo.join(3000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }

    public int totalPublicados() { return publicados.get(); }

    public int pendientesTotales() { return enVuelo.get(); }

    public List<Estadistica> estadisticas() {
        List<Estadistica> lista = new ArrayList<>();
        for (Suscripcion s : suscripciones) {
            lista.add(new Estadistica(s.nombre, s.recibidos.get(), s.procesados.get(), s.errores.get(),
                    s.cola.size(), s.ultimoError));
        }
        return lista;
    }
}
