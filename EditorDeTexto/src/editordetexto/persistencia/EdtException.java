package editordetexto.persistencia;

/** Error al leer o escribir un .edt. El mensaje ya viene listo para mostrarse. */
public class EdtException extends Exception {

    public EdtException(String mensaje) {
        super(mensaje);
    }

    public EdtException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
