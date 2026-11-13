package com.cenfotec.despacho.datos;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

public class ClienteApi {

    private static final String URL = "https://paginas-web-cr.com/Api/apis/mongodb.php";

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(8))
            .build();
    private final Gson gson = new Gson();

    /** GET: devuelve el arreglo "documentos" de la colección. */
    public JsonArray listar(String coleccion) throws IOException, InterruptedException {
        String url = URL + "?coleccion=" + URLEncoder.encode(coleccion, StandardCharsets.UTF_8);
        HttpRequest peticion = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(15))
                .header("Accept", "application/json")
                .GET()
                .build();
        HttpResponse<String> respuesta = http.send(peticion, HttpResponse.BodyHandlers.ofString());
        verificar(respuesta);
        JsonObject raiz = JsonParser.parseString(respuesta.body()).getAsJsonObject();
        JsonArray documentos = raiz.getAsJsonArray("documentos");
        return documentos == null ? new JsonArray() : documentos;
    }

    /** POST: inserta un documento. */
    public void insertar(String coleccion, Object dato) throws IOException, InterruptedException {
        JsonObject cuerpo = new JsonObject();
        cuerpo.addProperty("coleccion", coleccion);
        cuerpo.add("datos", gson.toJsonTree(dato));
        enviar("POST", cuerpo);
    }

    /** PUT: actualiza los documentos que cumplan el filtro. */
    public void actualizar(String coleccion, JsonObject filtro, JsonObject datos)
            throws IOException, InterruptedException {
        JsonObject cuerpo = new JsonObject();
        cuerpo.addProperty("coleccion", coleccion);
        cuerpo.add("filtro", filtro);
        cuerpo.add("datos", datos);
        enviar("PUT", cuerpo);
    }

    /** DELETE: elimina los documentos que cumplan el filtro. */
    public void eliminar(String coleccion, JsonObject filtro) throws IOException, InterruptedException {
        JsonObject cuerpo = new JsonObject();
        cuerpo.addProperty("coleccion", coleccion);
        cuerpo.add("filtro", filtro);
        enviar("DELETE", cuerpo);
    }

    private void enviar(String metodo, JsonObject cuerpo) throws IOException, InterruptedException {
        HttpRequest peticion = HttpRequest.newBuilder(URI.create(URL))
                .timeout(Duration.ofSeconds(15))
                .header("Content-Type", "application/json; charset=UTF-8")
                .header("Accept", "application/json")
                .method(metodo, HttpRequest.BodyPublishers.ofString(gson.toJson(cuerpo)))
                .build();
        verificar(http.send(peticion, HttpResponse.BodyHandlers.ofString()));
    }

    private void verificar(HttpResponse<String> respuesta) throws IOException {
        if (respuesta.statusCode() / 100 != 2) {
            throw new IOException("La API respondió " + respuesta.statusCode() + ": " + respuesta.body());
        }
    }
}