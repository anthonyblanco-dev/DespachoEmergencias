package com.cenfotec.despacho.ui;

import com.cenfotec.despacho.estructuras.Grafo;
import com.cenfotec.despacho.modelo.Incidente;
import com.cenfotec.despacho.modelo.Interseccion;
import com.cenfotec.despacho.modelo.Unidad;
import com.cenfotec.despacho.servicio.ServicioDespacho;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Dibuja el grafo de la ciudad con incidentes, unidades y la ruta resaltada. */
public class PanelMapa extends JPanel implements Refrescable {

    private final ServicioDespacho servicio;
    private final Lienzo lienzo = new Lienzo();
    private final JComboBox<Interseccion> comboOrigen = new JComboBox<>();
    private final JComboBox<Interseccion> comboDestino = new JComboBox<>();
    private final JLabel resultado = new JLabel("Elige dos intersecciones para calcular la ruta más corta.");

    public PanelMapa(ServicioDespacho servicio) {
        this.servicio = servicio;
        setLayout(new BorderLayout());

        for (Interseccion i : servicio.getIntersecciones().values()) {
            comboOrigen.addItem(i);
            comboDestino.addItem(i);
        }
        if (comboDestino.getItemCount() > 1) comboDestino.setSelectedIndex(comboDestino.getItemCount() - 1);

        JButton ruta = new JButton("Ruta más corta");
        ruta.addActionListener(e -> calcularRuta());
        JButton bfs = new JButton("Recorrido BFS");
        bfs.addActionListener(e -> mostrarRecorrido(true));
        JButton dfs = new JButton("Recorrido DFS");
        dfs.addActionListener(e -> mostrarRecorrido(false));
        JButton limpiar = new JButton("Limpiar");
        limpiar.addActionListener(e -> {
            lienzo.ruta = List.of();
            lienzo.repaint();
            resultado.setText(" ");
        });

        JPanel controles = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        controles.add(new JLabel("Origen:"));
        controles.add(comboOrigen);
        controles.add(new JLabel("Destino:"));
        controles.add(comboDestino);
        controles.add(ruta);
        controles.add(bfs);
        controles.add(dfs);
        controles.add(limpiar);

        JPanel norte = new JPanel(new BorderLayout());
        norte.add(controles, BorderLayout.NORTH);
        resultado.setBorder(BorderFactory.createEmptyBorder(0, 12, 6, 12));
        norte.add(resultado, BorderLayout.SOUTH);

        JPanel leyenda = new JPanel(new GridLayout(2, 1));
        leyenda.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));
        leyenda.add(new JLabel("Círculo = intersección  |  número en la calle = minutos de recorrido  |  naranja = ruta resaltada"));
        leyenda.add(new JLabel("Cuadro sobre la intersección = incidente (color por prioridad, verde = ya tiene unidad)  |  "
                + "cuadro debajo = unidad (azul disponible, gris ocupada)"));

        add(norte, BorderLayout.NORTH);
        add(lienzo, BorderLayout.CENTER);
        add(leyenda, BorderLayout.SOUTH);
    }

    /** Resalta una ruta (por ejemplo, la de una unidad recién despachada). */
    public void mostrarRuta(List<Integer> ruta, String texto) {
        lienzo.ruta = new ArrayList<>(ruta);
        resultado.setText(texto);
        lienzo.repaint();
    }

    @Override
    public void refrescar() {
        lienzo.repaint();
    }

    private void calcularRuta() {
        Interseccion a = (Interseccion) comboOrigen.getSelectedItem();
        Interseccion b = (Interseccion) comboDestino.getSelectedItem();
        if (a == null || b == null) return;
        Grafo.ResultadoDijkstra r = servicio.getMapa().dijkstra(a.getId());
        if (!r.alcanzable(b.getId())) {
            mostrarRuta(List.of(), "No hay ruta entre " + a.getNombre() + " y " + b.getNombre() + ".");
            return;
        }
        List<Integer> ruta = r.rutaA(b.getId());
        mostrarRuta(ruta, "Ruta más corta (Dijkstra): " + ruta + "  |  tiempo: " + r.distanciaA(b.getId()) + " min");
    }

    private void mostrarRecorrido(boolean anchura) {
        Interseccion a = (Interseccion) comboOrigen.getSelectedItem();
        if (a == null) return;
        List<Integer> orden = anchura ? servicio.getMapa().bfs(a.getId()) : servicio.getMapa().dfs(a.getId());
        mostrarRuta(List.of(), (anchura ? "Recorrido en anchura (BFS)" : "Recorrido en profundidad (DFS)")
                + " desde " + a.getId() + ": " + orden);
    }

    private class Lienzo extends JPanel {
        private static final int RADIO = 17;
        private List<Integer> ruta = List.of();

        Lienzo() {
            setBackground(Color.WHITE);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            Map<Integer, Interseccion> puntos = servicio.getIntersecciones();
            if (puntos.isEmpty()) {
                g2.drawString("No hay mapa cargado.", 20, 30);
                g2.dispose();
                return;
            }

            // Escala para que el mapa quepa en el panel dejando espacio para las etiquetas
            int minX = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE, minY = Integer.MAX_VALUE, maxY = Integer.MIN_VALUE;
            for (Interseccion p : puntos.values()) {
                minX = Math.min(minX, p.getX());
                maxX = Math.max(maxX, p.getX());
                minY = Math.min(minY, p.getY());
                maxY = Math.max(maxY, p.getY());
            }
            int margenX = 70, margenSuperior = 85, margenInferior = 70;
            double anchoMapa = Math.max(1, maxX - minX);
            double altoMapa = Math.max(1, maxY - minY);
            double escala = Math.min((getWidth() - 2.0 * margenX) / anchoMapa,
                    (getHeight() - margenSuperior - margenInferior) / altoMapa);
            escala = Math.max(0.3, escala);
            double escalaFinal = escala;
            int desdeX = (int) (margenX + (getWidth() - 2.0 * margenX - anchoMapa * escalaFinal) / 2);
            int desdeY = margenSuperior;
            int fx = minX, fy = minY;

            java.util.function.ToIntFunction<Interseccion> px = p -> desdeX + (int) ((p.getX() - fx) * escalaFinal);
            java.util.function.ToIntFunction<Interseccion> py = p -> desdeY + (int) ((p.getY() - fy) * escalaFinal);

            Set<String> enRuta = new HashSet<>();
            for (int i = 0; i + 1 < ruta.size(); i++) {
                int a = ruta.get(i), b = ruta.get(i + 1);
                enRuta.add(Math.min(a, b) + "-" + Math.max(a, b));
            }

            // Calles
            Font fuentePeso = getFont().deriveFont(Font.PLAIN, 11f);
            for (Grafo.Conexion c : servicio.getMapa().conexiones()) {
                Interseccion a = puntos.get(c.origen());
                Interseccion b = puntos.get(c.destino());
                if (a == null || b == null) continue;
                boolean resaltada = enRuta.contains(c.origen() + "-" + c.destino());
                g2.setColor(resaltada ? new Color(240, 130, 20) : new Color(170, 170, 170));
                g2.setStroke(new BasicStroke(resaltada ? 6f : 2f));
                g2.drawLine(px.applyAsInt(a), py.applyAsInt(a), px.applyAsInt(b), py.applyAsInt(b));
            }
            for (Grafo.Conexion c : servicio.getMapa().conexiones()) {
                Interseccion a = puntos.get(c.origen());
                Interseccion b = puntos.get(c.destino());
                if (a == null || b == null) continue;
                int mx = (px.applyAsInt(a) + px.applyAsInt(b)) / 2;
                int my = (py.applyAsInt(a) + py.applyAsInt(b)) / 2;
                etiquetaConFondo(g2, String.valueOf(c.peso()), fuentePeso, mx, my, Color.WHITE, new Color(90, 90, 90));
            }

            // Intersecciones
            Set<Integer> nodosRuta = new HashSet<>(ruta);
            g2.setFont(getFont().deriveFont(Font.BOLD, 12f));
            for (Interseccion p : puntos.values()) {
                int x = px.applyAsInt(p), y = py.applyAsInt(p);
                g2.setColor(nodosRuta.contains(p.getId()) ? new Color(255, 214, 160) : new Color(235, 240, 250));
                g2.fillOval(x - RADIO, y - RADIO, 2 * RADIO, 2 * RADIO);
                g2.setColor(new Color(50, 60, 90));
                g2.setStroke(new BasicStroke(2f));
                g2.drawOval(x - RADIO, y - RADIO, 2 * RADIO, 2 * RADIO);
                String id = String.valueOf(p.getId());
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(id, x - fm.stringWidth(id) / 2, y + fm.getAscent() / 2 - 1);
            }

            // Incidentes (arriba) y unidades (abajo) de cada intersección
            Map<Integer, List<Incidente>> incidentes = new HashMap<>();
            for (Incidente i : servicio.listarActivos()) {
                incidentes.computeIfAbsent(i.getIdInterseccion(), k -> new ArrayList<>()).add(i);
            }
            Map<Integer, List<Unidad>> unidades = new HashMap<>();
            for (Unidad u : servicio.getUnidades()) {
                unidades.computeIfAbsent(u.getIdInterseccion(), k -> new ArrayList<>()).add(u);
            }
            Font fuenteMarca = getFont().deriveFont(Font.BOLD, 11f);
            for (Interseccion p : puntos.values()) {
                int x = px.applyAsInt(p), y = py.applyAsInt(p);
                int nivel = 0;
                for (Incidente i : incidentes.getOrDefault(p.getId(), List.of())) {
                    Color fondo = "ASIGNADO".equals(i.getEstado()) ? new Color(40, 160, 70) : colorPrioridad(i.getPrioridad());
                    etiquetaConFondo(g2, "#" + i.getId() + " P" + i.getPrioridad(), fuenteMarca,
                            x, y - RADIO - 12 - nivel * 20, fondo, Color.WHITE);
                    nivel++;
                }
                nivel = 0;
                for (Unidad u : unidades.getOrDefault(p.getId(), List.of())) {
                    Color fondo = "DISPONIBLE".equals(u.getEstado()) ? new Color(30, 100, 200) : new Color(130, 130, 130);
                    etiquetaConFondo(g2, u.getPlaca(), fuenteMarca, x, y + RADIO + 14 + nivel * 20, fondo, Color.WHITE);
                    nivel++;
                }
            }
            g2.dispose();
        }

        private Color colorPrioridad(int prioridad) {
            return switch (prioridad) {
                case 1 -> new Color(170, 20, 20);
                case 2 -> new Color(215, 90, 20);
                case 3 -> new Color(190, 150, 10);
                case 4 -> new Color(90, 130, 170);
                default -> new Color(120, 120, 120);
            };
        }

        private void etiquetaConFondo(Graphics2D g2, String texto, Font fuente, int cx, int cy,
                                      Color fondo, Color letra) {
            g2.setFont(fuente);
            FontMetrics fm = g2.getFontMetrics();
            int ancho = fm.stringWidth(texto) + 8;
            int alto = fm.getHeight() + 2;
            g2.setColor(fondo);
            g2.fillRoundRect(cx - ancho / 2, cy - alto / 2, ancho, alto, 8, 8);
            g2.setColor(letra);
            g2.drawString(texto, cx - ancho / 2 + 4, cy - alto / 2 + fm.getAscent() + 1);
        }
    }
}
