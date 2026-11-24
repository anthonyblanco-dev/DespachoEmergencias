package com.cenfotec.despacho.ui;

import com.cenfotec.despacho.servicio.ServicioDespacho;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.util.List;

/** Muestra cómo está repartido el índice de incidentes (id -> incidente) en la tabla hash dinámica. */
public class PanelHash extends JPanel implements Refrescable {

    private final ServicioDespacho servicio;
    private final JLabel info = new JLabel(" ");
    private final DefaultTableModel modelo = new DefaultTableModel(
            new String[]{"Cubeta", "Elementos", "Cadena de claves (id de incidente)"}, 0) {
        @Override
        public boolean isCellEditable(int fila, int columna) { return false; }

        @Override
        public Class<?> getColumnClass(int columna) { return columna == 2 ? String.class : Integer.class; }
    };

    public PanelHash(ServicioDespacho servicio) {
        this.servicio = servicio;
        setLayout(new BorderLayout());
        info.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));

        JTable tabla = new JTable(modelo);
        tabla.setRowHeight(24);
        tabla.getColumnModel().getColumn(0).setMaxWidth(90);
        tabla.getColumnModel().getColumn(1).setMaxWidth(100);
        tabla.getColumnModel().getColumn(1).setCellRenderer(new RenderizadorCarga());

        JLabel nota = new JLabel("Encadenamiento separado. Cuando el factor de carga supera 0.75 la tabla duplica su capacidad "
                + "y reubica todas las claves (rehash).");
        nota.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));

        add(info, BorderLayout.NORTH);
        add(new JScrollPane(tabla), BorderLayout.CENTER);
        add(nota, BorderLayout.SOUTH);
    }

    @Override
    public void refrescar() {
        modelo.setRowCount(0);
        List<List<Integer>> cubetas = servicio.distribucionIndice();
        for (int i = 0; i < cubetas.size(); i++) {
            List<Integer> cadena = cubetas.get(i);
            StringBuilder sb = new StringBuilder();
            for (int j = 0; j < cadena.size(); j++) {
                if (j > 0) sb.append("  →  ");
                sb.append(cadena.get(j));
            }
            modelo.addRow(new Object[]{i, cadena.size(), sb.toString()});
        }
        info.setText(String.format("Tabla hash dinámica  |  Capacidad: %d  |  Elementos: %d  |  Factor de carga: %.2f"
                        + "  |  Redimensionamientos: %d",
                servicio.capacidadIndice(), servicio.elementosIndice(),
                servicio.factorCargaIndice(), servicio.redimensionamientosIndice()));
    }

    /** Pinta de más intenso las cubetas con colisiones. */
    private static class RenderizadorCarga extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable tabla, Object valor, boolean seleccionada,
                                                       boolean foco, int fila, int columna) {
            Component c = super.getTableCellRendererComponent(tabla, valor, seleccionada, foco, fila, columna);
            setHorizontalAlignment(CENTER);
            if (!seleccionada) {
                int n = valor instanceof Integer ? (Integer) valor : 0;
                c.setBackground(n == 0 ? Color.WHITE : n == 1 ? new Color(220, 240, 220) : new Color(250, 215, 170));
            }
            return c;
        }
    }
}
