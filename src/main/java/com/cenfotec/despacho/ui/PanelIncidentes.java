package com.cenfotec.despacho.ui;

import com.cenfotec.despacho.modelo.Incidente;
import com.cenfotec.despacho.modelo.Interseccion;
import com.cenfotec.despacho.modelo.Llamada;
import com.cenfotec.despacho.modelo.Unidad;
import com.cenfotec.despacho.servicio.ServicioDespacho;
import com.cenfotec.despacho.servicio.ServicioDespacho.ResultadoDespacho;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

/** Gestión diaria: incidentes activos, llamadas en cola y estado de las unidades. */
public class PanelIncidentes extends JPanel implements Refrescable {

    private static final String[] TIPOS = {"ACCIDENTE", "INCENDIO", "MEDICA", "ROBO", "RUIDO"};

    private final ServicioDespacho servicio;
    private final Runnable alCambiar;
    private final Consumer<ResultadoDespacho> alDespachar;

    private final DefaultTableModel modeloIncidentes = new DefaultTableModel(
            new String[]{"ID", "Prioridad", "Tipo", "Descripción", "Fecha y hora", "Estado", "Intersección", "Unidad"}, 0) {
        @Override
        public boolean isCellEditable(int fila, int columna) { return false; }
    };
    private final DefaultTableModel modeloUnidades = new DefaultTableModel(
            new String[]{"Placa", "Tipo", "Estado", "Intersección", "Incidente asignado"}, 0) {
        @Override
        public boolean isCellEditable(int fila, int columna) { return false; }
    };
    private final JTable tablaIncidentes = new JTable(modeloIncidentes);
    private final JLabel llamadas = new JLabel(" ");

    public PanelIncidentes(ServicioDespacho servicio, Runnable alCambiar, Consumer<ResultadoDespacho> alDespachar) {
        this.servicio = servicio;
        this.alCambiar = alCambiar;
        this.alDespachar = alDespachar;
        setLayout(new BorderLayout());

        tablaIncidentes.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tablaIncidentes.setRowHeight(22);
        tablaIncidentes.getColumnModel().getColumn(0).setMaxWidth(50);
        tablaIncidentes.getColumnModel().getColumn(1).setMaxWidth(80);
        tablaIncidentes.getColumnModel().getColumn(6).setMaxWidth(95);

        add(crearBarra(), BorderLayout.NORTH);
        add(new JScrollPane(tablaIncidentes), BorderLayout.CENTER);

        JTable tablaUnidades = new JTable(modeloUnidades);
        tablaUnidades.setRowHeight(22);
        JScrollPane scrollUnidades = new JScrollPane(tablaUnidades);
        scrollUnidades.setBorder(BorderFactory.createTitledBorder("Unidades"));
        scrollUnidades.setPreferredSize(new Dimension(100, 150));
        add(scrollUnidades, BorderLayout.SOUTH);
    }

