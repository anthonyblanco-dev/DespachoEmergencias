package com.cenfotec.despacho.componentes;

import com.cenfotec.despacho.mensajeria.BusMensajes;
import com.cenfotec.despacho.mensajeria.Mensaje.DespachoFallido;
import com.cenfotec.despacho.mensajeria.Mensaje.IncidenteCreado;
import com.cenfotec.despacho.servicio.ServicioDespacho;
import com.cenfotec.despacho.servicio.ServicioDespacho.ResultadoDespacho;

/**
 * Cuando se anuncia un incidente nuevo y el despacho automático está activo, busca la unidad más
 * cercana y la asigna sin intervención del operador. Si no se puede, avisa con un mensaje.
 */
public class ComponenteDespachoAutomatico implements Componente {

    private final ServicioDespacho servicio;
    private volatile boolean activo = false;
    private volatile int despachados = 0;
    private volatile int fallidos = 0;
    private BusMensajes bus;

    public ComponenteDespachoAutomatico(ServicioDespacho servicio) {
        this.servicio = servicio;
    }

    @Override
    public String nombre() { return "Despacho automático"; }

    @Override
    public void iniciar(BusMensajes bus) {
        this.bus = bus;
        bus.suscribir(nombre(), m -> {
            if (m instanceof IncidenteCreado creado) atender(creado.incidente().getId());
        }, IncidenteCreado.class);
    }

    @Override
    public String estado() {
        return (activo ? "Activo" : "Apagado") + " | despachados: " + despachados + " | sin unidad: " + fallidos;
    }

    public boolean isActivo() { return activo; }

    public void setActivo(boolean activo) { this.activo = activo; }

    private void atender(int idIncidente) {
        if (!activo) return;
        ResultadoDespacho r = servicio.despachar(idIncidente);
        if (r.exito()) {
            despachados++;
        } else {
            fallidos++;
            bus.publicar(new DespachoFallido(idIncidente, r.mensaje()));
        }
    }
}
