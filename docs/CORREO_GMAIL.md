# Enviar el comprobante por Gmail desde el programa

El programa manda el correo directamente con SMTP (smtp.gmail.com, puerto 587), sin pasar por la API.
Al cliente le llega un correo con el comprobante y los boletos con QR en un PDF adjunto.

## 1. Crear la contraseña de aplicación de Gmail (una sola vez)

Gmail no deja que un programa use la contraseña normal de la cuenta. Hay que crear una *contraseña de aplicación*:

1. Entra a tu cuenta de Google > **Seguridad** y activa la **Verificación en dos pasos** (si no está activa).
2. Abre https://myaccount.google.com/apppasswords
3. Escribe un nombre (por ejemplo `Boleteria`) y pulsa **Crear**.
4. Copia la contraseña de 16 letras que aparece.

Si no te aparece la opción de contraseñas de aplicación, casi siempre es porque la cuenta es de una organización o
centro educativo (por ejemplo, una cuenta de la universidad): usa una cuenta personal de Gmail.

## 2. Crear el archivo correo.properties

Copia `correo.properties.ejemplo` como `correo.properties` **en la carpeta del proyecto, junto al pom.xml** (si usas el .jar, también se encuentra ahí, o puedes dejarlo junto al .jar), y escribe tus datos:

    usuario=micuenta@gmail.com
    clave=abcd efgh ijkl mnop

(En NetBeans: pestaña **Archivos**, clic derecho al proyecto > Nuevo > Archivo vacío.)
Este archivo no se sube a GitHub (está en `.gitignore`); no lo compartas porque tiene la clave.
Otra opción, sin archivo: las variables de entorno `GMAIL_USUARIO` y `GMAIL_CLAVE`.

## 3. Usarlo

- **Automático:** al cobrar una venta, si el cliente tiene un correo válido, se le envía el comprobante.
  Si no existe `correo.properties`, la venta funciona igual pero no se envía nada.
- **Manual:** módulo **Comprobantes y QR** > *Enviar comprobante por correo (Gmail)*: escribes el número de una venta
  ya registrada, confirmas o corriges el correo y se envía.

El correo sale de la cuenta de Gmail configurada; ahí mismo aparece en *Enviados*.

## Problemas comunes

| Mensaje | Qué hacer |
| --- | --- |
| No se encontró el archivo correo.properties en esta carpeta | El mensaje muestra la carpeta donde busca: crea el archivo **ahí**, junto al pom.xml. En NetBeans, al ejecutar el proyecto, es la carpeta del proyecto. En Windows, activa "Extensiones de nombre de archivo" para comprobar que no se llame `correo.properties.txt` (también se acepta, pero conviene que quede sin `.txt`). |
| No se pudo enviar el correo. Detalle técnico: ... | Copia ese detalle completo y compártelo: dice la causa exacta. Si ejecutas con `java -jar` desde `cmd`, en esa ventana sale aún más información. |
| El archivo existe, pero le falta usuario o clave | Cada dato va en su propia línea y con esos nombres exactos: `usuario=...` y `clave=...`. |
| Gmail rechazó el usuario o la contraseña | Se usó la contraseña normal en vez de la de aplicación, o está mal copiada. Crea otra en el paso 1. |
| No se pudo enviar... revisa tu conexión | No hay internet o la red bloquea el puerto 587 (algunas redes de universidades lo bloquean). Prueba con otra red. |
| El cliente no tiene un correo válido | Edita el cliente en el módulo Clientes y escribe su correo. |

Límite de Gmail gratuito: alrededor de 500 correos por día. Si cambias la contraseña de la cuenta de Google, hay que crear otra contraseña de aplicación.
