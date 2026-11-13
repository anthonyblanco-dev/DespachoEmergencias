package com.cenfotec.despacho.datos;

import com.google.gson.Gson;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;

public class CargadorArchivo {

    public static DatosIniciales cargar(String recurso) throws IOException {
        try (InputStream in = CargadorArchivo.class.getResourceAsStream(recurso)) {
            if (in == null) {
                throw new FileNotFoundException("No se encontró el recurso: " + recurso);
            }
            try (Reader lector = new InputStreamReader(in, StandardCharsets.UTF_8)) {
                return new Gson().fromJson(lector, DatosIniciales.class);
            }
        }
    }
}
