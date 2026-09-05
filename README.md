# Editor de Texto

Proyecto NetBeans Java SE (Swing). Reparto del trabajo:

| Requisito | Responsable | Estado |
|---|---|---|
| 1. Formato de texto | Diego | listo |
| 2. Persistencia binaria `.edt` | Marcelo | listo |
| 3. Tablas | Diego + Marcelo | crear, editar y guardar en el `.edt`; falta formato dentro de las celdas |
| 4. Interfaz gráfica | Alex | listo |

## Cómo se conecta la persistencia con lo demás

Todo pasa por el `StyledDocument` del `JTextPane`. Nadie necesita conocer el
formato binario.

**Guardar** (menú Archivo → Guardar / Guardar como):

```java
DocumentoEdt doc = PuenteSwing.desdeSwing(textPane.getStyledDocument());
archivoActual = EdtArchivo.guardar(doc, archivoElegido);   // agrega .edt si falta
```

**Abrir** (menú Archivo → Abrir):

```java
try {
    DocumentoEdt doc = EdtArchivo.abrir(archivoElegido);
    PuenteSwing.haciaSwing(doc, textPane.getStyledDocument());
} catch (EdtException | BadLocationException e) {
    JOptionPane.showMessageDialog(ventana, e.getMessage(),
            "No se pudo abrir", JOptionPane.ERROR_MESSAGE);
}
```

El mensaje de `EdtException` ya viene redactado en español para mostrarse tal cual.

El formato de texto (requisito 1) se guarda solo: cualquier atributo que se
aplique con `StyleConstants` sobre el `StyledDocument` —color, familia,
tamaño, negrita, cursiva, subrayado, tachado— lo recoge `PuenteSwing`.

Las tablas también: `PuenteSwing` reconoce cualquier `TablaDocumentos` insertada
con `insertarEn(...)` y la guarda en la sección 3 del archivo, en su posición.

**Pendiente del requisito 3:** las celdas guardan texto plano. Para tener formato
dentro de las celdas hay que cambiar `TablaDocumentos` (celdas con `JTextPane` en
vez de `String`) y ampliar el bloque `TBL1`; el `.edt` no habría que tocarlo,
porque guarda el bloque de la tabla sin interpretarlo.

## Estructura del archivo binario

Documentada en [FORMATO_EDT.md](FORMATO_EDT.md).

## Probar la persistencia sin la GUI

Ejecutar `editordetexto.PruebaPersistencia`: guarda un documento con formato
mezclado y una tabla, lo reabre, compara, y fuerza los casos de error (archivo
inexistente, extensión equivocada, corrupto, truncado, firma inválida).
