package com.cenfotec.despacho;

import com.cenfotec.despacho.componentes.Componente;
import com.cenfotec.despacho.estructuras.Grafo;
import com.cenfotec.despacho.mensajeria.BusMensajes;
import com.cenfotec.despacho.modelo.Incidente;
import com.cenfotec.despacho.modelo.Unidad;
import com.cenfotec.despacho.servicio.ServicioDespacho;

import java.io.IOException;
import java.util.List;
import java.util.Scanner;

/** Interfaz de consola. La interfaz gráfica arranca desde {@link MainGrafico}. */
public class Main {

    public static void main(String[] args) {
        Arranque.Resultado sistema;
        try {
            sistema = Arranque.iniciar();
        } catch (IOException e) {
            System.out.println("No se pudieron cargar los datos: " + e.getMessage());
            return;
        }
        ServicioDespacho servicio = sistema.servicio();

        Scanner sc = new Scanner(System.in);
        int opcion;
        do {
            mostrarMenu();
            opcion = leerEntero(sc, "Opción: ");
            switch (opcion) {
                case 1 -> {
                    System.out.println("--- Incidentes activos (de más a menos urgente) ---");
                    servicio.listarActivos().forEach(System.out::println);
                }
                case 2 -> {
                    Incidente i = servicio.masUrgente();
                    System.out.println(i == null ? "No hay incidentes activos." : "Más urgente: " + i);
                }
                case 3 -> {
                    int id = leerEntero(sc, "ID del incidente: ");
                    Incidente i = servicio.buscarIncidente(id);
                    System.out.println(i == null ? "No se encontró el incidente." : "Encontrado: " + i);
                }
                case 4 -> {
                    System.out.print("Tipo (ACCIDENTE, INCENDIO, MEDICA, ROBO, RUIDO): ");
                    String tipo = sc.nextLine().trim().toUpperCase();
                    System.out.print("Descripción: ");
                    String desc = sc.nextLine().trim();
                    int prioridad = leerEnRango(sc, "Prioridad (1 crítica - 5 baja): ", 1, 5);
                    int interseccion = leerInterseccion(sc, servicio, "Intersección del incidente: ");
                    Incidente nuevo = servicio.crearIncidente(tipo, desc, prioridad, interseccion);
                    System.out.println("Registrado: " + nuevo);
                }
                case 5 -> {
                    int id = leerEntero(sc, "ID del incidente a reclasificar: ");
                    int p = leerEnRango(sc, "Nueva prioridad (1-5): ", 1, 5);
                    System.out.println(servicio.reclasificar(id, p) ? "Reclasificado." : "No existe ese incidente.");
                }
                case 6 -> {
                    int id = leerEntero(sc, "ID del incidente a cerrar: ");
                    Incidente cerrado = servicio.cerrarIncidente(id);
                    System.out.println(cerrado == null ? "No existe ese incidente."
                            : "Cerrado y enviado al historial: " + cerrado);
                }
                case 7 -> {
                    System.out.print("Teléfono: ");
                    String tel = sc.nextLine().trim();
                    System.out.print("Ubicación referida: ");
                    String ubic = sc.nextLine().trim();
                    System.out.println("En cola: " + servicio.recibirLlamada(tel, ubic));
                }
                case 8 -> {
                    if (servicio.llamadasPendientes() == 0) {
                        System.out.println("No hay llamadas pendientes.");
                    } else {
                        System.out.println("Llamada a atender: " + servicio.verSiguienteLlamada());
                        System.out.print("Tipo de emergencia: ");
                        String tipo = sc.nextLine().trim().toUpperCase();
                        int prioridad = leerEnRango(sc, "Prioridad (1-5): ", 1, 5);
                        int interseccion = leerInterseccion(sc, servicio, "Intersección del incidente: ");
                        System.out.println("Incidente creado: "
                                + servicio.atenderSiguienteLlamada(tipo, prioridad, interseccion));
                    }
                }
                case 9 -> {
                    System.out.println("--- Árbol Rojo-Negro (girado 90°; R = rojo, N = negro) ---");
                    servicio.mostrarArbol();
                    System.out.println("Nodos: " + servicio.cantidadActivos()
                            + " | Altura: " + servicio.alturaArbol()
                            + " | Válido: " + servicio.arbolValido());
                }
                case 10 -> {
                    System.out.println("--- Unidades ---");
                    for (Unidad u : servicio.getUnidades()) System.out.println(u);
                }
                case 11 -> {
                    System.out.println("--- Mapa de la ciudad (vecino(tiempo en min)) ---");
                    servicio.getMapa().imprimirListaAdyacencia();
                }
                case 12 -> {
                    int desde = leerInterseccion(sc, servicio, "Intersección de origen: ");
                    int hasta = leerInterseccion(sc, servicio, "Intersección de destino: ");
                    Grafo.ResultadoDijkstra r = servicio.getMapa().dijkstra(desde);
                    if (!r.alcanzable(hasta)) {
                        System.out.println("No hay ruta entre esas intersecciones.");
                    } else {
                        System.out.println("Ruta: " + r.rutaA(hasta) + " | tiempo: " + r.distanciaA(hasta) + " min");
                    }
                }
                case 13 -> {
                    int id = leerEntero(sc, "ID del incidente a despachar: ");
                    ServicioDespacho.ResultadoDespacho r = servicio.despachar(id);
                    if (r.exito()) {
                        System.out.println(r.mensaje() + " Ruta: " + r.ruta() + " | tiempo estimado: " + r.minutos() + " min");
                    } else {
                        System.out.println(r.mensaje());
                    }
                }
                case 14 -> {
                    System.out.print("Desde (2026-11-10 o 2026-11-10T08:30:00): ");
                    String desde = sc.nextLine().trim();
                    System.out.print("Hasta: ");
                    String hasta = sc.nextLine().trim();
                    try {
                        List<Incidente> lista = servicio.consultarHistorial(desde, hasta);
                        System.out.println("--- Historial entre fechas (" + lista.size() + ") ---");
                        lista.forEach(System.out::println);
                    } catch (IllegalArgumentException e) {
                        System.out.println(e.getMessage());
                    }
                }
                case 15 -> {
                    System.out.println("--- Árbol B+ del historial (orden " + servicio.ordenHistorial() + ") ---");
                    servicio.mostrarHistorial();
                    System.out.println("Cerrados: " + servicio.cantidadHistorial()
                            + " | Altura: " + servicio.alturaHistorial()
                            + " | Válido: " + servicio.historialValido());
                }
                case 16 -> {
                    System.out.println("--- Tabla hash dinámica (índice de incidentes por ID) ---");
                    servicio.mostrarIndice();
                }
                case 17 -> {
                    System.out.println("--- Bus de mensajes (colas asincrónicas) ---");
                    System.out.println("Mensajes publicados: " + sistema.bus().totalPublicados());
                    for (BusMensajes.Estadistica e : sistema.bus().estadisticas()) {
                        System.out.printf("%-22s recibidos %3d | procesados %3d | pendientes %3d | errores %d%n",
                                e.nombre(), e.recibidos(), e.procesados(), e.pendientes(), e.errores());
                    }
                    for (Componente c : sistema.componentes()) {
                        System.out.println(c.nombre() + ": " + c.estado());
                    }
                    if (!sistema.hayNube()) System.out.println("Sin nube: los cambios solo viven en memoria.");
                }
                case 18 -> {
                    boolean nuevo = !sistema.despachoAutomatico().isActivo();
                    sistema.despachoAutomatico().setActivo(nuevo);
                    System.out.println("Despacho automático " + (nuevo ? "ACTIVADO" : "APAGADO")
                            + (nuevo ? ": cada incidente nuevo recibirá su unidad sin intervención." : "."));
                }
                case 19 -> {
                    System.out.println("--- Últimos eventos (más reciente primero) ---");
                    sistema.registro().recientes().stream().limit(15).forEach(System.out::println);
                }
                case 0 -> System.out.println("Guardando cambios pendientes...");
                default -> System.out.println("Opción no válida.");
            }
        } while (opcion != 0);
        sistema.cerrar();
        System.out.println("Hasta luego.");
    }

