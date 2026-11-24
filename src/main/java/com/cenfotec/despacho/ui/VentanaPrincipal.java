package com.cenfotec.despacho.ui;

import com.cenfotec.despacho.Arranque;
import com.cenfotec.despacho.servicio.ServicioDespacho.ResultadoDespacho;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/** Ventana principal: una pestaña por cada estructura de datos, una para la gestión diaria y una para las colas. */
public class VentanaPrincipal extends JFrame {

    private final List<Refrescable> paneles = new ArrayList<>();
    private final JTabbedPane pestanas = new JTabbedPane();
    private final PanelMapa panelMapa;
    private final Arranque.Resultado sistema;
    private final AtomicBoolean refrescoPendiente = new AtomicBoolean(false);

    public VentanaPrincipal(Arranque.Resultado sistema) {
        super("Sistema de despacho de emergencias");
        this.sistema = sistema;
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);

        PanelIncidentes incidentes = new PanelIncidentes(sistema.servicio(), this::refrescarTodo, this::alDespachar);
        PanelArbolRN arbol = new PanelArbolRN(sistema.servicio());
        panelMapa = new PanelMapa(sistema.servicio());
        PanelHash hash = new PanelHash(sistema.servicio());
        PanelHistorial historial = new PanelHistorial(sistema.servicio());
        PanelMensajeria mensajeria = new PanelMensajeria(sistema);

        agregar("Incidentes y unidades", incidentes, incidentes);
        agregar("Árbol Rojo-Negro", arbol, arbol);
        agregar("Mapa (grafo)", panelMapa, panelMapa);
        agregar("Tabla hash", hash, hash);
        agregar("Historial (árbol B+)", historial, historial);
        agregar("Mensajería (colas)", mensajeria, mensajeria);

        String persistencia = sistema.hayNube()
                ? "Los cambios se guardan en la nube en segundo plano."
                : "Sin conexión a la nube: los cambios de esta sesión solo viven en memoria.";
        JLabel estado = new JLabel("Datos cargados desde " + sistema.origen() + ". " + persistencia);
        estado.setBorder(BorderFactory.createEmptyBorder(4, 10, 4, 10));

        add(pestanas, BorderLayout.CENTER);
        add(estado, BorderLayout.SOUTH);
        pestanas.addChangeListener(e -> refrescarTodo());

        // Cuando un componente cambia algo en segundo plano (despacho automático, etc.) se redibuja todo.
        sistema.registro().setAlRecibir(this::pedirRefresco);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                cerrarOrdenadamente();
            }
        });

        refrescarTodo();
        setSize(1180, 740);
        setLocationRelativeTo(null);
    }

    private void agregar(String titulo, java.awt.Component componente, Refrescable panel) {
        pestanas.addTab(titulo, componente);
        paneles.add(panel);
    }

    /** Vuelve a leer el estado del servicio en todos los paneles (siempre en el hilo de la interfaz). */
    public void refrescarTodo() {
        for (Refrescable p : paneles) p.refrescar();
    }

    /** Pide un refresco desde cualquier hilo; varios avisos seguidos se juntan en uno solo. */
    private void pedirRefresco() {
        if (refrescoPendiente.compareAndSet(false, true)) {
            SwingUtilities.invokeLater(() -> {
                refrescoPendiente.set(false);
                refrescarTodo();
            });
        }
    }

    /** Antes de cerrar espera a que se guarden los cambios pendientes en la nube. */
    private void cerrarOrdenadamente() {
        setTitle("Guardando cambios pendientes...");
        sistema.cerrar();
        dispose();
        System.exit(0);
    }

    /** Tras un despacho, muestra la ruta de la unidad en el mapa. */
    private void alDespachar(ResultadoDespacho r) {
        panelMapa.mostrarRuta(r.ruta(), "Unidad " + r.unidad().getPlaca() + " hacia el incidente #"
                + r.incidente().getId() + "  |  ruta: " + r.ruta() + "  |  tiempo: " + r.minutos() + " min");
        refrescarTodo();
        pestanas.setSelectedComponent(panelMapa);
    }
}
