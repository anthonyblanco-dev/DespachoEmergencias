package com.cenfotec.despacho;

import com.cenfotec.despacho.ui.VentanaPrincipal;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.io.IOException;

/** Arranca la interfaz gráfica (Swing). La versión de consola está en {@link Main}. */
public class MainGrafico {

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            // si falla se usa el aspecto por defecto de Swing
        }

        Arranque.Resultado resultado;
        try {
            resultado = Arranque.iniciar();
        } catch (IOException e) {
            JOptionPane.showMessageDialog(null, "No se pudieron cargar los datos: " + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        SwingUtilities.invokeLater(() ->
                new VentanaPrincipal(resultado.servicio(), resultado.origen()).setVisible(true));
    }
}
