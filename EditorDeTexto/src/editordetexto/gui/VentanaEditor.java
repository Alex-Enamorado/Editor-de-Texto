package editordetexto.gui;

import editordetexto.modelo.DocumentoEdt;
import editordetexto.persistencia.EdtArchivo;
import editordetexto.persistencia.EdtException;
import editordetexto.persistencia.PuenteSwing;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.Insets;
import java.io.File;
import java.util.function.Consumer;
import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.MutableAttributeSet;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;


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

    private boolean actualizandoBarra;
    private File archivoActual;

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
        archivo.add(item("Nuevo", e -> nuevo()));
        archivo.add(item("Abrir...", e -> abrir()));
        archivo.add(item("Guardar", e -> guardar()));
        archivo.add(item("Guardar como...", e -> guardarComo()));
        archivo.addSeparator();
        archivo.add(item("Salir", e -> System.exit(0)));

        barra.add(archivo);
        return barra;
    }

    private JMenuItem item(String texto, java.awt.event.ActionListener accion) {
        JMenuItem mi = new JMenuItem(texto);
        mi.addActionListener(accion);
        return mi;
    }

    private void nuevo() {
        // paso 2
    }

    private void abrir() {
        // paso 3
    }

    private void guardar() {
        // paso 4
    }

    private void guardarComo() {
        // paso 4
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
        comboFuente.addActionListener(e -> {
            if (actualizandoBarra) return;
            Object fuente = comboFuente.getSelectedItem();
            if (fuente != null) {
                aplicarFormato(a -> StyleConstants.setFontFamily(a, fuente.toString()));
            }
        });
        comboTamano.addActionListener(e -> {
            if (!actualizandoBarra) {
                aplicarFormato(a -> StyleConstants.setFontSize(a, tamanoElegido()));
            }
        });
        btnNegrita.addActionListener(e -> {
            if (!actualizandoBarra) {
                aplicarFormato(a -> StyleConstants.setBold(a, btnNegrita.isSelected()));
            }
        });
        btnCursiva.addActionListener(e -> {
            if (!actualizandoBarra) {
                aplicarFormato(a -> StyleConstants.setItalic(a, btnCursiva.isSelected()));
            }
        });
        btnSubrayado.addActionListener(e -> {
            if (!actualizandoBarra) {
                aplicarFormato(a -> StyleConstants.setUnderline(a, btnSubrayado.isSelected()));
            }
        });
        btnTachado.addActionListener(e -> {
            if (!actualizandoBarra) {
                aplicarFormato(a -> StyleConstants.setStrikeThrough(a, btnTachado.isSelected()));
            }
        });
        btnColor.addActionListener(e -> elegirColor());

        areaTexto.addCaretListener(e -> actualizarBarra());
    }

    private int tamanoElegido() {
        Object valor = comboTamano.getSelectedItem();
        try {
            return Math.max(1, Integer.parseInt(String.valueOf(valor).trim()));
        } catch (NumberFormatException e) {
            return StyleConstants.getFontSize(atributosActuales());
        }
    }

    private void elegirColor() {
        Color inicial = StyleConstants.getForeground(atributosActuales());
        Color color = JColorChooser.showDialog(this, "Color de fuente", inicial);
        if (color != null) {
            aplicarFormato(a -> StyleConstants.setForeground(a, color));
        }
    }

    private void aplicarFormato(Consumer<MutableAttributeSet> cambio) {
        SimpleAttributeSet atributos = new SimpleAttributeSet();
        cambio.accept(atributos);

        int inicio = areaTexto.getSelectionStart();
        int fin = areaTexto.getSelectionEnd();
        if (inicio != fin) {
            StyledDocument doc = areaTexto.getStyledDocument();
            doc.setCharacterAttributes(inicio, fin - inicio, atributos, false);
        } else {
            areaTexto.getInputAttributes().addAttributes(atributos);
        }
        areaTexto.requestFocusInWindow();
    }

    private AttributeSet atributosActuales() {
        int inicio = areaTexto.getSelectionStart();
        if (inicio != areaTexto.getSelectionEnd()) {
            return areaTexto.getStyledDocument().getCharacterElement(inicio).getAttributes();
        }
        return areaTexto.getInputAttributes();
    }

    private void actualizarBarra() {
        actualizandoBarra = true;
        AttributeSet a = atributosActuales();
        comboFuente.setSelectedItem(StyleConstants.getFontFamily(a));
        comboTamano.setSelectedItem(StyleConstants.getFontSize(a));
        btnNegrita.setSelected(StyleConstants.isBold(a));
        btnCursiva.setSelected(StyleConstants.isItalic(a));
        btnSubrayado.setSelected(StyleConstants.isUnderline(a));
        btnTachado.setSelected(StyleConstants.isStrikeThrough(a));
        actualizandoBarra = false;
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignorado) {
            
        }
        SwingUtilities.invokeLater(() -> new VentanaEditor().setVisible(true));
    }
}
