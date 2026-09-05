package editordetexto.gui;

import editordetexto.modelo.DocumentoEdt;
import editordetexto.persistencia.EdtArchivo;
import editordetexto.persistencia.EdtException;
import editordetexto.persistencia.PuenteSwing;
import Tablas.TablaDocumentos;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
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
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
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
    private final JButton btnTabla = new JButton("Tabla");

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
        if (areaTexto.getDocument().getLength() > 0
                && !confirmar("Nuevo documento",
                        "Se perdera lo que no este guardado. Continuar?")) {
            return;
        }
        areaTexto.setText("");
        archivoActual = null;
        actualizarTitulo();
    }

    private void abrir() {
        JFileChooser selector = crearSelector();
        if (selector.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        File archivo = selector.getSelectedFile();
        try {
            DocumentoEdt documento = EdtArchivo.abrir(archivo);
            PuenteSwing.haciaSwing(documento, areaTexto.getStyledDocument());
            areaTexto.setCaretPosition(0);
            archivoActual = archivo;
            actualizarTitulo();
        } catch (EdtException | BadLocationException e) {
            error("No se pudo abrir", e.getMessage());
        }
    }

    private void guardar() {
        if (archivoActual == null) {
            guardarComo();
        } else {
            escribirEn(archivoActual);
        }
    }

    private void guardarComo() {
        JFileChooser selector = crearSelector();
        if (archivoActual != null) {
            selector.setSelectedFile(archivoActual);
        }
        if (selector.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        File destino = EdtArchivo.asegurarExtension(selector.getSelectedFile());
        if (destino.exists() && !confirmar("Guardar como",
                "\"" + destino.getName() + "\" ya existe. Reemplazarlo?")) {
            return;
        }
        escribirEn(destino);
    }

    private void escribirEn(File archivo) {
        try {
            DocumentoEdt documento = PuenteSwing.desdeSwing(areaTexto.getStyledDocument());
            archivoActual = EdtArchivo.guardar(documento, archivo);
            actualizarTitulo();
        } catch (EdtException | BadLocationException e) {
            error("No se pudo guardar", e.getMessage());
        }
    }

    private void insertarTabla() {
        JSpinner filas = new JSpinner(new SpinnerNumberModel(2, 1, 100, 1));
        JSpinner columnas = new JSpinner(new SpinnerNumberModel(2, 1, 50, 1));

        JPanel panel = new JPanel(new GridLayout(2, 2, 6, 6));
        panel.add(new JLabel("Filas:"));
        panel.add(filas);
        panel.add(new JLabel("Columnas:"));
        panel.add(columnas);

        if (JOptionPane.showConfirmDialog(this, panel, "Insertar tabla",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE)
                != JOptionPane.OK_OPTION) {
            return;
        }
        try {
            TablaDocumentos tabla = new TablaDocumentos(
                    (Integer) filas.getValue(), (Integer) columnas.getValue());
            tabla.insertarEn(areaTexto.getStyledDocument(), areaTexto.getCaretPosition());
        } catch (BadLocationException | IllegalArgumentException e) {
            error("No se pudo insertar la tabla", e.getMessage());
        }
    }

    private JFileChooser crearSelector() {
        JFileChooser selector = new JFileChooser();
        selector.setAcceptAllFileFilterUsed(false);
        selector.setFileFilter(new FileNameExtensionFilter(
                "Documentos del editor (*" + EdtArchivo.EXTENSION + ")", "edt"));
        return selector;
    }

    private void actualizarTitulo() {
        setTitle((archivoActual == null ? "Sin titulo" : archivoActual.getName())
                + " - Editor de Texto");
    }

    private boolean confirmar(String titulo, String mensaje) {
        return JOptionPane.showConfirmDialog(this, mensaje, titulo,
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE)
                == JOptionPane.OK_OPTION;
    }

    private void error(String titulo, String mensaje) {
        JOptionPane.showMessageDialog(this, mensaje, titulo, JOptionPane.ERROR_MESSAGE);
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
        btnTabla.setToolTipText("Insertar tabla");

        barra.add(comboFuente);
        barra.add(comboTamano);
        barra.addSeparator();
        barra.add(btnNegrita);
        barra.add(btnCursiva);
        barra.add(btnSubrayado);
        barra.add(btnTachado);
        barra.addSeparator();
        barra.add(btnColor);
        barra.addSeparator();
        barra.add(btnTabla);
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
        btnTabla.addActionListener(e -> insertarTabla());

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
