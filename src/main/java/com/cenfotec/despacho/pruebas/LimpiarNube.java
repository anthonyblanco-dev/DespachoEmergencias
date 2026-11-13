package com.cenfotec.despacho.pruebas;

import com.cenfotec.despacho.datos.CargadorNube;
import com.cenfotec.despacho.datos.ClienteApi;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class LimpiarNube {

    public static void main(String[] args) throws Exception {
        ClienteApi api = new ClienteApi();
        String[] colecciones = {
                CargadorNube.COL_UNIDADES,
                CargadorNube.COL_INCIDENTES,
                CargadorNube.COL_LLAMADAS
        };

        for (String coleccion : colecciones) {
            JsonArray documentos = api.listar(coleccion);
            int borrados = 0;
            for (JsonElement e : documentos) {
                JsonObject doc = e.getAsJsonObject();
                if (!doc.has("_id")) continue;
                JsonElement idElemento = doc.get("_id");
                String id = idElemento.isJsonObject()
                        ? idElemento.getAsJsonObject().get("$oid").getAsString()
                        : idElemento.getAsString();
                JsonObject filtro = new JsonObject();
                filtro.addProperty("_id", id);
                api.eliminar(coleccion, filtro);
                borrados++;
            }
            System.out.println(coleccion + ": " + borrados + " documentos eliminados");
        }
        System.out.println("Nube limpia. Ahora ejecuta Main.");
    }
}
