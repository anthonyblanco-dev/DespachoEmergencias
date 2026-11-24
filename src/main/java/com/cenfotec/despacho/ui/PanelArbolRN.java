package com.cenfotec.despacho.ui;

import com.cenfotec.despacho.estructuras.ArbolRojoNegro.NodoVista;
import com.cenfotec.despacho.modelo.Incidente;
import com.cenfotec.despacho.servicio.ServicioDespacho;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.util.IdentityHashMap;
import java.util.Map;

/** Dibuja el árbol Rojo-Negro de incidentes activos (cada nodo muestra #id y prioridad). */
public class PanelArbolRN extends JPanel implements Refrescable {

    private final ServicioDespacho servicio;
    private final JLabel info = new JLabel(" ");
    private final Lienzo lienzo = new Lienzo();

    public PanelArbolRN(ServicioDespacho servicio) {
        this.servicio = servicio;
        setLayout(new BorderLayout());
        info.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));
        add(info, BorderLayout.NORTH);
        add(new JScrollPane(lienzo), BorderLayout.CENTER);
        add(leyenda(), BorderLayout.SOUTH);
    }

    private JLabel leyenda() {
        JLabel l = new JLabel("Rojo / negro = color del nodo. Menor prioridad (P1) y más antiguo = más a la izquierda. "
                + "El más urgente es el nodo del extremo izquierdo.");
        l.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));
        return l;
    }

    @Override
    public void refrescar() {
        lienzo.establecer(servicio.vistaArbol());
        info.setText("Árbol Rojo-Negro de incidentes activos  |  Nodos: " + servicio.cantidadActivos()
                + "  |  Altura: " + servicio.alturaArbol()
                + "  |  Propiedades válidas: " + (servicio.arbolValido() ? "sí" : "NO"));
    }

    private static class Lienzo extends JPanel {
        private static final int MARGEN = 40;
        private static final int DX = 56;
        private static final int DY = 78;
        private static final int DIAMETRO = 42;
        private static final Color ROJO = new Color(200, 40, 40);
        private static final Color NEGRO = new Color(35, 35, 35);

        private NodoVista<Incidente> raiz;
        private final Map<NodoVista<Incidente>, Point> posiciones = new IdentityHashMap<>();
        private int contador;
        private int profundidadMaxima;

        Lienzo() {
            setBackground(Color.WHITE);
        }

        void establecer(NodoVista<Incidente> nuevaRaiz) {
            raiz = nuevaRaiz;
            posiciones.clear();
            contador = 0;
            profundidadMaxima = 0;
            colocar(raiz, 0);
            int ancho = 2 * MARGEN + Math.max(1, contador) * DX;
            int alto = 2 * MARGEN + (profundidadMaxima + 1) * DY;
            setPreferredSize(new Dimension(ancho, alto));
            revalidate();
            repaint();
        }

        /** Recorrido in-orden: la posición horizontal es el orden del nodo, la vertical su profundidad. */
        private void colocar(NodoVista<Incidente> n, int profundidad) {
            if (n == null) return;
            colocar(n.izq(), profundidad + 1);
            posiciones.put(n, new Point(MARGEN + contador * DX, MARGEN + profundidad * DY));
            contador++;
            profundidadMaxima = Math.max(profundidadMaxima, profundidad);
            colocar(n.der(), profundidad + 1);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            if (raiz == null) {
                g2.setColor(Color.GRAY);
                g2.drawString("No hay incidentes activos.", 20, 30);
                g2.dispose();
                return;
            }
            int desplazamiento = Math.max(0, (getWidth() - getPreferredSize().width) / 2);
            g2.translate(desplazamiento, 0);

            g2.setColor(new Color(120, 120, 120));
            g2.setStroke(new BasicStroke(1.6f));
            for (Map.Entry<NodoVista<Incidente>, Point> e : posiciones.entrySet()) {
                dibujarEnlace(g2, e.getValue(), e.getKey().izq());
                dibujarEnlace(g2, e.getValue(), e.getKey().der());
            }
            for (Map.Entry<NodoVista<Incidente>, Point> e : posiciones.entrySet()) {
                dibujarNodo(g2, e.getKey(), e.getValue());
            }
            g2.dispose();
        }

        private void dibujarEnlace(Graphics2D g2, Point padre, NodoVista<Incidente> hijo) {
            if (hijo == null) return;
            Point p = posiciones.get(hijo);
            g2.drawLine(padre.x, padre.y, p.x, p.y);
        }

        private void dibujarNodo(Graphics2D g2, NodoVista<Incidente> n, Point centro) {
            int r = DIAMETRO / 2;
            g2.setColor(n.rojo() ? ROJO : NEGRO);
            g2.fillOval(centro.x - r, centro.y - r, DIAMETRO, DIAMETRO);
            if ("ASIGNADO".equals(n.dato().getEstado())) {          // borde verde: ya tiene unidad
                g2.setColor(new Color(40, 160, 70));
                g2.setStroke(new BasicStroke(3f));
                g2.drawOval(centro.x - r, centro.y - r, DIAMETRO, DIAMETRO);
            }
            g2.setColor(Color.WHITE);
            Font negrita = getFont().deriveFont(Font.BOLD, 12f);
            Font pequena = getFont().deriveFont(Font.PLAIN, 10f);
            texto(g2, "#" + n.dato().getId(), negrita, centro.x, centro.y - 2);
            texto(g2, "P" + n.dato().getPrioridad(), pequena, centro.x, centro.y + 11);
        }

        private void texto(Graphics2D g2, String s, Font f, int cx, int baseY) {
            g2.setFont(f);
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(s, cx - fm.stringWidth(s) / 2, baseY);
        }
    }
}
