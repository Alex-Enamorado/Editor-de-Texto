package editordetexto.gui;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.Insets;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;


public class VentanaEditor extends JFrame {

    private final JTextPane areaTexto = new JTextPane();

    public VentanaEditor() {
        super("Sin titulo - Editor de Texto");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(940, 660);
        setLocationRelativeTo(null);

        setJMenuBar(crearMenu());

        areaTexto.setFont(new Font("Arial", Font.PLAIN, 14));
        areaTexto.setMargin(new Insets(10, 12, 10, 12));
        add(new JScrollPane(areaTexto), BorderLayout.CENTER);
    }

    private JMenuBar crearMenu() {
        JMenuBar barra = new JMenuBar();

        JMenu archivo = new JMenu("Archivo");
        archivo.add(new JMenuItem("Nuevo"));
        archivo.add(new JMenuItem("Abrir..."));
        archivo.add(new JMenuItem("Guardar"));
        archivo.add(new JMenuItem("Guardar como..."));
        archivo.addSeparator();
        archivo.add(new JMenuItem("Salir"));

        barra.add(archivo);
        return barra;
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignorado) {
            
        }
        SwingUtilities.invokeLater(() -> new VentanaEditor().setVisible(true));
    }
}
