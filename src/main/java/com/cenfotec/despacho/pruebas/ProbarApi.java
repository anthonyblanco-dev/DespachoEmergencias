package com.cenfotec.despacho.pruebas;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.time.Duration;

public class ProbarApi {

    private static final String BASE = "https://paginas-web-cr.com/Api/";
    private static final String COLECCION = "despacho_prueba";
    private static final HttpClient CLIENTE = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public static void main(String[] args) {
        String mongo = BASE + "apis/mongodb.php";

        System.out.println("=== 1. GET con index.php (como dice el material) ===");
        enviar("GET", BASE + "index.php?coleccion=" + COLECCION, null);

        System.out.println("=== 2. GET con apis/mongodb.php ===");
        enviar("GET", mongo + "?coleccion=" + COLECCION, null);

        System.out.println("=== 3. POST (insertar) ===");
        enviar("POST", mongo,
                "{\"coleccion\":\"" + COLECCION + "\",\"datos\":{\"placa\":\"TEST-001\",\"tipo\":\"AMBULANCIA\"}}");

        System.out.println("=== 4. GET con filtro, después de insertar ===");
        enviar("GET", mongo + "?coleccion=" + COLECCION + "&placa=TEST-001", null);

        System.out.println("=== 5. PUT (actualizar) ===");
        enviar("PUT", mongo,
                "{\"coleccion\":\"" + COLECCION + "\",\"filtro\":{\"placa\":\"TEST-001\"},\"datos\":{\"estado\":\"OCUPADA\"}}");

        System.out.println("=== 6. DELETE (eliminar) ===");
        enviar("DELETE", mongo,
                "{\"coleccion\":\"" + COLECCION + "\",\"filtro\":{\"placa\":\"TEST-001\"}}");

        System.out.println("=== 7. GET final (debería salir vacío) ===");
        enviar("GET", mongo + "?coleccion=" + COLECCION, null);
    }

    private static void enviar(String metodo, String url, String cuerpo) {
        System.out.println(metodo + " " + url);
        if (cuerpo != null) System.out.println("Cuerpo: " + cuerpo);
        try {
            HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(15))
                    .header("Accept", "application/json");
            if (cuerpo == null) {
                b.GET();
            } else {
                b.header("Content-Type", "application/json; charset=UTF-8");
                b.method(metodo, BodyPublishers.ofString(cuerpo));
            }
            HttpResponse<String> r = CLIENTE.send(b.build(), HttpResponse.BodyHandlers.ofString());
            String tipo = r.headers().firstValue("Content-Type").orElse("(sin tipo)");
            String texto = r.body();
            if (texto.length() > 600) texto = texto.substring(0, 600) + "...(recortado)";
            System.out.println("Estado: " + r.statusCode() + " | Tipo: " + tipo);
            System.out.println("Respuesta: " + texto);
        } catch (Exception e) {
            System.out.println("ERROR: " + e);
        }
        System.out.println();
    }
}
