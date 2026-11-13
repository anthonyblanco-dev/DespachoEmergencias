package com.cenfotec.despacho;

import com.cenfotec.despacho.datos.CargadorArchivo;
import com.cenfotec.despacho.modelo.Incidente;
import com.cenfotec.despacho.modelo.Unidad;
import com.cenfotec.despacho.servicio.ServicioDespacho;
import com.cenfotec.despacho.datos.CargadorNube;
import com.cenfotec.despacho.datos.DatosIniciales;

import java.io.IOException;
import java.util.Scanner;

public class Main {

    public static void main(String[] args) {
        ServicioDespacho servicio = new ServicioDespacho();

        DatosIniciales datos = null;
        String origen = "";

        try {
            CargadorNube nube = new CargadorNube();
            int subidos = nube.sembrarSiVacio(CargadorArchivo.cargar("/datos_iniciales.json"));
            if (subidos > 0) {
                System.out.println("Se subieron " + subidos + " documentos iniciales a la nube.");
            }
            datos = nube.cargar();
            origen = "la nube (MongoDB)";
        } catch (Exception e) {
            System.out.println("No se pudo usar la nube (" + e.getMessage() + "). Se usará el archivo local.");
        }

        if (datos == null) {
            try {
                datos = CargadorArchivo.cargar("/datos_iniciales.json");
                origen = "el archivo local";
            } catch (IOException e) {
                System.out.println("No se pudieron cargar los datos: " + e.getMessage());
                return;
            }
        }

        servicio.cargar(datos);
        System.out.println("Datos iniciales cargados desde " + origen + ": "
                + servicio.cantidadActivos() + " incidentes, "
                + servicio.getUnidades().size() + " unidades, "
                + servicio.llamadasPendientes() + " llamadas en cola.");

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
                    System.out.print("Tipo (ACCIDENTE, INCENDIO, MEDICA, ROBO...): ");
                    String tipo = sc.nextLine().trim().toUpperCase();
                    System.out.print("Descripción: ");
                    String desc = sc.nextLine().trim();
                    int prioridad = leerEntero(sc, "Prioridad (1 crítica - 5 baja): ");
                    Incidente nuevo = servicio.crearIncidente(tipo, desc, prioridad, 0);
                    System.out.println("Registrado: " + nuevo);
                }
                case 5 -> {
                    int id = leerEntero(sc, "ID del incidente a reclasificar: ");
                    int p = leerEntero(sc, "Nueva prioridad (1-5): ");
                    System.out.println(servicio.reclasificar(id, p) ? "Reclasificado." : "No existe ese incidente.");
                }
                case 6 -> {
                    int id = leerEntero(sc, "ID del incidente a cerrar: ");
                    Incidente cerrado = servicio.cerrarIncidente(id);
                    System.out.println(cerrado == null ? "No existe ese incidente." : "Cerrado: " + cerrado);
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
                        System.out.print("Tipo de emergencia: ");
                        String tipo = sc.nextLine().trim().toUpperCase();
                        int prioridad = leerEntero(sc, "Prioridad (1-5): ");
                        System.out.println("Incidente creado: " + servicio.atenderSiguienteLlamada(tipo, prioridad));
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
                case 0 -> System.out.println("Hasta luego.");
                default -> System.out.println("Opción no válida.");
            }
        } while (opcion != 0);
    }

    private static void mostrarMenu() {
        System.out.println("\n===== SISTEMA DE DESPACHO DE EMERGENCIAS =====");
        System.out.println("1. Listar incidentes activos");
        System.out.println("2. Ver el incidente más urgente");
        System.out.println("3. Buscar incidente por ID");
        System.out.println("4. Registrar incidente nuevo");
        System.out.println("5. Reclasificar prioridad");
        System.out.println("6. Cerrar incidente (borrar)");
        System.out.println("7. Recibir llamada (encolar)");
        System.out.println("8. Atender siguiente llamada");
        System.out.println("9. Mostrar árbol Rojo-Negro");
        System.out.println("10. Ver unidades");
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
}
