# Formato de archivo `.edt`

Formato binario propio del editor. No usa ninguna biblioteca de formato
externa: se escribe y se lee con streams de bytes de la JDK
(`DataOutputStream` / `DataInputStream`).

Implementado en `EditorDeTexto/src/editordetexto/persistencia/EdtArchivo.java`.

## Convenciones

| Concepto | Regla |
|---|---|
| Orden de bytes | **big-endian** (el que usa `DataOutputStream` por defecto) |
| Cadenas | UTF-8, siempre precedidas por su longitud **en bytes** |
| Índices de formato | posición de **carácter Java (UTF-16)**, no de byte. Coinciden con los offsets del `Document` de Swing |
| Orden | fijo: cabecera → secciones en orden creciente de id → checksum |

Tipos: `u8` = 1 byte sin signo, `u16` = 2 bytes, `u32` = 4 bytes, `s32` = 4 bytes con signo.

## Estructura general

```
+---------------------------+
| CABECERA        8 bytes   |
+---------------------------+
| SECCIÓN 1  (texto)        |
| SECCIÓN 2  (formato)      |
| SECCIÓN 3  (tablas)       |
+---------------------------+
| CHECKSUM        4 bytes   |
+---------------------------+
```

### Cabecera (8 bytes)

| Offset | Tipo | Campo | Valor |
|---|---|---|---|
| 0 | char[4] | magia | `"EDT1"` (`45 44 54 31`) |
| 4 | u8 | versiónMayor | `1` |
| 5 | u8 | versiónMenor | `0` |
| 6 | u16 | númeroDeSecciones | `3` |

### Sección (se repite `númeroDeSecciones` veces)

| Tipo | Campo |
|---|---|
| u16 | idSección — `1` texto, `2` formato, `3` tablas |
| u32 | longitud del contenido en bytes |
| byte[longitud] | contenido |

Como cada sección lleva su longitud por delante, un lector de una versión
vieja puede **saltarse** una sección que no conoce sin romperse.

#### Sección 1 — Texto (id = 1)

| Tipo | Campo |
|---|---|
| u32 | longitud del texto en bytes UTF-8 |
| byte[…] | texto en UTF-8 |

Es el texto plano completo, con `\n` como salto de línea.

#### Sección 2 — Formato (id = 2)

| Tipo | Campo |
|---|---|
| u32 | cantidadDeRangos |

y luego, por cada rango:

| Tipo | Campo | Notas |
|---|---|---|
| s32 | inicio | índice de carácter dentro del texto |
| s32 | longitud | cantidad de caracteres que abarca |
| u32 | color | `0xAARRGGBB` |
| u16 | tamaño | tamaño de fuente en puntos |
| u8 | estilos | máscara de bits: `1` negrita, `2` cursiva, `4` subrayado, `8` tachado |
| u16 | longitudFuente | bytes UTF-8 del nombre |
| byte[…] | fuente | familia tipográfica, ej. `"Arial"` |

Los rangos son contiguos y cubren todo el texto. Como cada rango lleva a la
vez color, tamaño, familia y los cuatro estilos, **cualquier combinación
queda guardada** (ej. rojo + negrita + 16 + Arial es un solo rango).

#### Sección 3 — Tablas (id = 3) — *reservada*

| Tipo | Campo |
|---|---|
| u32 | cantidadDeTablas — hoy siempre `0` |

Sección reservada para el requisito 3. Se escribe vacía para que un archivo
generado hoy siga siendo legible si más adelante se agregan tablas.

### Checksum (4 bytes)

`u32` con el **CRC32 de todos los bytes anteriores** del archivo (desde la
magia hasta el final de la última sección). Detecta corrupción y archivos
incompletos.

## Ejemplo real

Documento: texto `"Hola"`, todo en Arial 16, negrita, rojo (`0xFFFF0000`).

```
0000  45 44 54 31 01 00 00 03  |EDT1....|   magia, v1.0, 3 secciones
0008  00 01 00 00 00 08        |........|   sección 1 (texto), 8 bytes
000E  00 00 00 04 48 6F 6C 61  |....Hola|   longitud 4 + "Hola"
0016  00 02 00 00 00 1A        |........|   sección 2 (formato), 26 bytes
001C  00 00 00 01              |........|   1 rango
0020  00 00 00 00 00 00 00 04  |........|   inicio 0, longitud 4
0028  FF FF 00 00 00 10 01     |........|   color ARGB, tamaño 16, estilos=1 (negrita)
002F  00 05 41 72 69 61 6C     |..Arial |   longitud 5 + "Arial"
0036  00 03 00 00 00 04        |........|   sección 3 (tablas), 4 bytes
003C  00 00 00 00              |........|   0 tablas
0040  FB D2 73 74              |..st    |   CRC32
```

Total: 68 bytes.

## Manejo de errores al abrir

`EdtArchivo.abrir(File)` lanza `EdtException` con un mensaje listo para
mostrar en un diálogo. Las validaciones van en este orden:

| Caso | Cómo se detecta | Mensaje |
|---|---|---|
| No existe | `!archivo.exists()` | «El archivo no existe: …» |
| Es una carpeta | `isDirectory()` | «La ruta indicada es una carpeta…» |
| Extensión equivocada | el nombre no termina en `.edt` | «Extensión incorrecta: se esperaba…» |
| Demasiado corto | menos de 12 bytes | «…está truncado: tiene N bytes…» |
| No es un `.edt` | los 4 primeros bytes no son `EDT1` | «…no es un documento .edt válido (firma incorrecta)» |
| Versión futura | versiónMayor ≠ 1 | «Versión de formato no soportada…» |
| Truncado a la mitad | se acaban los bytes al leer (`EOFException`) | «…está truncado: se cortó antes de terminar de leerlo» |
| Longitudes absurdas | sección > 64 MB o rangos > 4 000 000 | «…está corrupto: tamaño inválido» |
| Formato fuera del texto | `inicio + longitud > texto.length()` | «…hay formato que apunta fuera del texto» |
| Bytes alterados | el CRC32 no coincide | «…está corrupto o incompleto: el checksum no coincide» |

Los topes de tamaño existen para que un archivo dañado no haga reservar
memoria absurda antes de fallar.

La firma se comprueba **antes** que el checksum y el checksum **al final**,
para que cada caso dé el mensaje que corresponde en vez de que todo termine
como «checksum incorrecto».

Al **guardar**, si el archivo elegido no termina en `.edt` la extensión se
agrega sola (`EdtArchivo.asegurarExtension`), así «Guardar como» nunca
produce un archivo con la extensión equivocada.
