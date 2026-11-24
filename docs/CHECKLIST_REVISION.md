# Lista de revisión contra el enunciado

Para revisar paso a paso: cada requisito del enunciado, dónde está en el repositorio y cómo comprobarlo.
Marca la casilla cuando lo hayas visto funcionar tú mismo.

Ruta base del código: `src/main/java/com/cenfotec/despacho/`

## Antes de empezar

- [ ] `git pull` en tu copia local (si no tienes cambios propios, no habrá conflictos).
- [ ] Abre el proyecto en IntelliJ y recarga Maven. El `pom.xml` apunta a Java 25.
- [ ] `mvn package` termina en `BUILD SUCCESS` y crea `target/DespachoEmergencias-1.0-SNAPSHOT-jar-with-dependencies.jar`.
      (Se probó el código compilando con Java 21; Maven y Java 25 no se pudieron ejecutar en mi entorno.)

## 1. Estructuras de datos avanzadas (30 %)

| Requisito | Dónde | Cómo comprobarlo | ✔ |
|---|---|---|---|
| Árbol balanceado (Rojo-Negro) | `estructuras/ArbolRojoNegro.java` | Ejecutar `PruebaArbolRojoNegro`; en la ventana, pestaña "Árbol Rojo-Negro" | ☐ |
| Árbol B / B+ | `estructuras/ArbolBMas.java` | Ejecutar `PruebaArbolBMas`; pestaña "Historial (B+)" tras cerrar incidentes | ☐ |
| Tabla hash dinámica | `estructuras/TablaHashDinamica.java` | Ejecutar `PruebaTablaHash`; pestaña "Tabla hash" (crear incidentes y ver el factor de carga y los redimensionamientos) | ☐ |
| Grafos (recorridos y camino mínimo) | `estructuras/Grafo.java` | Ejecutar `PruebaGrafo`; pestaña "Mapa": ruta más corta, BFS, DFS | ☐ |
| Estructura extra: cola circular | `estructuras/ColaLlamadas.java` | Consola, opciones de llamadas; panel de incidentes | ☐ |
| Las estructuras resuelven una necesidad real, no están de adorno | Informe, sección 3 | Leer la tabla de justificación | ☐ |
| Implementación propia de las estructuras | Árboles, tabla hash y cola: sin colecciones de Java (solo `ArrayList` como contenedor interno). `Grafo`: lista de adyacencia propia, pero usa `TreeMap`/`HashMap`/`HashSet` para guardar vértices y `PriorityQueue` en Dijkstra | Revisar los imports de `estructuras/`. Si el docente exige Dijkstra con montículo propio, avísame y lo cambio. `TreeMap`/`HashMap` en las pruebas son solo la referencia | ☐ |

## 2. Paradigma de componentes e integración (20 %)

| Requisito | Dónde | Cómo comprobarlo | ✔ |
|---|---|---|---|
| Módulos independientes | `componentes/`, `servicio/`, `datos/`, `ui/` | Informe, 2.1 y 2.2 | ☐ |
| Comunicación asincrónica por colas | `mensajeria/BusMensajes.java`, `mensajeria/Mensaje.java` | Ejecutar `PruebaBus`; pestaña "Mensajería": ver las colas moverse al simular llamadas | ☐ |
| Componentes que solo conocen el bus | `componentes/Componente*.java` | Revisar que ninguno importa a otro componente | ☐ |
| Integración real (no solo pruebas sueltas) | `Arranque.java` conecta servicio, bus y componentes | Ejecutar `MainGrafico`, activar "Despacho automático" y crear un incidente: se asigna sin pulsar nada | ☐ |
| Seguridad entre hilos | `ServicioDespacho` (`synchronized`), mensajes con `copia()` | Informe 2.5; `PruebaBus` con cuatro publicadores | ☐ |

**A confirmar con el docente:** si el bus propio cuenta como "servicio de colas" o se espera un broker externo
(RabbitMQ, etc.). Si exigen broker, hay que cambiar solo la implementación del bus.

## 3. Base de datos en la nube e integración continua (15 %)

