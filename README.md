# Editor de Texto

Proyecto NetBeans Java SE (Swing). Reparto del trabajo:

| Requisito | Responsable | Estado |
|---|---|---|
| 1. Formato de texto | Diego | pendiente |
| 2. Persistencia binaria `.edt` | Marcelo | **listo** |
| 3. Tablas | — | fuera de alcance (el enunciado lo pide para equipos de 4) |
| 4. Interfaz gráfica | Alex | pendiente |

## Cómo se conecta la persistencia con lo demás

Todo pasa por el `StyledDocument` del `JTextPane`. Nadie necesita conocer el
formato binario.

**Guardar** (menú Archivo → Guardar / Guardar como):

```java
DocumentoEdt doc = PuenteSwing.desdeSwing(textPane.getStyledDocument());
File guardado = EdtArchivo.guardar(doc, archivoElegido);   // agrega .edt si falta
```

**Abrir** (menú Archivo → Abrir):

```java
try {
    DocumentoEdt doc = EdtArchivo.abrir(archivoElegido);
    PuenteSwing.haciaSwing(doc, textPane.getStyledDocument());
} catch (EdtException e) {
    JOptionPane.showMessageDialog(ventana, e.getMessage(),
            "No se pudo abrir", JOptionPane.ERROR_MESSAGE);
}
```

El mensaje de `EdtException` ya viene redactado en español para mostrarse tal cual.

El formato de texto (requisito 1) se guarda solo: cualquier atributo que se
aplique con `StyleConstants` sobre el `StyledDocument` —color, familia,
tamaño, negrita, cursiva, subrayado, tachado— lo recoge `PuenteSwing`.

## Estructura del archivo binario

Documentada en [FORMATO_EDT.md](FORMATO_EDT.md).

## Probar la persistencia sin la GUI

Ejecutar `editordetexto.PruebaPersistencia`: guarda un documento con formato
mezclado, lo reabre, compara, y fuerza los casos de error (archivo
inexistente, extensión equivocada, corrupto, truncado, firma inválida).
