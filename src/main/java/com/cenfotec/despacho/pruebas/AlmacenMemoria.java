package com.cenfotec.despacho.pruebas;

import com.cenfotec.despacho.datos.AlmacenNube;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Base de datos falsa en memoria para las pruebas: se comporta como la nube pero sin red, y puede
 * simular fallos. Actualizar mezcla los campos recibidos en los documentos que cumplan el filtro.
 */
public class AlmacenMemoria implements AlmacenNube {

    private final Map<String, List<JsonObject>> colecciones = new HashMap<>();
    private final Gson gson = new Gson();
    private int fallosPendientes = 0;
    private int contadorId = 0;

    /** Hace que las próximas n operaciones de escritura lancen IOException. */
    public synchronized void fallarProximas(int n) { this.fallosPendientes = n; }

    private void simularFallo() throws IOException {
        if (fallosPendientes > 0) {
            fallosPendientes--;
            throw new IOException("fallo de red simulado");
        }
    }

    private List<JsonObject> coleccion(String nombre) {
        return colecciones.computeIfAbsent(nombre, k -> new ArrayList<>());
    }

    @Override
    public synchronized JsonArray listar(String coleccion) {
        JsonArray resultado = new JsonArray();
        for (JsonObject doc : coleccion(coleccion)) {
            JsonObject copia = doc.deepCopy();
            JsonObject id = new JsonObject();                  // Mongo agrega un _id a cada documento
            id.addProperty("$oid", "id" + (++contadorId));
            copia.add("_id", id);
            resultado.add(copia);
        }
        return resultado;
    }

    @Override
    public synchronized void insertar(String coleccion, Object dato) throws IOException {
        simularFallo();
        coleccion(coleccion).add(gson.toJsonTree(dato).getAsJsonObject());
    }

    @Override
    public synchronized void actualizar(String coleccion, JsonObject filtro, JsonObject datos) throws IOException {
        simularFallo();
        for (JsonObject doc : coleccion(coleccion)) {
            if (cumple(doc, filtro)) {
                for (var campo : datos.entrySet()) doc.add(campo.getKey(), campo.getValue().deepCopy());
            }
        }
    }

    @Override
    public synchronized void eliminar(String coleccion, JsonObject filtro) throws IOException {
        simularFallo();
        coleccion(coleccion).removeIf(doc -> cumple(doc, filtro));
    }

    /** Documento de la colección cuyo campo tiene ese valor, o null. */
    public synchronized JsonObject buscar(String coleccion, String campo, Object valor) {
        JsonObject filtro = new JsonObject();
        if (valor instanceof Number n) filtro.addProperty(campo, n);
        else filtro.addProperty(campo, String.valueOf(valor));
        for (JsonObject doc : coleccion(coleccion)) {
            if (cumple(doc, filtro)) return doc.deepCopy();
        }
        return null;
    }

    public synchronized int cantidad(String coleccion) { return coleccion(coleccion).size(); }

    private static boolean cumple(JsonObject doc, JsonObject filtro) {
        for (var condicion : filtro.entrySet()) {
            if (!doc.has(condicion.getKey()) || !doc.get(condicion.getKey()).equals(condicion.getValue())) return false;
        }
        return true;
    }
}
