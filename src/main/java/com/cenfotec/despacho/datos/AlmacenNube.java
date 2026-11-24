package com.cenfotec.despacho.datos;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.io.IOException;

/**
 * Operaciones que el sistema necesita de la base de datos en la nube.
 * {@link ClienteApi} las hace contra la API real; las pruebas usan una versión en memoria.
 */
public interface AlmacenNube {

    JsonArray listar(String coleccion) throws IOException, InterruptedException;

    void insertar(String coleccion, Object dato) throws IOException, InterruptedException;

    void actualizar(String coleccion, JsonObject filtro, JsonObject datos) throws IOException, InterruptedException;

    void eliminar(String coleccion, JsonObject filtro) throws IOException, InterruptedException;
}
