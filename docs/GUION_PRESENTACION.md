# Guion de la presentación y demostración (7 a 8 minutos)

Antes de empezar: abre la ventana (`MainGrafico`) con internet, deja la consola de IntelliJ visible para que se vea
`Datos iniciales cargados desde la nube (MongoDB)`, y abre en el navegador la pestaña **Actions** del repositorio
y la base de datos en la nube.

## Guion

| Min | Qué haces | Qué dices |
|---|---|---|
| 0:00 | Pantalla de inicio | "Es un centro de despacho de emergencias. Cada estructura del curso resuelve una parte: orden de urgencia, búsqueda por id, historial por fechas, rutas y llamadas en espera." |
| 0:40 | Muestra el mensaje de la consola | "Los datos vienen de la nube. Si no hubiera internet, usa un archivo local y sigue funcionando." |
| 1:00 | Pestaña **Incidentes**: señala el orden. Pestaña **Árbol Rojo-Negro** | "Los incidentes viven en un árbol Rojo-Negro ordenado por prioridad y hora. El más urgente es el del extremo izquierdo, y se obtiene en O(log n)." |
| 1:40 | Reclasifica un incidente a P1 y vuelve al árbol | "Cambiar la prioridad cambia la clave, así que sale y vuelve a entrar. El árbol se rebalancea y sigue siendo válido." |
| 2:10 | **Recibir llamada** y luego **Atender siguiente** (elige tipo, prioridad e intersección) | "Las llamadas esperan en una cola circular y se atienden en orden de llegada." |
| 2:40 | Pestaña **Tabla hash** | "El índice por id es una tabla hash dinámica: cuando el factor de carga pasa de 0.75 duplica su tamaño y reubica las claves." Si quieres que se vea crecer, crea unos incidentes más antes. |
| 3:10 | **Despachar unidad** sobre un incidente | "Dijkstra calcula el tiempo desde el incidente a cada unidad libre del tipo correcto y envía la más cercana." (Se abre el mapa con la ruta naranja.) |
| 3:50 | Pestaña **Mapa**: ruta más corta entre dos puntos, BFS | "El mapa es un grafo ponderado: 15 intersecciones, 25 calles con tiempo en minutos." |
| 4:20 | **Cerrar** un incidente y abre **Historial (B+)** | "Al cerrar, la unidad se libera y el incidente pasa a un árbol B+. Las hojas están enlazadas." |
| 4:50 | En el historial, consulta un rango de fechas | "El rango se resuelve buscando el inicio y recorriendo hojas hacia la derecha. Es lo que hacen los índices de las bases de datos." |
| 5:20 | Pestaña **Mensajería**: activa el despacho automático y pulsa **Simular 5 llamadas**; crea un incidente | "Los módulos no se llaman entre sí: se comunican por colas. El servicio publica un mensaje y cada componente lo procesa en su hilo: persistencia, despacho automático y registro. Quien publica nunca espera." |
| 6:10 | Señala las colas y el registro de eventos | "Aquí se ve la cola de cada componente y el flujo de mensajes." |
| 6:30 | Cierra la ventana y vuelve a abrirla | "Lo que hice se guardó en la nube en segundo plano. Al reabrir, el estado es el mismo: las unidades siguen ocupadas, el historial conserva lo cerrado y las llamadas ya atendidas no regresan." |
| 7:15 | Pestaña **Actions** de GitHub | "Cada push compila, ejecuta las siete pruebas y publica el jar. Aquí está en verde." |
| 7:40 | Cierre | "Cada estructura tiene un papel concreto y el diseño por componentes permitió agregar la nube y el despacho automático sin tocar el núcleo." |

## Plan B

- **Sin internet:** el programa cae solo al archivo local; la barra de estado de la ventana dirá "Sin conexión a la nube".
  Di que esa es justamente la tolerancia a fallos, y muestra la prueba `PruebaPersistencia` (reiniciar y recargar contra un almacén en memoria).
- **El CI no está verde:** muestra la última ejecución correcta o corre las pruebas en IntelliJ.
- **Un dato raro en la nube:** `LimpiarNube` la deja vacía y `Main` la vuelve a sembrar.

## Preguntas probables

| Pregunta | Respuesta corta |
|---|---|
| ¿Por qué Rojo-Negro y no AVL? | Ambos garantizan O(log n). El Rojo-Negro rota menos al insertar y borrar, y aquí hay muchas altas y bajas de incidentes. |
| ¿Por qué un B+ para el historial y no otro Rojo-Negro? | El historial se consulta por rangos de fecha. En el B+ las hojas están enlazadas y el rango es un recorrido; además es la estructura de los índices de bases de datos. |
| ¿Qué pasa si crece mucho la tabla hash? | Al pasar el factor de carga 0.75 duplica su capacidad y reubica las claves; el costo se reparte entre las inserciones (amortizado O(1)). |
| ¿Por qué Dijkstra? | Los tiempos de las calles no son negativos y necesito la distancia desde el incidente hacia todas las unidades en una sola pasada. |
| ¿Cómo evitas errores entre hilos? | Los métodos del servicio son `synchronized`, cada suscriptor tiene su cola y su hilo, y los mensajes llevan copias inmutables. |
| ¿Y si falla la nube? | Cada escritura se reintenta tres veces; si falla, se cuenta y se sigue. Si no hay nube al arrancar, se trabaja con el archivo local. |
| ¿Es un servicio de colas real? | Es un bus propio con colas bloqueantes y un hilo por componente. Está diseñado para cambiarlo por un broker como RabbitMQ sin tocar los componentes. |
| ¿Cómo sabes que el árbol sigue siendo válido? | `esValido()` comprueba las propiedades del árbol y las pruebas lo llaman cientos de veces durante inserciones y borrados. |
