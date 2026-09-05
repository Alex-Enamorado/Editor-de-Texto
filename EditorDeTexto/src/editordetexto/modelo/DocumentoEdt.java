package editordetexto.modelo;

import java.util.ArrayList;
import java.util.List;

/** Documento en memoria, independiente de la interfaz. */
public class DocumentoEdt {

    private final String texto;
    private final List<RangoFormato> rangos;
    private final List<TablaEmbebida> tablas;

    public DocumentoEdt(String texto, List<RangoFormato> rangos) {
        this(texto, rangos, List.of());
    }

    public DocumentoEdt(String texto, List<RangoFormato> rangos, List<TablaEmbebida> tablas) {
        this.texto = texto == null ? "" : texto;
        this.rangos = new ArrayList<>(rangos);
        this.tablas = new ArrayList<>(tablas);
    }

    public String texto() {
        return texto;
    }

    public List<RangoFormato> rangos() {
        return rangos;
    }

    public List<TablaEmbebida> tablas() {
        return tablas;
    }

    @Override
    public String toString() {
        return "DocumentoEdt[" + texto.length() + " caracteres, "
                + rangos.size() + " rangos de formato, " + tablas.size() + " tablas]";
    }
}
