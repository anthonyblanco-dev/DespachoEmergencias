package com.cenfotec.despacho;

import com.cenfotec.despacho.datos.CargadorArchivo;
import com.cenfotec.despacho.datos.CargadorNube;
import com.cenfotec.despacho.datos.DatosIniciales;
import com.cenfotec.despacho.servicio.ServicioDespacho;

import java.io.IOException;

/** Prepara el servicio: mapa desde el archivo local y resto de datos desde la nube (o el archivo si falla). */
public final class Arranque {

    private Arranque() { }

    /** Servicio ya cargado y texto de dónde salieron los datos. */
    public record Resultado(ServicioDespacho servicio, String origen) { }

    public static Resultado iniciar() throws IOException {
        ServicioDespacho servicio = new ServicioDespacho();

        DatosIniciales local = CargadorArchivo.cargar("/datos_iniciales.json");
        servicio.cargarMapa(local);     // el mapa siempre sale del archivo local

        DatosIniciales datos = null;
        String origen = "";

        try {
            CargadorNube nube = new CargadorNube();
            int subidos = nube.sembrarSiVacio(local);
            if (subidos > 0) {
                System.out.println("Se subieron " + subidos + " documentos iniciales a la nube.");
            }
            datos = nube.cargar();
            origen = "la nube (MongoDB)";
        } catch (Exception e) {
            System.out.println("No se pudo usar la nube (" + e.getMessage() + "). Se usará el archivo local.");
        }

        if (datos == null) {
            datos = local;
            origen = "el archivo local";
        }

        servicio.cargar(datos);
        System.out.println("Datos iniciales cargados desde " + origen + ": "
                + servicio.cantidadActivos() + " incidentes, "
                + servicio.getUnidades().size() + " unidades, "
                + servicio.llamadasPendientes() + " llamadas en cola.");
        return new Resultado(servicio, origen);
    }
}
