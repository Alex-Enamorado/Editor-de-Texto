package editordetexto.persistencia;

import editordetexto.modelo.DocumentoEdt;
import editordetexto.modelo.RangoFormato;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.CRC32;

/** Lectura y escritura del formato binario .edt, descrito en FORMATO_EDT.md. */
public final class EdtArchivo {

    public static final String EXTENSION = ".edt";

    static final byte[] MAGIC = { 'E', 'D', 'T', '1' };
    static final int VERSION_MAYOR = 1;
    static final int VERSION_MENOR = 0;

    static final int SEC_TEXTO = 1;
    static final int SEC_FORMATO = 2;
    static final int SEC_TABLAS = 3;

    private static final int TAM_CABECERA = 8;
    private static final int TAM_CHECKSUM = 4;

    // Topes de cordura: evitan reservar memoria absurda si el archivo esta danado.
    private static final int MAX_SECCION = 64 * 1024 * 1024;
    private static final int MAX_RANGOS = 4_000_000;

    private EdtArchivo() {
    }

    // ---------------------------------------------------------------- escribir

    public static File guardar(DocumentoEdt documento, File archivo) throws EdtException {
        File destino = asegurarExtension(archivo);
        try {
            Files.write(destino.toPath(), serializar(documento));
            return destino;
        } catch (IOException e) {
            throw new EdtException("No se pudo escribir el archivo: " + e.getMessage(), e);
        }
    }

    static byte[] serializar(DocumentoEdt documento) throws EdtException {
        try {
            byte[] secTexto = seccionTexto(documento);
            byte[] secFormato = seccionFormato(documento);
            byte[] secTablas = seccionTablas();

            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(buffer);

            out.write(MAGIC);
            out.writeByte(VERSION_MAYOR);
            out.writeByte(VERSION_MENOR);
            out.writeShort(3);

            escribirSeccion(out, SEC_TEXTO, secTexto);
            escribirSeccion(out, SEC_FORMATO, secFormato);
            escribirSeccion(out, SEC_TABLAS, secTablas);

            out.flush();
            CRC32 crc = new CRC32();
            crc.update(buffer.toByteArray());
            out.writeInt((int) crc.getValue());
            out.flush();

            return buffer.toByteArray();
        } catch (IOException e) {
            throw new EdtException("Error armando el archivo: " + e.getMessage(), e);
        }
    }

    private static void escribirSeccion(DataOutputStream out, int id, byte[] contenido)
            throws IOException {
        out.writeShort(id);
        out.writeInt(contenido.length);
        out.write(contenido);
    }

    private static byte[] seccionTexto(DocumentoEdt documento) throws IOException {
        byte[] utf8 = documento.texto().getBytes(StandardCharsets.UTF_8);
        ByteArrayOutputStream b = new ByteArrayOutputStream();
        DataOutputStream o = new DataOutputStream(b);
        o.writeInt(utf8.length);
        o.write(utf8);
        o.flush();
        return b.toByteArray();
    }

    private static byte[] seccionFormato(DocumentoEdt documento) throws IOException {
        List<RangoFormato> rangos = documento.rangos();
        ByteArrayOutputStream b = new ByteArrayOutputStream();
        DataOutputStream o = new DataOutputStream(b);
        o.writeInt(rangos.size());
        for (RangoFormato r : rangos) {
            o.writeInt(r.inicio());
            o.writeInt(r.longitud());
            o.writeInt(r.colorARGB());
            o.writeShort(r.tamano());
            o.writeByte(r.estilosComoBits());
            byte[] fuente = (r.fuente() == null ? "" : r.fuente())
                    .getBytes(StandardCharsets.UTF_8);
            o.writeShort(fuente.length);
            o.write(fuente);
        }
        o.flush();
        return b.toByteArray();
    }

    private static byte[] seccionTablas() throws IOException {
        ByteArrayOutputStream b = new ByteArrayOutputStream();
        DataOutputStream o = new DataOutputStream(b);
        o.writeInt(0);   // numTablas: reservado para el requisito 3
        o.flush();
        return b.toByteArray();
    }

    // ------------------------------------------------------------------- leer

    public static DocumentoEdt abrir(File archivo) throws EdtException {
        if (archivo == null) {
            throw new EdtException("No se indico ningun archivo.");
        }
        if (!archivo.exists()) {
            throw new EdtException("El archivo no existe: " + archivo.getPath());
        }
        if (archivo.isDirectory()) {
            throw new EdtException("La ruta indicada es una carpeta, no un archivo: "
                    + archivo.getPath());
        }
        if (!tieneExtensionEdt(archivo)) {
            throw new EdtException("Extension incorrecta: se esperaba un archivo "
                    + EXTENSION + " y se recibio \"" + archivo.getName() + "\".");
        }

        byte[] bytes;
        try {
            bytes = Files.readAllBytes(archivo.toPath());
        } catch (IOException e) {
            throw new EdtException("No se pudo leer el archivo: " + e.getMessage(), e);
        }
        return deserializar(bytes, archivo.getName());
    }

