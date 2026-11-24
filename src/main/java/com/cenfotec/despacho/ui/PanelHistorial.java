package com.cenfotec.despacho.ui;

import com.cenfotec.despacho.estructuras.ArbolBMas.NodoVista;
import com.cenfotec.despacho.modelo.ClaveHistorial;
import com.cenfotec.despacho.modelo.Incidente;
import com.cenfotec.despacho.servicio.ServicioDespacho;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/** Dibuja el árbol B+ del historial de incidentes cerrados y permite consultar por rango de fechas. */
public class PanelHistorial extends JPanel implements Refrescable {

    private final ServicioDespacho servicio;
    private final JLabel info = new JLabel(" ");
    private final Lienzo lienzo = new Lienzo();
    private final JTextField campoDesde = new JTextField(LocalDate.now().withDayOfYear(1).toString(), 12);
    private final JTextField campoHasta = new JTextField(LocalDate.now().withMonth(12).withDayOfMonth(31).toString(), 12);
    private final DefaultTableModel modeloResultados = new DefaultTableModel(
            new String[]{"ID", "Prioridad", "Tipo", "Descripción", "Fecha y hora"}, 0) {
        @Override
        public boolean isCellEditable(int fila, int columna) { return false; }
    };
    private final JLabel resumenConsulta = new JLabel("Consulta por rango: recorre solo las hojas enlazadas necesarias.");

