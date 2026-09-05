package editordetexto.modelo;

import java.util.Arrays;

/**
 * Una tabla dentro del documento: en que posicion del texto va, y su bloque
 * binario TBL1 ya serializado por TablaDocumentos.
 */
public record TablaEmbebida(int posicion, byte[] datos) {

    @Override
    public boolean equals(Object otro) {
        return otro instanceof TablaEmbebida t
                && t.posicion == posicion && Arrays.equals(t.datos, datos);
    }

    @Override
    public int hashCode() {
        return 31 * posicion + Arrays.hashCode(datos);
    }

    @Override
    public String toString() {
        return "TablaEmbebida[posicion=" + posicion + ", " + datos.length + " bytes]";
    }
}
