package editordetexto.modelo;

/**
 * Tramo de texto con formato homogeneo. Los indices son posiciones de caracter
 * Java (UTF-16), iguales a los offsets del Document de Swing.
 */
public record RangoFormato(
        int inicio,
        int longitud,
        String fuente,
        int tamano,
        int colorARGB,
        boolean negrita,
        boolean cursiva,
        boolean subrayado,
        boolean tachado) {

    public int estilosComoBits() {
        int b = 0;
        if (negrita)   b |= 1;
        if (cursiva)   b |= 2;
        if (subrayado) b |= 4;
        if (tachado)   b |= 8;
        return b;
    }

    public static RangoFormato desdeBits(int inicio, int longitud, String fuente,
                                         int tamano, int colorARGB, int bits) {
        return new RangoFormato(inicio, longitud, fuente, tamano, colorARGB,
                (bits & 1) != 0, (bits & 2) != 0, (bits & 4) != 0, (bits & 8) != 0);
    }
}