    public PanelHistorial(ServicioDespacho servicio) {
        this.servicio = servicio;
        setLayout(new BorderLayout());
        info.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));
        add(info, BorderLayout.NORTH);

        JPanel arbol = new JPanel(new BorderLayout());
        arbol.add(new JScrollPane(lienzo), BorderLayout.CENTER);

        JPanel consulta = new JPanel(new BorderLayout());
        JPanel formulario = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        formulario.add(new JLabel("Desde:"));
        formulario.add(campoDesde);
        formulario.add(new JLabel("Hasta:"));
        formulario.add(campoHasta);
        JButton consultar = new JButton("Consultar rango");
        consultar.addActionListener(e -> consultar(true));
        formulario.add(consultar);
        formulario.add(resumenConsulta);
        consulta.add(formulario, BorderLayout.NORTH);
        JTable tabla = new JTable(modeloResultados);
        tabla.setRowHeight(22);
        tabla.getColumnModel().getColumn(0).setMaxWidth(60);
        tabla.getColumnModel().getColumn(1).setMaxWidth(80);
        consulta.add(new JScrollPane(tabla), BorderLayout.CENTER);

        JSplitPane division = new JSplitPane(JSplitPane.VERTICAL_SPLIT, arbol, consulta);
        division.setResizeWeight(0.62);
        add(division, BorderLayout.CENTER);
    }

    @Override
    public void refrescar() {
        lienzo.establecer(servicio.vistaHistorial());
        info.setText("Árbol B+ del historial (incidentes cerrados, indexados por fecha y hora)  |  Orden: "
                + servicio.ordenHistorial() + "  |  Cerrados: " + servicio.cantidadHistorial()
                + "  |  Altura: " + servicio.alturaHistorial()
                + "  |  Válido: " + (servicio.historialValido() ? "sí" : "NO"));
        consultar(false);                    // mantiene la tabla al día con el rango escrito
    }

    private void consultar(boolean avisarSiHayError) {
        try {
            List<Incidente> lista = servicio.consultarHistorial(campoDesde.getText(), campoHasta.getText());
            modeloResultados.setRowCount(0);
            for (Incidente i : lista) {
                modeloResultados.addRow(new Object[]{i.getId(), "P" + i.getPrioridad(), i.getTipo(),
                        i.getDescripcion(), i.getFechaHora()});
            }
            resumenConsulta.setText(lista.size() + " incidente(s) en el rango.");
        } catch (IllegalArgumentException ex) {
            if (avisarSiHayError) {
                JOptionPane.showMessageDialog(this, ex.getMessage(), "Fecha inválida", JOptionPane.WARNING_MESSAGE);
            }
        }
    }

    private static class Lienzo extends JPanel {
        private static final int MARGEN = 30;
        private static final int ANCHO_CLAVE = 106;
        private static final int ALTO_NODO = 30;
        private static final int SEPARACION_HOJAS = 34;
        private static final int SEPARACION_NIVELES = 85;

        private NodoVista<ClaveHistorial> raiz;
        private final Map<NodoVista<ClaveHistorial>, Rectangle> cajas = new IdentityHashMap<>();
        private final List<NodoVista<ClaveHistorial>> hojas = new ArrayList<>();
        private int siguienteX;
        private int niveles;

        Lienzo() {
            setBackground(Color.WHITE);
        }

        void establecer(NodoVista<ClaveHistorial> nuevaRaiz) {
            raiz = nuevaRaiz;
            cajas.clear();
            hojas.clear();
            siguienteX = MARGEN;
            niveles = 0;
            if (raiz != null && !raiz.claves().isEmpty()) {
                disponer(raiz, 0);
                setPreferredSize(new Dimension(siguienteX + MARGEN, 2 * MARGEN + niveles * SEPARACION_NIVELES));
            } else {
                setPreferredSize(new Dimension(300, 100));
            }
            revalidate();
            repaint();
        }

        /** Las hojas se colocan de izquierda a derecha y cada padre se centra sobre sus hijos. */
        private void disponer(NodoVista<ClaveHistorial> n, int nivel) {
            niveles = Math.max(niveles, nivel + 1);
            int ancho = Math.max(1, n.claves().size()) * ANCHO_CLAVE;
            int y = MARGEN + nivel * SEPARACION_NIVELES;
            if (n.hoja()) {
                cajas.put(n, new Rectangle(siguienteX, y, ancho, ALTO_NODO));
                hojas.add(n);
                siguienteX += ancho + SEPARACION_HOJAS;
                return;
            }
            for (NodoVista<ClaveHistorial> hijo : n.hijos()) disponer(hijo, nivel + 1);
            Rectangle primero = cajas.get(n.hijos().get(0));
            Rectangle ultimo = cajas.get(n.hijos().get(n.hijos().size() - 1));
            int centro = (primero.x + ultimo.x + ultimo.width) / 2;
            cajas.put(n, new Rectangle(centro - ancho / 2, y, ancho, ALTO_NODO));
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            if (raiz == null || raiz.claves().isEmpty()) {
                g2.setColor(Color.GRAY);
                g2.drawString("El historial está vacío. Cierra incidentes (pestaña Incidentes) para llenarlo.", 20, 40);
                g2.dispose();
                return;
            }

            // Enlaces padre -> hijos
            g2.setColor(new Color(120, 120, 120));
            g2.setStroke(new BasicStroke(1.5f));
            for (Map.Entry<NodoVista<ClaveHistorial>, Rectangle> e : cajas.entrySet()) {
                Rectangle padre = e.getValue();
                for (NodoVista<ClaveHistorial> hijo : e.getKey().hijos()) {
                    Rectangle r = cajas.get(hijo);
                    g2.drawLine(padre.x + padre.width / 2, padre.y + padre.height, r.x + r.width / 2, r.y);
                }
            }

            // Enlaces entre hojas (permiten recorrer el rango sin volver a subir)
            g2.setColor(new Color(40, 140, 70));
            g2.setStroke(new BasicStroke(2f));
            for (int i = 0; i + 1 < hojas.size(); i++) {
                Rectangle a = cajas.get(hojas.get(i));
                Rectangle b = cajas.get(hojas.get(i + 1));
                int y = a.y + a.height / 2;
                g2.drawLine(a.x + a.width, y, b.x, y);
                g2.fillPolygon(new int[]{b.x, b.x - 7, b.x - 7}, new int[]{y, y - 4, y + 4}, 3);
            }

            g2.setFont(getFont().deriveFont(Font.PLAIN, 10f));
            FontMetrics fm = g2.getFontMetrics();
            for (Map.Entry<NodoVista<ClaveHistorial>, Rectangle> e : cajas.entrySet()) {
                NodoVista<ClaveHistorial> n = e.getKey();
                Rectangle r = e.getValue();
                g2.setColor(n.hoja() ? new Color(225, 245, 228) : new Color(222, 232, 250));
                g2.fillRect(r.x, r.y, r.width, r.height);
                g2.setColor(new Color(50, 60, 90));
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawRect(r.x, r.y, r.width, r.height);
                for (int i = 0; i < n.claves().size(); i++) {
                    int x = r.x + i * ANCHO_CLAVE;
                    if (i > 0) g2.drawLine(x, r.y, x, r.y + r.height);
                    String texto = etiqueta(n.claves().get(i));
                    g2.drawString(texto, x + (ANCHO_CLAVE - fm.stringWidth(texto)) / 2,
                            r.y + (r.height + fm.getAscent()) / 2 - 2);
                }
            }
            g2.dispose();
        }

        /** "11-10 08:25 #4": fecha corta y número de incidente. */
        private String etiqueta(ClaveHistorial clave) {
            String f = clave.fechaHora();
            String corta = f.length() >= 16 ? f.substring(5, 16).replace('T', ' ') : f;
            return corta + " #" + clave.id();
        }
    }
}