    private JPanel crearBarra() {
        JPanel barra = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 6));
        barra.add(boton("Nuevo incidente", this::nuevoIncidente));
        barra.add(boton("Reclasificar", this::reclasificar));
        barra.add(boton("Despachar unidad", this::despachar));
        barra.add(boton("Cerrar", this::cerrar));
        barra.add(boton("Buscar por ID", this::buscar));
        barra.add(new JLabel("   |   "));
        barra.add(boton("Recibir llamada", this::recibirLlamada));
        barra.add(boton("Atender siguiente", this::atenderLlamada));
        barra.add(llamadas);
        return barra;
    }

    private JButton boton(String texto, Runnable accion) {
        JButton b = new JButton(texto);
        b.addActionListener(e -> accion.run());
        return b;
    }

    @Override
    public void refrescar() {
        // Qué unidad atiende cada incidente (para mostrarlo en ambas tablas)
        Map<String, Integer> incidentePorPlaca = new HashMap<>();
        int filaSeleccionada = tablaIncidentes.getSelectedRow();
        Integer idSeleccionado = filaSeleccionada < 0 ? null : (Integer) modeloIncidentes.getValueAt(filaSeleccionada, 0);
        modeloIncidentes.setRowCount(0);
        for (Incidente i : servicio.listarActivos()) {
            String placa = servicio.unidadAsignada(i.getId());
            if (placa != null) incidentePorPlaca.put(placa, i.getId());
            modeloIncidentes.addRow(new Object[]{i.getId(), "P" + i.getPrioridad(), i.getTipo(), i.getDescripcion(),
                    i.getFechaHora(), i.getEstado(), i.getIdInterseccion(), placa == null ? "-" : placa});
        }
        modeloUnidades.setRowCount(0);
        for (Unidad u : servicio.getUnidades()) {
            Integer id = incidentePorPlaca.get(u.getPlaca());
            modeloUnidades.addRow(new Object[]{u.getPlaca(), u.getTipo(), u.getEstado(), u.getIdInterseccion(),
                    id == null ? "-" : "#" + id});
        }
        llamadas.setText("Llamadas en cola: " + servicio.llamadasPendientes());
        if (idSeleccionado != null) seleccionar(idSeleccionado);     // mantiene la selección al redibujar
    }

    // ---------- Acciones ----------

    private void nuevoIncidente() {
        JComboBox<String> tipo = new JComboBox<>(TIPOS);
        JTextField descripcion = new JTextField(20);
        JSpinner prioridad = new JSpinner(new SpinnerNumberModel(3, 1, 5, 1));
        JComboBox<Interseccion> interseccion = comboIntersecciones();
        JPanel form = formulario("Tipo:", tipo, "Descripción:", descripcion,
                "Prioridad (1 crítica - 5 baja):", prioridad, "Intersección:", interseccion);
        if (JOptionPane.showConfirmDialog(this, form, "Nuevo incidente",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) != JOptionPane.OK_OPTION) return;
        if (descripcion.getText().isBlank()) {
            aviso("La descripción no puede estar vacía.");
            return;
        }
        Incidente nuevo = servicio.crearIncidente((String) tipo.getSelectedItem(), descripcion.getText().trim(),
                (Integer) prioridad.getValue(), ((Interseccion) interseccion.getSelectedItem()).getId());
        alCambiar.run();
        seleccionar(nuevo.getId());
    }

    private void reclasificar() {
        Integer id = idSeleccionado();
        if (id == null) return;
        JSpinner prioridad = new JSpinner(new SpinnerNumberModel(3, 1, 5, 1));
        JPanel form = formulario("Nueva prioridad para #" + id + " (1-5):", prioridad);
        if (JOptionPane.showConfirmDialog(this, form, "Reclasificar",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) != JOptionPane.OK_OPTION) return;
        servicio.reclasificar(id, (Integer) prioridad.getValue());
        alCambiar.run();
        seleccionar(id);
    }

    private void despachar() {
        Integer id = idSeleccionado();
        if (id == null) return;
        ResultadoDespacho r = servicio.despachar(id);
        if (!r.exito()) {
            aviso(r.mensaje());
            return;
        }
        JOptionPane.showMessageDialog(this, r.mensaje() + "\nRuta: " + r.ruta() + "\nTiempo estimado: "
                + r.minutos() + " min", "Despacho", JOptionPane.INFORMATION_MESSAGE);
        alDespachar.accept(r);
    }

    private void cerrar() {
        Integer id = idSeleccionado();
        if (id == null) return;
        if (JOptionPane.showConfirmDialog(this, "¿Cerrar el incidente #" + id + "? Pasará al historial.",
                "Cerrar incidente", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return;
        servicio.cerrarIncidente(id);
        alCambiar.run();
    }

    private void buscar() {
        String texto = JOptionPane.showInputDialog(this, "ID del incidente:", "Buscar", JOptionPane.QUESTION_MESSAGE);
        if (texto == null) return;
        try {
            int id = Integer.parseInt(texto.trim());
            Incidente i = servicio.buscarIncidente(id);
            if (i == null) {
                aviso("No se encontró un incidente activo con ID " + id + ".");
            } else {
                seleccionar(id);
            }
        } catch (NumberFormatException e) {
            aviso("Escribe un número válido.");
        }
    }

    private void recibirLlamada() {
        JTextField telefono = new JTextField(14);
        JTextField ubicacion = new JTextField(20);
        JPanel form = formulario("Teléfono:", telefono, "Ubicación referida:", ubicacion);
        if (JOptionPane.showConfirmDialog(this, form, "Recibir llamada",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) != JOptionPane.OK_OPTION) return;
        if (telefono.getText().isBlank()) {
            aviso("El teléfono no puede estar vacío.");
            return;
        }
        servicio.recibirLlamada(telefono.getText().trim(), ubicacion.getText().trim());
        alCambiar.run();
    }

    private void atenderLlamada() {
        Llamada siguiente = servicio.verSiguienteLlamada();
        if (siguiente == null) {
            aviso("No hay llamadas pendientes.");
            return;
        }
        JComboBox<String> tipo = new JComboBox<>(TIPOS);
        JSpinner prioridad = new JSpinner(new SpinnerNumberModel(3, 1, 5, 1));
        JComboBox<Interseccion> interseccion = comboIntersecciones();
        JPanel form = formulario("Llamada:", new JLabel(siguiente.getTelefono() + " - " + siguiente.getUbicacionReferida()),
                "Tipo de emergencia:", tipo, "Prioridad (1-5):", prioridad, "Intersección:", interseccion);
        if (JOptionPane.showConfirmDialog(this, form, "Atender llamada",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) != JOptionPane.OK_OPTION) return;
        Incidente creado = servicio.atenderSiguienteLlamada((String) tipo.getSelectedItem(),
                (Integer) prioridad.getValue(), ((Interseccion) interseccion.getSelectedItem()).getId());
        alCambiar.run();
        if (creado != null) seleccionar(creado.getId());
    }

    // ---------- Auxiliares ----------

    private JComboBox<Interseccion> comboIntersecciones() {
        return new JComboBox<>(servicio.getIntersecciones().values().toArray(new Interseccion[0]));
    }

    private JPanel formulario(Object... filas) {
        JPanel p = new JPanel(new GridLayout(0, 2, 8, 8));      // pares etiqueta, componente
        for (int i = 0; i < filas.length; i += 2) {
            p.add(new JLabel((String) filas[i]));
            p.add((java.awt.Component) filas[i + 1]);
        }
        return p;
    }

    private Integer idSeleccionado() {
        int fila = tablaIncidentes.getSelectedRow();
        if (fila < 0) {
            aviso("Selecciona un incidente de la tabla.");
            return null;
        }
        return (Integer) modeloIncidentes.getValueAt(fila, 0);
    }

    private void seleccionar(int id) {
        for (int fila = 0; fila < modeloIncidentes.getRowCount(); fila++) {
            if (((Integer) modeloIncidentes.getValueAt(fila, 0)) == id) {
                tablaIncidentes.setRowSelectionInterval(fila, fila);
                tablaIncidentes.scrollRectToVisible(tablaIncidentes.getCellRect(fila, 0, true));
                return;
            }
        }
    }

    private void aviso(String mensaje) {
        JOptionPane.showMessageDialog(this, mensaje, "Aviso", JOptionPane.WARNING_MESSAGE);
    }
}