    private static void mostrarMenu() {
        System.out.println("\n===== SISTEMA DE DESPACHO DE EMERGENCIAS =====");
        System.out.println("1. Listar incidentes activos");
        System.out.println("2. Ver el incidente más urgente");
        System.out.println("3. Buscar incidente por ID");
        System.out.println("4. Registrar incidente nuevo");
        System.out.println("5. Reclasificar prioridad");
        System.out.println("6. Cerrar incidente (pasa al historial)");
        System.out.println("7. Recibir llamada (encolar)");
        System.out.println("8. Atender siguiente llamada");
        System.out.println("9. Mostrar árbol Rojo-Negro");
        System.out.println("10. Ver unidades");
        System.out.println("11. Ver mapa de la ciudad (lista de adyacencia)");
        System.out.println("12. Ruta más corta entre dos intersecciones");
        System.out.println("13. Despachar unidad a un incidente");
        System.out.println("14. Consultar historial entre dos fechas");
        System.out.println("15. Mostrar árbol B+ del historial");
        System.out.println("16. Mostrar tabla hash dinámica");
        System.out.println("17. Estado del bus de mensajes y componentes");
        System.out.println("18. Activar/apagar despacho automático");
        System.out.println("19. Ver últimos eventos del sistema");
        System.out.println("0. Salir");
    }

    private static int leerEntero(Scanner sc, String mensaje) {
        while (true) {
            System.out.print(mensaje);
            try {
                return Integer.parseInt(sc.nextLine().trim());
            } catch (NumberFormatException e) {
                System.out.println("Escribe un número válido.");
            }
        }
    }

    private static int leerEnRango(Scanner sc, String mensaje, int minimo, int maximo) {
        while (true) {
            int n = leerEntero(sc, mensaje);
            if (n >= minimo && n <= maximo) return n;
            System.out.println("Debe estar entre " + minimo + " y " + maximo + ".");
        }
    }

    private static int leerInterseccion(Scanner sc, ServicioDespacho servicio, String mensaje) {
        while (true) {
            int id = leerEntero(sc, mensaje);
            if (servicio.getMapa().contieneVertice(id)) return id;
            System.out.println("Esa intersección no existe (usa un número del 1 al "
                    + servicio.getMapa().cantidadVertices() + ").");
        }
    }
}
