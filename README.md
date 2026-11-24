# Sistema de Despacho de Emergencias

Proyecto final de **Estructuras de Datos II** (CENFOTEC). Simula un centro de despacho: recibe llamadas,
las convierte en incidentes priorizados y envía la unidad (ambulancia, bomberos o policía) disponible más cercana.

Documentación para la entrega en [`docs/`](docs/): [informe técnico](docs/INFORME_TECNICO.md),
[guion de la presentación](docs/GUION_PRESENTACION.md) y [lista de verificación contra el enunciado](docs/CHECKLIST_REVISION.md).

## Estructuras de datos y para qué se usan

| Estructura | Archivo | Uso en el sistema |
|---|---|---|
| Árbol Rojo-Negro | `estructuras/ArbolRojoNegro.java` | Incidentes activos ordenados por prioridad y hora; el más urgente se obtiene en O(log n) |
| Tabla hash dinámica | `estructuras/TablaHashDinamica.java` | Índice de incidentes por ID y de unidades por placa; duplica su capacidad si el factor de carga supera 0.75 |
| Árbol B+ | `estructuras/ArbolBMas.java` | Historial de incidentes cerrados indexado por fecha y hora; consultas por rango recorriendo hojas enlazadas |
| Grafo + Dijkstra | `estructuras/Grafo.java` | Mapa de la ciudad (15 intersecciones, 25 calles con tiempo en minutos); ruta más corta de la unidad al incidente |
| Cola circular | `estructuras/ColaLlamadas.java` | Llamadas entrantes, atendidas en orden de llegada |

## Arquitectura por componentes

```
                    ┌────────────────────┐
  Interfaz Swing ──▶│  ServicioDespacho  │──publica──▶ BusMensajes (una cola + un hilo por suscriptor)
  Consola        ──▶│  (núcleo, estructuras)         │
                    └────────────────────┘           ├──▶ Persistencia  ──▶ base de datos en la nube
                                                     ├──▶ Despacho automático ──▶ vuelve a llamar al servicio
                                                     └──▶ Registro de eventos ──▶ interfaz (se redibuja sola)
```

- `mensajeria/`: `BusMensajes` (publicación/suscripción con colas bloqueantes) y los mensajes inmutables (`Mensaje`).
- `componentes/`: cada componente implementa `Componente` y solo conoce el bus, nunca a los demás.
- `servicio/ServicioDespacho`: núcleo; anuncia cada cambio por el bus.
- `ui/`: interfaz Swing. `Main` (consola) y `MainGrafico` (ventana) arrancan con `Arranque`.

## Cómo ejecutar

Requiere Java 25 y Maven.

```bash
mvn package
# Interfaz gráfica (Swing), con el jar ejecutable que genera Maven
java -jar target/DespachoEmergencias-1.0-SNAPSHOT-jar-with-dependencies.jar
```

En IntelliJ basta con ejecutar `MainGrafico` (ventana) o `Main` (consola).

## Interfaz gráfica

Una pestaña por cada estructura y una para las colas:

- **Incidentes y unidades**: crear, reclasificar, despachar y cerrar incidentes; recibir y atender llamadas.
- **Árbol Rojo-Negro**: el árbol de incidentes activos con colores y prioridad.
- **Mapa (grafo)**: intersecciones, calles, incidentes, unidades y la ruta de despacho; ruta más corta, BFS y DFS.
- **Tabla hash**: cubetas, cadenas de colisión, factor de carga y redimensionamientos.
- **Historial (árbol B+)**: el árbol, las hojas enlazadas y la consulta por rango de fechas.
- **Mensajería (colas)**: estado de cada cola, despacho automático, simulador de llamadas y registro de eventos.

## Consola

Opciones 1 a 19: incidentes, llamadas, unidades, árbol Rojo-Negro (9), mapa y rutas (11, 12), despacho (13),
historial por fechas (14), árbol B+ (15), tabla hash (16), estado del bus (17), despacho automático (18) y
registro de eventos (19).

## Datos y persistencia

- Al arrancar se leen incidentes, unidades y llamadas de la base de datos en la nube (MongoDB vía API). Si no hay
  conexión se usa `src/main/resources/datos_iniciales.json` y los cambios solo viven en memoria.
  El mapa de la ciudad siempre sale del archivo local (es configuración fija).
- Con la nube conectada, cada cambio (incidente nuevo, reclasificación, despacho, cierre, llamada recibida o atendida,
  estado de una unidad) se guarda en segundo plano por el componente de persistencia, con 3 intentos por escritura.
- Al cerrar el programa se espera a que terminen los guardados pendientes.
- Colecciones: `despacho_unidades`, `despacho_incidentes`, `despacho_llamadas`.
- Al volver a cargar se reconstruye todo: los incidentes cerrados vuelven al árbol B+, las llamadas ya atendidas no
  regresan a la cola y cada incidente asignado recuerda su unidad.

## Pruebas

Programas de verificación en `src/main/java/com/cenfotec/despacho/pruebas/` (no requieren red):

| Clase | Qué comprueba |
|---|---|
| `PruebaArbolRojoNegro` | 2000 inserciones y mitad de borrados manteniendo las propiedades del árbol |
| `PruebaArbolBMas` | Órdenes 3 a 8 contra un `TreeMap`: inserción, búsqueda, rango y borrado |
| `PruebaTablaHash` | Crecimiento automático y comportamiento igual a un `HashMap` |
| `PruebaGrafo` | Dijkstra, BFS y DFS con resultados conocidos |
| `PruebaDespacho` | Flujo completo: despachar, liberar unidad, cerrar al historial |
| `PruebaBus` | Orden de entrega, filtrado por tipo, errores en manejadores, asincronía, cuatro publicadores a la vez, cierre |
| `PruebaPersistencia` | Guardar, "reiniciar" y recargar con un almacén en memoria; reintentos, fallos y despacho automático |

`ProbarApi` y `LimpiarNube` son herramientas para la API en la nube, no pruebas automáticas.

## Integración continua

El flujo `.github/workflows/ci.yml` compila con Java 25, ejecuta las siete pruebas y publica el jar ejecutable
como artefacto en cada push a `main`.
