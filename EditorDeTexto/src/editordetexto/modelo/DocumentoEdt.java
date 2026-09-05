package editordetexto.modelo;

import java.util.ArrayList;
import java.util.List;

/** Documento en memoria, independiente de la interfaz. */
public class DocumentoEdt {

    private final String texto;
    private final List<RangoFormato> rangos;

    public DocumentoEdt(String texto, List<RangoFormato> rangos) {
        this.texto = texto == null ? "" : texto;
        this.rangos = new ArrayList<>(rangos);
    }

    public String texto() {
        return texto;
    }

    public List<RangoFormato> rangos() {
        return rangos;
    }

    @Override
    public String toString() {
        return "DocumentoEdt[" + texto.length() + " caracteres, "
                + rangos.size() + " rangos de formato]";
    }
}
