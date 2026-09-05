package editordetexto;

import editordetexto.modelo.DocumentoEdt;
import editordetexto.modelo.RangoFormato;
import editordetexto.persistencia.EdtArchivo;
import editordetexto.persistencia.EdtException;
import editordetexto.persistencia.PuenteSwing;

import java.awt.Color;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

/** Prueba del requisito 2 sin la interfaz grafica: ida y vuelta + casos de error. */
public class PruebaPersistencia {

    private static int fallos = 0;

    public static void main(String[] args) throws Exception {
        Path carpeta = Files.createTempDirectory("pruebaEdt");

        pruebaIdaYVuelta(carpeta);
        pruebaBytePorByte(carpeta);
        pruebaArchivoNoExiste(carpeta);
        pruebaExtensionEquivocada(carpeta);
        pruebaArchivoCorrupto(carpeta);
        pruebaArchivoTruncado(carpeta);

        System.out.println();
        System.out.println(fallos == 0 ? "TODO OK" : (fallos + " PRUEBA(S) FALLARON"));
    }

    private static void pruebaIdaYVuelta(Path carpeta) throws Exception {
        StyledDocument doc = new DefaultStyledDocument();

        SimpleAttributeSet rojo = new SimpleAttributeSet();
        StyleConstants.setForeground(rojo, Color.RED);
        StyleConstants.setBold(rojo, true);
        StyleConstants.setFontSize(rojo, 16);
        StyleConstants.setFontFamily(rojo, "Arial");

        SimpleAttributeSet raro = new SimpleAttributeSet();
        StyleConstants.setForeground(raro, new Color(18, 200, 77));
        StyleConstants.setItalic(raro, true);
        StyleConstants.setUnderline(raro, true);
        StyleConstants.setStrikeThrough(raro, true);
        StyleConstants.setFontSize(raro, 28);
        StyleConstants.setFontFamily(raro, "Times New Roman");

        doc.insertString(0, "Hola ", null);
        doc.insertString(doc.getLength(), "rojo negrita 16 Arial", rojo);
        doc.insertString(doc.getLength(), "\nacentos: ninos, camion, áéíóú", raro);

        DocumentoEdt original = PuenteSwing.desdeSwing(doc);
        File archivo = EdtArchivo.guardar(original, new File(carpeta.toFile(), "demo.edt"));

        DocumentoEdt leido = EdtArchivo.abrir(archivo);
        StyledDocument doc2 = new DefaultStyledDocument();
        PuenteSwing.haciaSwing(leido, doc2);
        DocumentoEdt revuelta = PuenteSwing.desdeSwing(doc2);

        verificar("texto identico", original.texto().equals(leido.texto()));
        verificar("formato identico", mismosRangos(original.rangos(), leido.rangos()));
        verificar("ida y vuelta por Swing estable",
                mismosRangos(original.rangos(), revuelta.rangos())
                        && original.texto().equals(revuelta.texto()));
        System.out.println("   " + original + " -> " + archivo.length() + " bytes");
    }

    private static void pruebaBytePorByte(Path carpeta) throws Exception {
        DocumentoEdt d = new DocumentoEdt("abc", List.of(
                new RangoFormato(0, 3, "Arial", 14, Color.BLUE.getRGB(),
                        true, false, true, false)));
        File a = EdtArchivo.guardar(d, new File(carpeta.toFile(), "a.edt"));
        File b = EdtArchivo.guardar(EdtArchivo.abrir(a), new File(carpeta.toFile(), "b.edt"));
        verificar("reescritura byte por byte",
                java.util.Arrays.equals(Files.readAllBytes(a.toPath()),
                        Files.readAllBytes(b.toPath())));
    }

    private static void pruebaArchivoNoExiste(Path carpeta) {
        esperarError("archivo inexistente",
                new File(carpeta.toFile(), "no_esta.edt"));
    }

    private static void pruebaExtensionEquivocada(Path carpeta) throws Exception {
        File txt = new File(carpeta.toFile(), "documento.txt");
        Files.writeString(txt.toPath(), "esto no es un edt");
        esperarError("extension equivocada", txt);
    }

    private static void pruebaArchivoCorrupto(Path carpeta) throws Exception {
        File origen = new File(carpeta.toFile(), "demo.edt");
        byte[] bytes = Files.readAllBytes(origen.toPath());
        bytes[bytes.length / 2] ^= 0x5A;
        File roto = new File(carpeta.toFile(), "roto.edt");
        Files.write(roto.toPath(), bytes);
        esperarError("archivo corrupto", roto);

        byte[] sinFirma = Files.readAllBytes(origen.toPath());
        sinFirma[0] = 'X';
        File otro = new File(carpeta.toFile(), "firma.edt");
        Files.write(otro.toPath(), sinFirma);
        esperarError("firma invalida", otro);
    }

    private static void pruebaArchivoTruncado(Path carpeta) throws Exception {
        byte[] bytes = Files.readAllBytes(new File(carpeta.toFile(), "demo.edt").toPath());
        File corto = new File(carpeta.toFile(), "corto.edt");
        Files.write(corto.toPath(), java.util.Arrays.copyOf(bytes, bytes.length / 2));
        esperarError("archivo truncado", corto);
    }

    private static void esperarError(String caso, File archivo) {
        try {
            EdtArchivo.abrir(archivo);
            verificar(caso + " -> deberia haber fallado", false);
        } catch (EdtException e) {
            verificar(caso, true);
            System.out.println("   mensaje: " + e.getMessage());
        }
    }

    private static boolean mismosRangos(List<RangoFormato> a, List<RangoFormato> b) {
        return a.equals(b);
    }

    private static void verificar(String nombre, boolean ok) {
        if (!ok) {
            fallos++;
        }
        System.out.println((ok ? "[OK]   " : "[FALLA] ") + nombre);
    }
}
