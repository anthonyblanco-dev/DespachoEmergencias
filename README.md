# Sistema de Despacho de Emergencias

Proyecto final de **Estructuras de Datos II** (CENFOTEC). Simula un centro de despacho: recibe llamadas,
las convierte en incidentes priorizados y envía la unidad (ambulancia, bomberos o policía) disponible más cercana.

## Estructuras de datos y para qué se usan

| Estructura | Archivo | Uso en el sistema |
|---|---|---|
| Árbol Rojo-Negro | `estructuras/ArbolRojoNegro.java` | Incidentes activos ordenados por prioridad y hora; el más urgente se obtiene en O(log n) |
| Tabla hash dinámica | `estructuras/TablaHashDinamica.java` | Índice de incidentes por ID y de unidades por placa; duplica su capacidad si el factor de carga supera 0.75 |
| Árbol B+ | `estructuras/ArbolBMas.java` | Historial de incidentes cerrados indexado por fecha y hora; consultas por rango recorriendo hojas enlazadas |
| Grafo + Dijkstra | `estructuras/Grafo.java` | Mapa de la ciudad (15 intersecciones, 25 calles con tiempo en minutos); ruta más corta de la unidad al incidente |
| Cola circular | `estructuras/ColaLlamadas.java` | Llamadas entrantes, atendidas en orden de llegada |

## Cómo ejecutar

Requiere Java 25 y Maven.

```bash
mvn package
# Interfaz gráfica (Swing)
java -cp target/classes:<gson.jar> com.cenfotec.despacho.MainGrafico
# o con el jar ejecutable que genera Maven
java -jar target/DespachoEmergencias-1.0-SNAPSHOT-jar-with-dependencies.jar
# Interfaz de consola
java -cp target/classes:<gson.jar> com.cenfotec.despacho.Main
```

En IntelliJ basta con ejecutar `MainGrafico` (ventana) o `Main` (consola).

## Interfaz gráfica

Una pestaña por cada estructura:

- **Incidentes y unidades**: crear, reclasificar, despachar y cerrar incidentes; recibir y atender llamadas.
- **Árbol Rojo-Negro**: el árbol de incidentes activos con colores y prioridad.
- **Mapa (grafo)**: intersecciones, calles, incidentes, unidades y la ruta de despacho; ruta más corta, BFS y DFS.
- **Tabla hash**: cubetas, cadenas de colisión, factor de carga y redimensionamientos.
- **Historial (árbol B+)**: el árbol, las hojas enlazadas y la consulta por rango de fechas.

## Datos

Al arrancar se leen los incidentes, unidades y llamadas de la base de datos en la nube (MongoDB vía API). Si no hay
conexión se usa `src/main/resources/datos_iniciales.json`. El mapa de la ciudad siempre sale del archivo local.

## Pruebas

Programas de verificación en `src/main/java/com/cenfotec/despacho/pruebas/`:

| Clase | Qué comprueba |
|---|---|
| `PruebaArbolRojoNegro` | 2000 inserciones y mitad de borrados manteniendo las propiedades del árbol |
| `PruebaArbolBMas` | Órdenes 3 a 8 contra un `TreeMap`: inserción, búsqueda, rango y borrado |
| `PruebaTablaHash` | Crecimiento automático y comportamiento igual a un `HashMap` |
| `PruebaGrafo` | Dijkstra, BFS y DFS con resultados conocidos |
| `PruebaDespacho` | Flujo completo: despachar, liberar unidad, cerrar al historial |

`ProbarApi` y `LimpiarNube` son herramientas para la API en la nube, no pruebas automáticas.

## Integración continua

El flujo `.github/workflows/ci.yml` compila con Java 25, ejecuta las pruebas y publica el jar ejecutable
como artefacto en cada push a `main`.
