package com.cenfotec.despacho.pruebas;

import com.cenfotec.despacho.mensajeria.BusMensajes;
import com.cenfotec.despacho.mensajeria.Mensaje;
import com.cenfotec.despacho.mensajeria.Mensaje.DespachoFallido;
import com.cenfotec.despacho.mensajeria.Mensaje.IncidenteCreado;
import com.cenfotec.despacho.mensajeria.Mensaje.UnidadActualizada;
import com.cenfotec.despacho.modelo.Incidente;
import com.cenfotec.despacho.modelo.Unidad;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/** Comprueba el bus de mensajes: orden, filtrado por tipo, errores, asincronía y concurrencia. */
public class PruebaBus {

    public static void main(String[] args) throws Exception {
        probarOrdenYFiltrado();
        probarErroresNoMatanElHilo();
        probarPublicarNoEspera();
        probarCadenaDeMensajes();
        probarVariosPublicadores();
        probarCierre();
        System.out.println("PRUEBA SUPERADA: el bus entrega en orden, filtra, resiste errores y es asincrónico.");
    }

    private static Mensaje creado(int id) {
        return new IncidenteCreado(new Incidente(id, "ROBO", "x", 3, "2026-11-10T08:00:00", "ACTIVO", 1));
    }

    private static Mensaje unidad(int n) {
        return new UnidadActualizada(new Unidad("U-" + n, "POLICIA", "DISPONIBLE", 1));
    }

    private static void probarOrdenYFiltrado() {
        BusMensajes bus = new BusMensajes();
        List<Mensaje> todos = Collections.synchronizedList(new ArrayList<>());
        List<Mensaje> soloCreados = Collections.synchronizedList(new ArrayList<>());
        bus.suscribir("todos", todos::add);
        bus.suscribir("creados", soloCreados::add, IncidenteCreado.class);

        for (int i = 0; i < 100; i++) bus.publicar(i % 2 == 0 ? creado(i) : unidad(i));
        verificar(bus.esperarInactivo(3000), "el bus debe quedar inactivo");

        verificar(todos.size() == 100, "'todos' debe recibir los 100 mensajes");
        verificar(soloCreados.size() == 50, "'creados' solo debe recibir los 50 IncidenteCreado");
        for (int i = 0; i < 100; i++) {
            Mensaje m = todos.get(i);
            int idEsperado = i;
            boolean enOrden = (i % 2 == 0)
                    ? m instanceof IncidenteCreado c && c.incidente().getId() == idEsperado
                    : m instanceof UnidadActualizada u && u.unidad().getPlaca().equals("U-" + idEsperado);
            verificar(enOrden, "el mensaje " + i + " llegó fuera de orden");
        }
        verificar(bus.totalPublicados() == 100, "total publicados");
        bus.cerrar();
        System.out.println("Orden y filtrado: OK");
    }

    private static void probarErroresNoMatanElHilo() {
        BusMensajes bus = new BusMensajes();
        AtomicInteger buenos = new AtomicInteger();
        bus.suscribir("fragil", m -> {
            if (m instanceof IncidenteCreado c && c.incidente().getId() == 3) {
                throw new IllegalStateException("fallo a propósito");
            }
            buenos.incrementAndGet();
        });
        for (int i = 0; i < 10; i++) bus.publicar(creado(i));
        verificar(bus.esperarInactivo(3000), "debe quedar inactivo aunque un manejador falle");

        BusMensajes.Estadistica e = bus.estadisticas().get(0);
        verificar(buenos.get() == 9, "los otros 9 mensajes deben procesarse, fueron " + buenos.get());
        verificar(e.errores() == 1, "debe contarse 1 error");
        verificar(e.procesados() == 9 && e.recibidos() == 10, "contadores de procesados y recibidos");
        verificar(e.ultimoError().contains("fallo a propósito"), "debe guardarse el último error");
        bus.cerrar();
        System.out.println("Errores en manejadores: OK");
    }

    private static void probarPublicarNoEspera() {
        BusMensajes bus = new BusMensajes();
        bus.suscribir("lento", m -> {
            try {
                Thread.sleep(40);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        long inicio = System.nanoTime();
        for (int i = 0; i < 25; i++) bus.publicar(creado(i));          // el consumidor necesita ~1 segundo
        long publicarMs = (System.nanoTime() - inicio) / 1_000_000;

        verificar(publicarMs < 300, "publicar no debe esperar al consumidor (tardó " + publicarMs + " ms)");
        verificar(bus.pendientesTotales() > 0, "justo después de publicar debe haber mensajes en cola");
        verificar(!bus.esperarInactivo(50), "con poco tiempo de espera aún no debe estar inactivo");
        verificar(bus.esperarInactivo(5000), "al final debe procesarlo todo");
        verificar(bus.pendientesTotales() == 0, "sin pendientes al final");
        bus.cerrar();
        System.out.println("Publicar sin esperar: OK (publicó 25 mensajes en " + publicarMs + " ms)");
    }

    private static void probarCadenaDeMensajes() {
        BusMensajes bus = new BusMensajes();
        AtomicInteger fallos = new AtomicInteger();
        bus.suscribir("intermedio", m -> {
            if (m instanceof IncidenteCreado c) bus.publicar(new DespachoFallido(c.incidente().getId(), "sin unidades"));
        }, IncidenteCreado.class);
        bus.suscribir("final", m -> {
            try {
                Thread.sleep(2);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            fallos.incrementAndGet();
        }, DespachoFallido.class);

        for (int i = 0; i < 40; i++) bus.publicar(creado(i));
        verificar(bus.esperarInactivo(5000), "debe esperar también los mensajes publicados por manejadores");
        verificar(fallos.get() == 40, "los 40 mensajes encadenados deben llegar, llegaron " + fallos.get());
        bus.cerrar();
        System.out.println("Mensajes encadenados: OK");
    }

    private static void probarVariosPublicadores() throws InterruptedException {
        BusMensajes bus = new BusMensajes();
        AtomicInteger recibidos = new AtomicInteger();
        bus.suscribir("contador", m -> recibidos.incrementAndGet());

        List<Thread> hilos = new ArrayList<>();
        for (int t = 0; t < 4; t++) {
            Thread h = new Thread(() -> {
                for (int i = 0; i < 500; i++) bus.publicar(creado(i));
            });
            hilos.add(h);
            h.start();
        }
        for (Thread h : hilos) h.join();
        verificar(bus.esperarInactivo(5000), "debe vaciarse");
        verificar(recibidos.get() == 2000, "deben llegar los 2000 mensajes, llegaron " + recibidos.get());
        bus.cerrar();
        System.out.println("Cuatro publicadores a la vez: OK");
    }

    private static void probarCierre() {
        BusMensajes bus = new BusMensajes();
        AtomicInteger recibidos = new AtomicInteger();
        bus.suscribir("uno", m -> recibidos.incrementAndGet());
        for (int i = 0; i < 20; i++) bus.publicar(creado(i));
        bus.cerrar();                                              // procesa lo que había en cola antes de parar
        verificar(recibidos.get() == 20, "al cerrar debe procesar lo pendiente, procesó " + recibidos.get());
        bus.publicar(creado(99));                                  // después de cerrar se ignora
        verificar(bus.totalPublicados() == 20, "tras cerrar no se aceptan más mensajes");
        bus.cerrar();                                              // cerrar dos veces no hace daño
        System.out.println("Cierre ordenado: OK");
    }

    private static void verificar(boolean condicion, String mensaje) {
        if (!condicion) throw new IllegalStateException("FALLÓ: " + mensaje);
    }
}
