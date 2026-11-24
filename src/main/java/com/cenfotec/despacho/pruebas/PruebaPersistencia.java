package com.cenfotec.despacho.pruebas;

import com.cenfotec.despacho.Arranque;
import com.cenfotec.despacho.componentes.ComponentePersistencia;
import com.cenfotec.despacho.datos.CargadorArchivo;
import com.cenfotec.despacho.datos.CargadorNube;
import com.cenfotec.despacho.datos.DatosIniciales;
import com.cenfotec.despacho.modelo.Incidente;
import com.cenfotec.despacho.servicio.ServicioDespacho;
import com.google.gson.JsonObject;

/**
 * Prueba de extremo a extremo de la persistencia, sin red: sembrar, trabajar con el sistema
 * (los cambios viajan por el bus hasta el componente de persistencia), "reiniciar" cargando todo
 * otra vez desde el almacén y comprobar que el estado es el mismo.
 */
public class PruebaPersistencia {

    public static void main(String[] args) throws Exception {
        DatosIniciales local = CargadorArchivo.cargar("/datos_iniciales.json");
        AlmacenMemoria nube = new AlmacenMemoria();

        // ---- 1. Sembrar la nube vacía ----
        CargadorNube cargador = new CargadorNube(nube);
        verificar(cargador.sembrarSiVacio(local) == 17, "debe sembrar 4 unidades + 10 incidentes + 3 llamadas");
        verificar(cargador.sembrarSiVacio(local) == 0, "una nube con datos no se vuelve a sembrar");

        // ---- 2. Arrancar el sistema y trabajar ----
        ServicioDespacho servicio = new ServicioDespacho();
        servicio.cargarMapa(local);
        servicio.cargar(cargador.cargar());
        Arranque.Resultado sistema = Arranque.conectar(servicio, "almacén de prueba", new ComponentePersistencia(nube, 10));

        verificar(servicio.despachar(2).exito(), "despachar el incendio #2");           // BOM-201
        verificar(servicio.despachar(4).exito(), "despachar el paro cardíaco #4");      // AMB-102
        verificar(servicio.reclasificar(5, 1), "reclasificar #5");
        servicio.recibirLlamada("8999-0000", "Calle nueva");                             // llamada #4
        Incidente desdeLlamada = servicio.atenderSiguienteLlamada("ROBO", 2, 3);         // atiende la llamada #1
        verificar(desdeLlamada.getId() == 11, "el incidente nuevo debe ser el #11");
        verificar(servicio.cerrarIncidente(4) != null, "cerrar #4");

        verificar(sistema.bus().esperarInactivo(5000), "la persistencia debe terminar de guardar");
        verificar(sistema.persistencia().fallidos() == 0, "no debe haber escrituras fallidas");
        verificar(sistema.persistencia().guardados() >= 10, "debe haber guardado todos los cambios");

        // ---- 3. Lo que quedó guardado ----
        verificar(estado(nube, CargadorNube.COL_UNIDADES, "placa", "BOM-201").equals("OCUPADA"), "BOM-201 ocupada en la nube");
        verificar(estado(nube, CargadorNube.COL_UNIDADES, "placa", "AMB-102").equals("DISPONIBLE"), "AMB-102 liberada en la nube");
        JsonObject inc2 = nube.buscar(CargadorNube.COL_INCIDENTES, "id", 2);
        verificar(inc2.get("estado").getAsString().equals("ASIGNADO"), "#2 ASIGNADO en la nube");
        verificar(inc2.get("placaAsignada").getAsString().equals("BOM-201"), "#2 recuerda su unidad en la nube");
        verificar(nube.buscar(CargadorNube.COL_INCIDENTES, "id", 5).get("prioridad").getAsInt() == 1, "prioridad nueva de #5");
        verificar(estado(nube, CargadorNube.COL_INCIDENTES, "id", 4).equals("CERRADO"), "#4 CERRADO en la nube");
        verificar(nube.buscar(CargadorNube.COL_INCIDENTES, "id", 11) != null, "#11 insertado en la nube");
        verificar(nube.cantidad(CargadorNube.COL_INCIDENTES) == 11, "11 incidentes en total en la nube");
        verificar(nube.buscar(CargadorNube.COL_LLAMADAS, "id", 1).get("atendida").getAsBoolean(), "llamada #1 marcada atendida");
        verificar(!nube.buscar(CargadorNube.COL_LLAMADAS, "id", 4).get("atendida").getAsBoolean(), "llamada #4 sigue pendiente");

        // ---- 4. Reiniciar: cargar todo de nuevo desde la nube ----
        ServicioDespacho despues = new ServicioDespacho();
        despues.cargarMapa(local);
        despues.cargar(cargador.cargar());
        verificar(despues.cantidadActivos() == 10, "tras reiniciar: 10 activos (10 - #4 + #11)");
        verificar(despues.cantidadHistorial() == 1 && despues.historialValido(), "tras reiniciar: #4 está en el historial B+");
        verificar(despues.buscarIncidente(5).getPrioridad() == 1, "tras reiniciar: #5 conserva su prioridad");
        verificar("BOM-201".equals(despues.unidadAsignada(2)), "tras reiniciar: #2 sigue atendido por BOM-201");
        verificar(despues.buscarUnidad("BOM-201").getEstado().equals("OCUPADA"), "tras reiniciar: BOM-201 sigue ocupada");
        verificar(despues.buscarUnidad("AMB-102").getEstado().equals("DISPONIBLE"), "tras reiniciar: AMB-102 disponible");
        verificar(despues.llamadasPendientes() == 3 && despues.verSiguienteLlamada().getId() == 2,
                "tras reiniciar: la llamada atendida no vuelve a la cola");
        verificar(despues.arbolValido(), "tras reiniciar: árbol Rojo-Negro válido");
        verificar(despues.cerrarIncidente(2) != null
                && despues.buscarUnidad("BOM-201").getEstado().equals("DISPONIBLE"),
                "tras reiniciar: cerrar #2 libera a BOM-201 (la asignación sobrevivió)");
        verificar(despues.crearIncidente("RUIDO", "x", 5, 1).getId() == 12, "los ids siguen donde se quedaron");

        // ---- 5. Reconciliación de datos inconsistentes ----
        AlmacenMemoria danada = new AlmacenMemoria();
        new CargadorNube(danada).sembrarSiVacio(local);
        JsonObject solo = new JsonObject();
        solo.addProperty("estado", "OCUPADA");
        danada.actualizar(CargadorNube.COL_UNIDADES, filtro("placa", "POL-301"), solo);   // ocupada sin incidente
        JsonObject huerfano = new JsonObject();
        huerfano.addProperty("estado", "ASIGNADO");
        huerfano.addProperty("placaAsignada", "NO-EXISTE");
        danada.actualizar(CargadorNube.COL_INCIDENTES, filtro("id", 3), huerfano);         // asignado a una unidad inexistente
        ServicioDespacho reparado = new ServicioDespacho();
        reparado.cargarMapa(local);
        reparado.cargar(new CargadorNube(danada).cargar());
        verificar(reparado.buscarUnidad("POL-301").getEstado().equals("DISPONIBLE"), "unidad ocupada sin incidente se libera");
        verificar(reparado.buscarIncidente(3).getEstado().equals("ACTIVO"), "incidente asignado a unidad inexistente vuelve a ACTIVO");

        // ---- 6. Reintentos y fallos ----
        nube.fallarProximas(2);                                    // dos fallos seguidos: el tercer intento funciona
        servicio.recibirLlamada("8777-1111", "Reintento");
        verificar(sistema.bus().esperarInactivo(5000), "esperar guardado con reintentos");
        verificar(sistema.persistencia().fallidos() == 0, "con 2 fallos y 3 intentos no se pierde nada");
        verificar(nube.buscar(CargadorNube.COL_LLAMADAS, "telefono", "8777-1111") != null, "la llamada se guardó tras reintentar");

        nube.fallarProximas(3);                                    // fallan los 3 intentos: se da por perdido ese cambio
        servicio.recibirLlamada("8666-2222", "Se pierde");
        verificar(sistema.bus().esperarInactivo(5000), "esperar el fallo");
        verificar(sistema.persistencia().fallidos() == 1, "debe contarse 1 escritura fallida");
        verificar(sistema.persistencia().estado().contains("fallo de red simulado"), "el estado muestra el error");
        verificar(nube.buscar(CargadorNube.COL_LLAMADAS, "telefono", "8666-2222") == null, "la llamada perdida no está guardada");

        servicio.recibirLlamada("8555-3333", "Sigue funcionando");   // el componente se recupera solo
        verificar(sistema.bus().esperarInactivo(5000), "esperar guardado posterior");
        verificar(nube.buscar(CargadorNube.COL_LLAMADAS, "telefono", "8555-3333") != null,
                "tras un fallo el componente sigue guardando");

        // ---- 7. Despacho automático por mensajes ----
        sistema.despachoAutomatico().setActivo(true);
        Incidente medico1 = servicio.crearIncidente("MEDICA", "auto 1", 2, 14);
        Incidente medico2 = servicio.crearIncidente("MEDICA", "auto 2", 2, 8);
        Incidente medico3 = servicio.crearIncidente("MEDICA", "auto 3", 2, 10);
        verificar(sistema.bus().esperarInactivo(5000), "esperar el despacho automático");
        int asignados = 0;
        for (Incidente i : new Incidente[]{medico1, medico2, medico3}) {
            if ("ASIGNADO".equals(servicio.buscarIncidente(i.getId()).getEstado())) asignados++;
        }
        verificar(asignados == 2, "con 2 ambulancias libres se asignan 2 de los 3, se asignaron " + asignados);
        verificar(sistema.registro().recientes().stream().anyMatch(l -> l.contains("no fue posible")),
                "el que no tuvo unidad genera un mensaje de despacho fallido");
        verificar(estado(nube, CargadorNube.COL_INCIDENTES, "id", medico1.getId()).equals("ASIGNADO"),
                "la asignación automática también se guarda en la nube");
        verificar(sistema.persistencia().fallidos() == 1, "no hubo más fallos de guardado");

        // ---- 8. Cierre ----
        sistema.cerrar();
        System.out.println("Guardados: " + sistema.persistencia().guardados()
                + " | fallidos: " + sistema.persistencia().fallidos()
                + " | eventos registrados: " + sistema.registro().total());
        System.out.println("PRUEBA SUPERADA: los cambios se guardan, sobreviven al reinicio y los fallos no detienen el sistema.");
    }

    private static JsonObject filtro(String campo, Object valor) {
        JsonObject f = new JsonObject();
        if (valor instanceof Number n) f.addProperty(campo, n);
        else f.addProperty(campo, String.valueOf(valor));
        return f;
    }

    private static String estado(AlmacenMemoria nube, String coleccion, String campo, Object valor) {
        JsonObject doc = nube.buscar(coleccion, campo, valor);
        return doc == null ? "(no existe)" : doc.get("estado").getAsString();
    }

    private static void verificar(boolean condicion, String mensaje) {
        if (!condicion) throw new IllegalStateException("FALLÓ: " + mensaje);
    }
}
