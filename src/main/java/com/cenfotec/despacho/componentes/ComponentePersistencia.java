package com.cenfotec.despacho.componentes;

import com.cenfotec.despacho.datos.AlmacenNube;
import com.cenfotec.despacho.datos.CargadorNube;
import com.cenfotec.despacho.mensajeria.BusMensajes;
import com.cenfotec.despacho.mensajeria.Mensaje;
import com.cenfotec.despacho.mensajeria.Mensaje.IncidenteActualizado;
import com.cenfotec.despacho.mensajeria.Mensaje.IncidenteCerrado;
import com.cenfotec.despacho.mensajeria.Mensaje.IncidenteCreado;
import com.cenfotec.despacho.mensajeria.Mensaje.LlamadaAtendida;
import com.cenfotec.despacho.mensajeria.Mensaje.LlamadaRecibida;
import com.cenfotec.despacho.mensajeria.Mensaje.UnidadActualizada;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Guarda en la nube cada cambio que anuncia el sistema. Corre en su propio hilo, así que la
 * interfaz nunca espera a la red. Cada escritura se reintenta unas veces antes de darse por fallida.
 *
 * <p>Siempre se envía el documento completo (no solo el campo cambiado), de modo que el resultado
 * es el mismo si la API actualiza por campos o reemplaza el documento.</p>
 */
public class ComponentePersistencia implements Componente {

    private static final int INTENTOS = 3;

    private final AlmacenNube almacen;
    private final long esperaEntreIntentosMs;
    private final Gson gson = new Gson();
    private final AtomicInteger guardados = new AtomicInteger();
    private final AtomicInteger fallidos = new AtomicInteger();
    private volatile String ultimoError = "";

    public ComponentePersistencia(AlmacenNube almacen) {
        this(almacen, 600);
    }

    public ComponentePersistencia(AlmacenNube almacen, long esperaEntreIntentosMs) {
        this.almacen = almacen;
        this.esperaEntreIntentosMs = esperaEntreIntentosMs;
    }

    @Override
    public String nombre() { return "Persistencia"; }

    @Override
    public void iniciar(BusMensajes bus) {
        bus.suscribir(nombre(), this::manejar,
                LlamadaRecibida.class, LlamadaAtendida.class,
                IncidenteCreado.class, IncidenteActualizado.class, IncidenteCerrado.class,
                UnidadActualizada.class);
    }

    @Override
    public String estado() {
        String base = "Nube: " + guardados.get() + " cambios guardados, " + fallidos.get() + " fallidos";
        return fallidos.get() > 0 ? base + " (último error: " + ultimoError + ")" : base;
    }

    public int guardados() { return guardados.get(); }

    public int fallidos() { return fallidos.get(); }

    private void manejar(Mensaje mensaje) {
        try {
            switch (mensaje) {
                case LlamadaRecibida m -> reintentar(() ->
                        almacen.insertar(CargadorNube.COL_LLAMADAS, m.llamada()));
                case LlamadaAtendida m -> reintentar(() ->
                        almacen.actualizar(CargadorNube.COL_LLAMADAS, filtro("id", m.llamada().getId()), json(m.llamada())));
                case IncidenteCreado m -> reintentar(() ->
                        almacen.insertar(CargadorNube.COL_INCIDENTES, m.incidente()));
                case IncidenteActualizado m -> reintentar(() ->
                        almacen.actualizar(CargadorNube.COL_INCIDENTES, filtro("id", m.incidente().getId()), json(m.incidente())));
                case IncidenteCerrado m -> reintentar(() ->
                        almacen.actualizar(CargadorNube.COL_INCIDENTES, filtro("id", m.incidente().getId()), json(m.incidente())));
                case UnidadActualizada m -> reintentar(() ->
                        almacen.actualizar(CargadorNube.COL_UNIDADES, filtro("placa", m.unidad().getPlaca()), json(m.unidad())));
                default -> { }      // los demás mensajes no se guardan
            }
            guardados.incrementAndGet();
        } catch (IOException e) {
            fallidos.incrementAndGet();
            ultimoError = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /** Operación contra la nube que puede fallar. */
    private interface Escritura {
        void ejecutar() throws IOException, InterruptedException;
    }

    private void reintentar(Escritura escritura) throws IOException, InterruptedException {
        IOException ultimo = null;
        for (int intento = 1; intento <= INTENTOS; intento++) {
            try {
                escritura.ejecutar();
                return;
            } catch (IOException e) {
                ultimo = e;
                if (intento < INTENTOS) Thread.sleep(esperaEntreIntentosMs);
            }
        }
        throw ultimo;
    }

    private JsonObject json(Object dato) {
        return gson.toJsonTree(dato).getAsJsonObject();
    }

    private JsonObject filtro(String campo, Object valor) {
        JsonObject f = new JsonObject();
        if (valor instanceof Number n) f.addProperty(campo, n);
        else f.addProperty(campo, String.valueOf(valor));
        return f;
    }
}