| Requisito | Dónde | Cómo comprobarlo | ✔ |
|---|---|---|---|
| Datos persistidos en la nube | `datos/ClienteApi.java`, `datos/CargadorNube.java`, `componentes/ComponentePersistencia.java` | Ejecutar `MainGrafico` con internet; la consola dice "cargados desde la nube (MongoDB)". Crear un incidente, cerrar y volver a abrir: sigue ahí | ☐ |
| Los cambios se ven en la base de datos | Colecciones `despacho_unidades`, `despacho_incidentes`, `despacho_llamadas` | Revisar la colección en el panel de la nube tras un cambio | ☐ |
| Funciona sin nube | `Arranque.iniciar()` | Desconectar internet y abrir: usa el archivo local y lo avisa en la barra de estado | ☐ |
| Persistencia probada sin red | `pruebas/PruebaPersistencia.java` y `AlmacenMemoria.java` | Ejecutarla: guarda, reinicia, recarga, reintenta y sobrevive a fallos | ☐ |
| CI: compila | `.github/workflows/ci.yml`, paso "Compilar y empaquetar" | Pestaña **Actions** de GitHub en verde tras el push | ☐ |
| CI: pruebas | Paso "Pruebas de las estructuras de datos" | Ver el log: siete bloques `=== Prueba... ===` con `PRUEBA SUPERADA` | ☐ |
| CI: despliegue | Paso "Publicar el jar ejecutable" | En la ejecución, sección *Artifacts*: `despacho-emergencias-jar` | ☐ |

**Puntos que no pude comprobar yo:** que Actions quede en verde con Java 25 y que las escrituras reales a la API
funcionen igual que con el almacén de prueba (por eso se envían documentos completos). Revisa ambos.

## 4. Interfaz (consola o web)

| Requisito | Dónde | Cómo comprobarlo | ✔ |
|---|---|---|---|
| Interfaz de usuario | `MainGrafico` (Swing) y `Main` (consola, opciones 1 a 19) | Abrir ambas | ☐ |
| Permite ver las estructuras | `ui/Panel*.java` | Recorrer las seis pestañas | ☐ |
| Permite operar el sistema | `ui/PanelIncidentes.java` | Crear, reclasificar, despachar, cerrar, recibir y atender llamadas | ☐ |

## 5. Informe técnico (20 %)

| Contenido pedido | Dónde | ✔ |
|---|---|---|
| Introducción al problema y solución | `docs/INFORME_TECNICO.md`, sección 1 | ☐ |
| Arquitectura y diagramas UML | Sección 2 y `docs/diagramas/` (componentes, clases, secuencia) | ☐ |
| Justificación de las estructuras | Sección 3 | ☐ |
| Pruebas y análisis de resultados | Sección 8 | ☐ |
| Portada con tus datos | Líneas de nombre, carné, docente y fecha (**completar**) | ☐ |
| Capturas reales | `docs/capturas/` son renders con datos de ejemplo; **reemplázalas por capturas de tu ventana** | ☐ |

Si el docente pide Word o PDF, el `.md` se convierte fácilmente (puedo hacerlo).

## 6. Presentación y demostración (15 %)

| Requisito | Dónde | ✔ |
|---|---|---|
| Guion con demo | `docs/GUION_PRESENTACION.md` | ☐ |
| Ensayo cronometrado (7-8 min) | Tú | ☐ |
| Plan B sin internet | Misma guía | ☐ |
| Preguntas probables ensayadas | Misma guía | ☐ |

## 7. Repositorio

- [ ] Código documentado y en Git (historial de commits visible).
- [ ] `README.md` explica estructuras, arquitectura, cómo ejecutar y cómo probar.
- [ ] No hay archivos de más. `.gitignore` ya cubre `.idea/` y `target/`, pero tres archivos de `.idea/` ya estaban versionados desde antes; puedes quitarlos con `git rm -r --cached .idea`.

## Cómo ejecutar todas las pruebas desde IntelliJ

Clic derecho en cada archivo de `pruebas/` y *Run*, o con Maven:

```bash
mvn -q package
mvn -q dependency:build-classpath -Dmdep.outputFile=cp.txt
for p in PruebaGrafo PruebaTablaHash PruebaArbolBMas PruebaArbolRojoNegro PruebaDespacho PruebaBus PruebaPersistencia; do
  java -cp "target/classes:$(cat cp.txt)" com.cenfotec.despacho.pruebas.$p
done
```
