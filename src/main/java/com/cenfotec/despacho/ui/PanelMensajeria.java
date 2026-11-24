package com.cenfotec.despacho.ui;

import com.cenfotec.despacho.Arranque;
import com.cenfotec.despacho.componentes.Componente;
import com.cenfotec.despacho.mensajeria.BusMensajes;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.Timer;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.util.List;

/** Muestra el bus de mensajes: colas de cada componente, estado de cada uno y el registro de eventos. */
public class PanelMensajeria extends JPanel implements Refrescable {

    private static final String[][] LLAMADAS_DE_PRUEBA = {
            {"8801-0001", "Frente al mercado central"},
            {"8801-0002", "Barrio Escalante, casa azul"},
            {"8801-0003", "Autopista, kilómetro 12"},
            {"8801-0004", "Parque de la Paz"},
            {"8801-0005", "Escuela del centro"}
    };

    private final Arranque.Resultado sistema;
    private final JCheckBox automatico = new JCheckBox("Despacho automático de incidentes nuevos");
    private final JLabel resumen = new JLabel(" ");
    private final JLabel estadoComponentes = new JLabel(" ");
    private final DefaultTableModel modeloColas = new DefaultTableModel(
            new String[]{"Componente (hilo propio)", "Recibidos", "Procesados", "En cola", "Errores"}, 0) {
        @Override
        public boolean isCellEditable(int fila, int columna) { return false; }
    };
    private final DefaultListModel<String> modeloEventos = new DefaultListModel<>();
    private int siguienteLlamadaDePrueba = 0;

    public PanelMensajeria(Arranque.Resultado sistema) {
        this.sistema = sistema;
        setLayout(new BorderLayout());

        automatico.setSelected(sistema.despachoAutomatico().isActivo());
        automatico.addActionListener(e -> sistema.despachoAutomatico().setActivo(automatico.isSelected()));

        JButton simular = new JButton("Simular 5 llamadas");
        simular.addActionListener(e -> simularLlamadas());

        JPanel controles = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 6));
        controles.add(automatico);
        controles.add(simular);

        JPanel norte = new JPanel(new GridLayout(0, 1));
        norte.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
        norte.add(controles);
        norte.add(resumen);
        norte.add(estadoComponentes);
        add(norte, BorderLayout.NORTH);

        JTable tablaColas = new JTable(modeloColas);
        tablaColas.setRowHeight(24);
        JScrollPane scrollColas = new JScrollPane(tablaColas);
        scrollColas.setBorder(BorderFactory.createTitledBorder("Colas del bus (cada componente consume la suya)"));

        JList<String> listaEventos = new JList<>(modeloEventos);
        listaEventos.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        JScrollPane scrollEventos = new JScrollPane(listaEventos);
        scrollEventos.setBorder(BorderFactory.createTitledBorder("Registro de eventos (el más reciente arriba)"));

        JSplitPane division = new JSplitPane(JSplitPane.VERTICAL_SPLIT, scrollColas, scrollEventos);
        division.setResizeWeight(0.3);
        add(division, BorderLayout.CENTER);

        JLabel nota = new JLabel("Quien publica nunca espera: el mensaje queda en la cola de cada componente interesado, "
                + "que lo procesa en su propio hilo y en orden.");
        nota.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));
        add(nota, BorderLayout.SOUTH);

        // Las colas cambian en segundo plano: se vuelven a leer cada segundo mientras la pestaña se ve
        Timer reloj = new Timer(1000, e -> {
            if (isShowing()) refrescar();
        });
        reloj.start();
    }

    private void simularLlamadas() {
        for (int i = 0; i < 5; i++) {
            String[] datos = LLAMADAS_DE_PRUEBA[siguienteLlamadaDePrueba % LLAMADAS_DE_PRUEBA.length];
            siguienteLlamadaDePrueba++;
            sistema.servicio().recibirLlamada(datos[0], datos[1]);
        }
        refrescar();
    }

    @Override
    public void refrescar() {
        modeloColas.setRowCount(0);
        for (BusMensajes.Estadistica e : sistema.bus().estadisticas()) {
            modeloColas.addRow(new Object[]{e.nombre(), e.recibidos(), e.procesados(), e.pendientes(), e.errores()});
        }
        resumen.setText("Mensajes publicados: " + sistema.bus().totalPublicados()
                + "   |   Llamadas esperando operador: " + sistema.servicio().llamadasPendientes()
                + "   |   Datos de: " + sistema.origen());

        StringBuilder sb = new StringBuilder("<html>");
        List<Componente> componentes = sistema.componentes();
        for (Componente c : componentes) {
            sb.append("<b>").append(c.nombre()).append(":</b> ").append(c.estado()).append("&nbsp;&nbsp;&nbsp;");
        }
        if (!sistema.hayNube()) sb.append("<br><b>Sin nube:</b> los cambios solo viven en memoria.");
        estadoComponentes.setText(sb.append("</html>").toString());

        List<String> eventos = sistema.registro().recientes();
        modeloEventos.clear();
        for (String linea : eventos) modeloEventos.addElement(linea);
        automatico.setSelected(sistema.despachoAutomatico().isActivo());
    }
}
