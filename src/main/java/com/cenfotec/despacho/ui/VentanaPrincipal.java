package com.cenfotec.despacho.ui;

import com.cenfotec.despacho.servicio.ServicioDespacho;
import com.cenfotec.despacho.servicio.ServicioDespacho.ResultadoDespacho;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JTabbedPane;
import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.List;

/** Ventana principal: una pestaña por cada estructura de datos y una para la gestión diaria. */
public class VentanaPrincipal extends JFrame {

    private final List<Refrescable> paneles = new ArrayList<>();
    private final JTabbedPane pestanas = new JTabbedPane();
    private final PanelMapa panelMapa;

    public VentanaPrincipal(ServicioDespacho servicio, String origenDatos) {
        super("Sistema de despacho de emergencias");
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        PanelIncidentes incidentes = new PanelIncidentes(servicio, this::refrescarTodo, this::alDespachar);
        PanelArbolRN arbol = new PanelArbolRN(servicio);
        panelMapa = new PanelMapa(servicio);
        PanelHash hash = new PanelHash(servicio);
        PanelHistorial historial = new PanelHistorial(servicio);

        agregar("Incidentes y unidades", incidentes, incidentes);
        agregar("Árbol Rojo-Negro", arbol, arbol);
        agregar("Mapa (grafo)", panelMapa, panelMapa);
        agregar("Tabla hash", hash, hash);
        agregar("Historial (árbol B+)", historial, historial);

        JLabel estado = new JLabel("Datos cargados desde " + origenDatos
                + ". Los cambios de esta sesión viven en memoria.");
        estado.setBorder(BorderFactory.createEmptyBorder(4, 10, 4, 10));

        add(pestanas, BorderLayout.CENTER);
        add(estado, BorderLayout.SOUTH);
        pestanas.addChangeListener(e -> refrescarTodo());

        refrescarTodo();
        setSize(1180, 740);
        setLocationRelativeTo(null);
    }

    private void agregar(String titulo, java.awt.Component componente, Refrescable panel) {
        pestanas.addTab(titulo, componente);
        paneles.add(panel);
    }

    /** Vuelve a leer el estado del servicio en todos los paneles. */
    public void refrescarTodo() {
        for (Refrescable p : paneles) p.refrescar();
    }

    /** Tras un despacho, muestra la ruta de la unidad en el mapa. */
    private void alDespachar(ResultadoDespacho r) {
        panelMapa.mostrarRuta(r.ruta(), "Unidad " + r.unidad().getPlaca() + " hacia el incidente #"
                + r.incidente().getId() + "  |  ruta: " + r.ruta() + "  |  tiempo: " + r.minutos() + " min");
        refrescarTodo();
        pestanas.setSelectedComponent(panelMapa);
    }
}
