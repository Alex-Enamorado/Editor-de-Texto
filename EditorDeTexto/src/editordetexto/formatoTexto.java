
package editordetexto;

import java.awt.Color;
import java.util.Objects;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

public final class formatoTexto {

    private final StyledDocument documento;

    public formatoTexto(StyledDocument documento) {
        this.documento = Objects.requireNonNull(documento, "El documento no puede ser null.");
    }


    public void aplicarColor(int inicio, int fin, Color color) {
        Objects.requireNonNull(color, "El color no puede ser null.");

        SimpleAttributeSet atributos = new SimpleAttributeSet();
        StyleConstants.setForeground(atributos, color);

        aplicar(inicio, fin, atributos);
    }


    public void aplicarNegrita(int inicio, int fin, boolean activar) {
        SimpleAttributeSet atributos = new SimpleAttributeSet();
        StyleConstants.setBold(atributos, activar);

        aplicar(inicio, fin, atributos);
    }


    public void aplicarCursiva(int inicio, int fin, boolean activar) {
        SimpleAttributeSet atributos = new SimpleAttributeSet();
        StyleConstants.setItalic(atributos, activar);

        aplicar(inicio, fin, atributos);
    }


    public void aplicarSubrayado(int inicio, int fin, boolean activar) {
        SimpleAttributeSet atributos = new SimpleAttributeSet();
        StyleConstants.setUnderline(atributos, activar);

        aplicar(inicio, fin, atributos);
    }

  
    public void aplicarTachado(int inicio, int fin, boolean activar) {
        SimpleAttributeSet atributos = new SimpleAttributeSet();
        StyleConstants.setStrikeThrough(atributos, activar);

        aplicar(inicio, fin, atributos);
    }

   
    public void cambiarFuente(int inicio, int fin, String fuente) {
        String fuenteValidada = validarFuente(fuente);

        SimpleAttributeSet atributos = new SimpleAttributeSet();
        StyleConstants.setFontFamily(atributos, fuenteValidada);

        aplicar(inicio, fin, atributos);
    }

 
    public void cambiarTamano(int inicio, int fin, int tamano) {
        validarTamano(tamano);

        SimpleAttributeSet atributos = new SimpleAttributeSet();
        StyleConstants.setFontSize(atributos, tamano);

        aplicar(inicio, fin, atributos);
    }


    public void aplicarFormatoCompleto(
            int inicio,
            int fin,
            Color color,
            String fuente,
            int tamano,
            boolean negrita,
            boolean cursiva,
            boolean subrayado,
            boolean tachado) {

        Objects.requireNonNull(color, "El color no puede ser null.");
        String fuenteValidada = validarFuente(fuente);
        validarTamano(tamano);

        SimpleAttributeSet atributos = new SimpleAttributeSet();

        StyleConstants.setForeground(atributos, color);
        StyleConstants.setFontFamily(atributos, fuenteValidada);
        StyleConstants.setFontSize(atributos, tamano);
        
        StyleConstants.setBold(atributos, negrita);
        StyleConstants.setItalic(atributos, cursiva);
        StyleConstants.setUnderline(atributos, subrayado);
        StyleConstants.setStrikeThrough(atributos, tachado);

        aplicar(inicio, fin, atributos);
    }


    private void aplicar(
            int inicio,
            int fin,
            SimpleAttributeSet atributos) {

        validarSeleccion(inicio, fin);

        documento.setCharacterAttributes(
                inicio,
                fin - inicio,
                atributos,
                false
        );
    }

    private void validarSeleccion(int inicio, int fin) {
        if (inicio < 0 || fin < inicio || fin > documento.getLength()) {
            throw new IllegalArgumentException(
                    "La selección debe cumplir: 0 <= inicio <= fin <= "
                            + documento.getLength() + "."
            );
        }

        if (inicio == fin) {
            throw new IllegalArgumentException(
                    "Debes seleccionar texto para aplicar formato."
            );
        }
    }

    private String validarFuente(String fuente) {
        if (fuente == null || fuente.trim().isEmpty()) {
            throw new IllegalArgumentException(
                    "La familia tipográfica no puede estar vacía."
            );
        }

        return fuente.trim();
    }

    private void validarTamano(int tamano) {
        if (tamano <= 0) {
            throw new IllegalArgumentException(
                    "El tamaño de fuente debe ser mayor que cero."
            );
        }
    }
}
