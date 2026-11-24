package com.cenfotec.despacho;

import com.cenfotec.despacho.componentes.Componente;
import com.cenfotec.despacho.componentes.ComponenteDespachoAutomatico;
import com.cenfotec.despacho.componentes.ComponentePersistencia;
import com.cenfotec.despacho.componentes.ComponenteRegistroEventos;
import com.cenfotec.despacho.datos.CargadorArchivo;
import com.cenfotec.despacho.datos.CargadorNube;
import com.cenfotec.despacho.datos.ClienteApi;
import com.cenfotec.despacho.datos.DatosIniciales;
import com.cenfotec.despacho.mensajeria.BusMensajes;
import com.cenfotec.despacho.servicio.ServicioDespacho;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Arma el sistema completo: carga los datos (nube o archivo), crea el bus de mensajes y conecta
 * los componentes. La consola y la ventana usan esta misma clase.
 */
public final class Arranque {

    private Arranque() { }

    /**
     * El sistema ya armado.
     *
     * @param persistencia es null cuando se trabaja sin nube (los cambios solo viven en memoria)
     */
    public record Resultado(ServicioDespacho servicio, String origen, BusMensajes bus,
                            ComponenteRegistroEventos registro, ComponenteDespachoAutomatico despachoAutomatico,
                            ComponentePersistencia persistencia) {

        public boolean hayNube() { return persistencia != null; }

        public List<Componente> componentes() {
            List<Componente> lista = new ArrayList<>();
            lista.add(registro);
            lista.add(despachoAutomatico);
            if (persistencia != null) lista.add(persistencia);
            return lista;
        }

        /** Espera a que se guarden los cambios pendientes y detiene los hilos. Llamar al salir. */
        public void cerrar() {
            if (!bus.esperarInactivo(10_000)) {
                System.out.println("Aviso: quedaron cambios sin guardar en la nube.");
            }
            bus.cerrar();
        }
    }

    public static Resultado iniciar() throws IOException {
        ServicioDespacho servicio = new ServicioDespacho();

        DatosIniciales local = CargadorArchivo.cargar("/datos_iniciales.json");
        servicio.cargarMapa(local);     // el mapa siempre sale del archivo local

        DatosIniciales datos = null;
        ClienteApi api = null;
        String origen = "";

        try {
            api = new ClienteApi();
            CargadorNube nube = new CargadorNube(api);
            int subidos = nube.sembrarSiVacio(local);
            if (subidos > 0) {
                System.out.println("Se subieron " + subidos + " documentos iniciales a la nube.");
            }
            datos = nube.cargar();
            origen = "la nube (MongoDB)";
        } catch (Exception e) {
            System.out.println("No se pudo usar la nube (" + e.getMessage() + "). Se usará el archivo local.");
            api = null;
        }

        if (datos == null) {
            datos = local;
            origen = "el archivo local";
        }

        servicio.cargar(datos);
        System.out.println("Datos iniciales cargados desde " + origen + ": "
                + servicio.cantidadActivos() + " incidentes activos, "
                + servicio.cantidadHistorial() + " cerrados, "
                + servicio.getUnidades().size() + " unidades, "
                + servicio.llamadasPendientes() + " llamadas en cola.");

        return conectar(servicio, origen, api == null ? null : new ComponentePersistencia(api));
    }

    /**
     * Crea el bus, arranca los componentes y hace que el servicio anuncie sus cambios.
     * Se separa de {@link #iniciar()} para poder armar el sistema en las pruebas con un almacén falso.
     */
    public static Resultado conectar(ServicioDespacho servicio, String origen, ComponentePersistencia persistencia) {
        BusMensajes bus = new BusMensajes();
        ComponenteRegistroEventos registro = new ComponenteRegistroEventos();
        ComponenteDespachoAutomatico despacho = new ComponenteDespachoAutomatico(servicio);

        registro.iniciar(bus);
        despacho.iniciar(bus);
        if (persistencia != null) persistencia.iniciar(bus);
        servicio.setPublicador(bus::publicar);      // desde aquí cada cambio se anuncia por el bus

        return new Resultado(servicio, origen, bus, registro, despacho, persistencia);
    }
}
