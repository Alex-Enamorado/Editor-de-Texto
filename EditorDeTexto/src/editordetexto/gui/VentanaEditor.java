package editordetexto.gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.Insets;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;


public class VentanaEditor extends JFrame {

    private static final Integer[] TAMANOS =
            { 8, 9, 10, 11, 12, 14, 16, 18, 20, 24, 28, 36, 48, 72 };

    private final JTextPane areaTexto = new JTextPane();

    private final JComboBox<String> comboFuente = new JComboBox<>(
            GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames());
    private final JComboBox<Integer> comboTamano = new JComboBox<>(TAMANOS);
    private final JToggleButton btnNegrita   = new JToggleButton("N");
    private final JToggleButton btnCursiva   = new JToggleButton("C");
    private final JToggleButton btnSubrayado = new JToggleButton("S");
    private final JToggleButton btnTachado   = new JToggleButton("T");
    private final JButton btnColor = new JButton("Color");

    public VentanaEditor() {
        super("Sin titulo - Editor de Texto");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(940, 660);
        setLocationRelativeTo(null);

        setJMenuBar(crearMenu());
        add(crearBarraFormato(), BorderLayout.NORTH);
        conectarBarra();

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

    private JToolBar crearBarraFormato() {
        JToolBar barra = new JToolBar();
        barra.setFloatable(false);
        barra.setRollover(true);

        comboFuente.setSelectedItem("Arial");
        comboFuente.setMaximumSize(new Dimension(190, 26));
        comboFuente.setToolTipText("Tipo de fuente");

        comboTamano.setEditable(true);
        comboTamano.setSelectedItem(14);
        comboTamano.setMaximumSize(new Dimension(64, 26));
        comboTamano.setToolTipText("Tamano de fuente");

        btnNegrita.setToolTipText("Negrita");
        btnCursiva.setToolTipText("Cursiva");
        btnSubrayado.setToolTipText("Subrayado");
        btnTachado.setToolTipText("Tachado");
        btnColor.setToolTipText("Color de la fuente");

        barra.add(comboFuente);
        barra.add(comboTamano);
        barra.addSeparator();
        barra.add(btnNegrita);
        barra.add(btnCursiva);
        barra.add(btnSubrayado);
        barra.add(btnTachado);
        barra.addSeparator();
        barra.add(btnColor);
        return barra;
    }

    private void conectarBarra() {
        // TODO paso 3: aplicar el formato a la seleccion y sincronizar la barra
        // con la posicion del cursor.
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignorado) {
            
        }
        SwingUtilities.invokeLater(() -> new VentanaEditor().setVisible(true));
    }
}
