package editordetexto.persistencia;

import editordetexto.modelo.DocumentoEdt;
import editordetexto.modelo.RangoFormato;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Element;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

/** Puente entre el StyledDocument del JTextPane y el DocumentoEdt que se guarda. */
public final class PuenteSwing {

    private PuenteSwing() {
    }

    public static DocumentoEdt desdeSwing(StyledDocument doc) throws BadLocationException {
        int largo = doc.getLength();
        String texto = doc.getText(0, largo);
        List<RangoFormato> rangos = new ArrayList<>();

        int pos = 0;
        while (pos < largo) {
            Element elemento = doc.getCharacterElement(pos);
            int fin = Math.min(elemento.getEndOffset(), largo);
            if (fin <= pos) {
                fin = pos + 1;      // red de seguridad: nunca quedarse en el mismo sitio
            }
            AttributeSet a = elemento.getAttributes();
            rangos.add(new RangoFormato(
                    pos,
                    fin - pos,
                    StyleConstants.getFontFamily(a),
                    StyleConstants.getFontSize(a),
                    StyleConstants.getForeground(a).getRGB(),
                    StyleConstants.isBold(a),
                    StyleConstants.isItalic(a),
                    StyleConstants.isUnderline(a),
                    StyleConstants.isStrikeThrough(a)));
            pos = fin;
        }
        return new DocumentoEdt(texto, rangos);
    }

    public static void haciaSwing(DocumentoEdt documento, StyledDocument doc)
            throws BadLocationException {
        doc.remove(0, doc.getLength());
        doc.insertString(0, documento.texto(), null);

        for (RangoFormato r : documento.rangos()) {
            SimpleAttributeSet a = new SimpleAttributeSet();
            if (r.fuente() != null && !r.fuente().isEmpty()) {
                StyleConstants.setFontFamily(a, r.fuente());
            }
            if (r.tamano() > 0) {
                StyleConstants.setFontSize(a, r.tamano());
            }
            StyleConstants.setForeground(a, new Color(r.colorARGB(), true));
            StyleConstants.setBold(a, r.negrita());
            StyleConstants.setItalic(a, r.cursiva());
            StyleConstants.setUnderline(a, r.subrayado());
            StyleConstants.setStrikeThrough(a, r.tachado());
            doc.setCharacterAttributes(r.inicio(), r.longitud(), a, true);
        }
    }
}
