package com.cenfotec.despacho.datos;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.util.List;

public class CargadorNube {

    public static final String COL_UNIDADES = "despacho_unidades";
    public static final String COL_INCIDENTES = "despacho_incidentes";
    public static final String COL_LLAMADAS = "despacho_llamadas";

    private final AlmacenNube api;
    private final Gson gson = new Gson();

    public CargadorNube() {
        this(new ClienteApi());
    }

    public CargadorNube(AlmacenNube almacen) {
        this.api = almacen;
    }

    /** Sube los datos base a la nube solo en las colecciones que estén vacías. Devuelve cuántos subió. */
    public int sembrarSiVacio(DatosIniciales base) throws IOException, InterruptedException {
        int subidos = 0;
        subidos += sembrar(COL_UNIDADES, base.getUnidades());
        subidos += sembrar(COL_INCIDENTES, base.getIncidentes());
        subidos += sembrar(COL_LLAMADAS, base.getLlamadas());
        return subidos;
    }

    /** Lee las tres colecciones de la nube y las convierte a los objetos del modelo. */
    public DatosIniciales cargar() throws IOException, InterruptedException {
        JsonObject raiz = new JsonObject();
        raiz.add("unidades", limpiar(api.listar(COL_UNIDADES)));
        raiz.add("incidentes", limpiar(api.listar(COL_INCIDENTES)));
        raiz.add("llamadas", limpiar(api.listar(COL_LLAMADAS)));

        DatosIniciales datos = gson.fromJson(raiz, DatosIniciales.class);
        if (datos.getIncidentes() == null || datos.getIncidentes().isEmpty()) {
            throw new IOException("La nube no tiene incidentes guardados.");
        }
        return datos;
    }

    private <T> int sembrar(String coleccion, List<T> datos) throws IOException, InterruptedException {
        if (datos == null || !api.listar(coleccion).isEmpty()) return 0;
        for (T dato : datos) {
            api.insertar(coleccion, dato);
        }
        return datos.size();
    }

    /** Quita el campo _id que agrega Mongo, porque el modelo no lo usa. */
    private JsonArray limpiar(JsonArray documentos) {
        JsonArray limpio = new JsonArray();
        for (JsonElement e : documentos) {
            JsonObject copia = e.getAsJsonObject().deepCopy();
            copia.remove("_id");
            limpio.add(copia);
        }
        return limpio;
    }
}