    static DocumentoEdt deserializar(byte[] bytes, String nombre) throws EdtException {
        if (bytes.length < TAM_CABECERA + TAM_CHECKSUM) {
            throw new EdtException("El archivo \"" + nombre
                    + "\" esta truncado: tiene " + bytes.length
                    + " bytes y el minimo son " + (TAM_CABECERA + TAM_CHECKSUM) + ".");
        }
        if (bytes[0] != MAGIC[0] || bytes[1] != MAGIC[1]
                || bytes[2] != MAGIC[2] || bytes[3] != MAGIC[3]) {
            throw new EdtException("El archivo \"" + nombre
                    + "\" no es un documento .edt valido (firma incorrecta).");
        }

        DataInputStream in = new DataInputStream(
                new ByteArrayInputStream(bytes, 4, bytes.length - TAM_CHECKSUM - 4));
        try {
            int mayor = in.readUnsignedByte();
            int menor = in.readUnsignedByte();
            if (mayor != VERSION_MAYOR) {
                throw new EdtException("Version de formato no soportada: " + mayor + "." + menor
                        + ". Este editor lee la version " + VERSION_MAYOR + ".x.");
            }

            int numSecciones = in.readUnsignedShort();

            String texto = null;
            List<RangoFormato> rangos = new ArrayList<>();

            for (int i = 0; i < numSecciones; i++) {
                int id = in.readUnsignedShort();
                int longitud = in.readInt();
                if (longitud < 0 || longitud > MAX_SECCION) {
                    throw new EdtException("El archivo \"" + nombre
                            + "\" esta corrupto: la seccion " + id
                            + " declara un tamano invalido (" + longitud + " bytes).");
                }
                byte[] contenido = new byte[longitud];
                in.readFully(contenido);

                switch (id) {
                    case SEC_TEXTO -> texto = leerSeccionTexto(contenido, nombre);
                    case SEC_FORMATO -> rangos = leerSeccionFormato(contenido, nombre);
                    default -> { }   // reservada o de una version futura: se salta
                }
            }

            if (texto == null) {
                throw new EdtException("El archivo \"" + nombre
                        + "\" esta corrupto: falta la seccion de texto.");
            }
            validarRangos(rangos, texto.length(), nombre);

            // El checksum va al final para que un archivo cortado se reporte
            // como truncado (EOF) y no como "checksum incorrecto".
            verificarChecksum(bytes, nombre);
            return new DocumentoEdt(texto, rangos);

        } catch (EOFException e) {
            throw new EdtException("El archivo \"" + nombre
                    + "\" esta truncado: se corto antes de terminar de leerlo.", e);
        } catch (IOException e) {
            throw new EdtException("Error leyendo \"" + nombre + "\": " + e.getMessage(), e);
        }
    }

    private static void verificarChecksum(byte[] bytes, String nombre) throws EdtException {
        int corte = bytes.length - TAM_CHECKSUM;
        CRC32 crc = new CRC32();
        crc.update(bytes, 0, corte);
        int esperado = (int) crc.getValue();
        int guardado = ((bytes[corte] & 0xFF) << 24)
                | ((bytes[corte + 1] & 0xFF) << 16)
                | ((bytes[corte + 2] & 0xFF) << 8)
                | (bytes[corte + 3] & 0xFF);
        if (esperado != guardado) {
            throw new EdtException("El archivo \"" + nombre
                    + "\" esta corrupto o incompleto: el checksum no coincide.");
        }
    }

    private static String leerSeccionTexto(byte[] contenido, String nombre)
            throws IOException, EdtException {
        DataInputStream in = new DataInputStream(new ByteArrayInputStream(contenido));
        int largo = in.readInt();
        if (largo < 0 || largo > contenido.length - 4) {
            throw new EdtException("El archivo \"" + nombre
                    + "\" esta corrupto: la longitud del texto no cuadra con la seccion.");
        }
        byte[] utf8 = new byte[largo];
        in.readFully(utf8);
        return new String(utf8, StandardCharsets.UTF_8);
    }

    private static List<RangoFormato> leerSeccionFormato(byte[] contenido, String nombre)
            throws IOException, EdtException {
        DataInputStream in = new DataInputStream(new ByteArrayInputStream(contenido));
        int cantidad = in.readInt();
        if (cantidad < 0 || cantidad > MAX_RANGOS) {
            throw new EdtException("El archivo \"" + nombre
                    + "\" esta corrupto: cantidad de rangos invalida (" + cantidad + ").");
        }
        List<RangoFormato> rangos = new ArrayList<>(Math.min(cantidad, 1024));
        for (int i = 0; i < cantidad; i++) {
            int inicio = in.readInt();
            int longitud = in.readInt();
            int color = in.readInt();
            int tamano = in.readUnsignedShort();
            int bits = in.readUnsignedByte();
            int largoFuente = in.readUnsignedShort();
            byte[] fuente = new byte[largoFuente];
            in.readFully(fuente);
            rangos.add(RangoFormato.desdeBits(inicio, longitud,
                    new String(fuente, StandardCharsets.UTF_8), tamano, color, bits));
        }
        return rangos;
    }

    private static void validarRangos(List<RangoFormato> rangos, int largoTexto, String nombre)
            throws EdtException {
        for (RangoFormato r : rangos) {
            if (r.inicio() < 0 || r.longitud() < 0
                    || (long) r.inicio() + r.longitud() > largoTexto) {
                throw new EdtException("El archivo \"" + nombre
                        + "\" esta corrupto: hay formato que apunta fuera del texto ("
                        + r.inicio() + "+" + r.longitud() + " sobre " + largoTexto + ").");
            }
        }
    }

    // ---------------------------------------------------------------- utiles

    public static boolean tieneExtensionEdt(File archivo) {
        return archivo.getName().toLowerCase().endsWith(EXTENSION);
    }

    /** Agrega .edt si falta, para que "Guardar como" nunca cree otra extension. */
    public static File asegurarExtension(File archivo) {
        return tieneExtensionEdt(archivo)
                ? archivo
                : new File(archivo.getParentFile(), archivo.getName() + EXTENSION);
    }
}
