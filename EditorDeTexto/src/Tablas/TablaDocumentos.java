/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Tablas;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import javax.swing.text.BadLocationException;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

/**
 *
 * @author diego
 */

public final class TablaDocumentos extends JPanel {

    private static final int FIRMA = 0x54424C31; // "TBL1"
    private static final int VERSION = 1;


    private static final int MAX_FILAS = 1000;
    private static final int MAX_COLUMNAS = 100;
    private static final int MAX_CELDAS = 10000;
    private static final int MAX_BYTES_CELDA = 1_000_000;
    private static final long MAX_BYTES_TABLA = 16_000_000;

    private final DefaultTableModel modelo;
    private final JTable tabla;

    public TablaDocumentos(int filas, int columnas) {
        super(new BorderLayout());

        validarDimensiones(filas, columnas);

        modelo = new DefaultTableModel(filas, columnas) {
            @Override
            public Class<?> getColumnClass(int columna) {
                return String.class;
            }

            @Override
            public boolean isCellEditable(int fila, int columna) {
                return true;
            }
        };

        tabla = new JTable(modelo);
        tabla.setRowHeight(24);
        tabla.setCellSelectionEnabled(true);
        tabla.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        tabla.putClientProperty("terminateEditOnFocusLost", true);

     
        tabla.setTableHeader(null);

        for (int columna = 0; columna < columnas; columna++) {
            tabla.getColumnModel() .getColumn(columna).setPreferredWidth(120);
        }

        tabla.setPreferredScrollableViewportSize(
                new Dimension(Math.min(columnas * 120, 720),Math.min(filas * 24, 288) )
      );

        JScrollPane desplazamiento = new JScrollPane(tabla);
        desplazamiento.setColumnHeaderView(null);
        add(desplazamiento, BorderLayout.CENTER);

   
        setMaximumSize(getPreferredSize());
        setAlignmentY(0.8f);
    }

    public int obtenerFilas() {
        return modelo.getRowCount();
    }

    public int obtenerColumnas() {
        return modelo.getColumnCount();
    }


    public String obtenerTexto(int fila, int columna) {
        validarCelda(fila, columna);
        finalizarEdicion();
        return leerCelda(fila, columna);
    }

 
    public void establecerTexto(int fila, int columna, String texto) {
        validarCelda(fila, columna);

        if (texto == null) {
            throw new IllegalArgumentException( "El texto no puede ser null; usa \"\" para una celda vacía.");
        }

        if (texto.getBytes(StandardCharsets.UTF_8).length > MAX_BYTES_CELDA) {
            throw new IllegalArgumentException("El contenido de la celda supera el límite permitido." );
        }

        finalizarEdicion();
        modelo.setValueAt(texto, fila, columna);
    }


    public void insertarEn(StyledDocument documento, int posicion)
            throws BadLocationException {

        if (documento == null) {
            throw new IllegalArgumentException( "El documento no puede ser null." );
        }

        if (posicion < 0 || posicion > documento.getLength()) {
            throw new IllegalArgumentException(
                    "La posición está fuera del documento."
            );
        }

        SimpleAttributeSet atributos = new SimpleAttributeSet();
        StyleConstants.setComponent(atributos, this);

        documento.insertString(posicion, "\uFFFC", atributos);
    }


    public void finalizarEdicion() {
        if (tabla.isEditing()
                && !tabla.getCellEditor().stopCellEditing()) {
            throw new IllegalStateException(
                    "No se pudo finalizar la edición de la celda."
            );
        }
    }


    public void guardarBinario(DataOutput salida) throws IOException {
        if (salida == null) {
            throw new IllegalArgumentException( "La salida no puede ser null."  );
        }

        finalizarEdicion();


        byte[][] contenidos =
                new byte[obtenerFilas() * obtenerColumnas()][];

        long totalBytes = 0;
        int indice = 0;

        for (int fila = 0; fila < obtenerFilas(); fila++) {
            for (int columna = 0; columna < obtenerColumnas(); columna++) {
                byte[] bytes = leerCelda(fila, columna)
                        .getBytes(StandardCharsets.UTF_8);

                totalBytes += bytes.length;

                if (bytes.length > MAX_BYTES_CELDA || totalBytes > MAX_BYTES_TABLA) {
                    throw new IOException(
                            "El contenido de la tabla supera el límite."
                    );
                }

                contenidos[indice++] = bytes;
            }
        }

        salida.writeInt(FIRMA);
        salida.writeInt(VERSION);
        salida.writeInt(obtenerFilas());
        salida.writeInt(obtenerColumnas());

        for (byte[] bytes : contenidos) {
            salida.writeInt(bytes.length);
            salida.write(bytes);
        }
    }


    public static TablaDocumentos leerBinario(DataInput entrada)
            throws IOException {

        if (entrada == null) {
            throw new IllegalArgumentException(
                    "La entrada no puede ser null."
            );
        }

        if (entrada.readInt() != FIRMA) {
            throw new IOException("La firma de la tabla es inválida.");
        }

        if (entrada.readInt() != VERSION) {
            throw new IOException("Versión de tabla no compatible.");
        }

        int filas = entrada.readInt();
        int columnas = entrada.readInt();

        try {
            validarDimensiones(filas, columnas);
        } 
        
        catch (IllegalArgumentException e) {
            throw new IOException( "Las dimensiones de la tabla son inválidas.", e);
        }

        TablaDocumentos resultado = new TablaDocumentos(filas, columnas);
        long totalBytes = 0;

        for (int fila = 0; fila < filas; fila++) {
            for (int columna = 0; columna < columnas; columna++) {
                int longitud = entrada.readInt();

                if (longitud < 0 || longitud > MAX_BYTES_CELDA) {
                    throw new IOException( "La longitud de una celda es inválida." );
                }

                totalBytes += longitud;

                if (totalBytes > MAX_BYTES_TABLA) {
                    throw new IOException("El contenido de la tabla supera el límite.");
                }

                byte[] bytes = new byte[longitud];
                entrada.readFully(bytes);

                String texto = decodificarTexto(bytes);
                resultado.establecerTexto(fila, columna, texto);
            }
        }

        return resultado;
    }

    private String leerCelda(int fila, int columna) {
        Object valor = modelo.getValueAt(fila, columna);
        return valor == null ? "" : valor.toString();
    }

    private void validarCelda(int fila, int columna) {
        if (fila < 0 || fila >= obtenerFilas() || columna < 0 || columna >= obtenerColumnas()) {
            throw new IllegalArgumentException( "La fila o columna está fuera de la tabla.");
        }
    }

    private static void validarDimensiones(int filas, int columnas) {
        if (filas <= 0 || columnas <= 0 || filas > MAX_FILAS|| columnas > MAX_COLUMNAS|| (long) filas * columnas > MAX_CELDAS) {
            throw new IllegalArgumentException(
                    "La tabla debe tener entre 1 y " + MAX_FILAS + " filas, entre 1 y " + MAX_COLUMNAS+ " columnas y un máximo de " + MAX_CELDAS + " celdas." );
        }
    }

    private static String decodificarTexto(byte[] bytes)
            throws IOException {

        try {
           
            return StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT) .onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(bytes)).toString();
            
            
        } catch (CharacterCodingException e) {
            throw new IOException("Una celda contiene texto UTF-8 inválido.", e );
        }
    }
}
