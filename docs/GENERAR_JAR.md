# Generar el archivo .jar ejecutable

El proyecto ya trae todo preparado en el `pom.xml`: un perfil llamado **jar-ejecutable** que junta el programa y todas sus
librerías (JavaFX, JasperReports, correo, etc.) en un solo archivo:

    target/proyecto-1.0-SNAPSHOT-all.jar

## Requisitos
- **JDK 24 o superior** (el proyecto usa JavaFX 26, que no funciona con Java más viejo). Se recomienda JDK 25 o 26.
- Compilarlo **en el mismo sistema operativo donde se va a usar** (en Windows, para Windows): el .jar lleva dentro las
  librerías de JavaFX de la computadora donde se genera. Un .jar hecho en Linux o Mac no abre en Windows.
- Internet la primera vez, para que Maven descargue las librerías.

## Opción 1: desde NetBeans
1. Clic derecho al proyecto > **Run Maven** > **Goals...**
2. En **Goals** escribe: `clean package`
3. En **Profiles** escribe: `jar-ejecutable`
4. Pulsa **OK** y espera el mensaje `BUILD SUCCESS` en la ventana de salida (puede tardar unos minutos).
5. El archivo queda en la carpeta `target` del proyecto: `proyecto-1.0-SNAPSHOT-all.jar`
   (ojo: usa el que termina en `-all.jar`; el otro, sin `-all`, no incluye las librerías y no abre solo).

## Opción 2: desde la terminal (si tienes Maven instalado)
En la carpeta del proyecto (donde está el pom.xml):

    mvn clean package -Pjar-ejecutable

## Cómo ejecutarlo
    java -jar proyecto-1.0-SNAPSHOT-all.jar

Hacer doble clic en el .jar también puede funcionar, pero si en la computadora el Java predeterminado es más viejo que el 24
no pasa nada y no se ve ningún error. Para ver el mensaje, crea al lado del .jar un archivo `ejecutar.bat` con esto:

    @echo off
    cd /d "%~dp0"
    java -jar proyecto-1.0-SNAPSHOT-all.jar
    pause

y ábrelo con doble clic.

## Para el envío de correos
El archivo `correo.properties` **no** va dentro del .jar. El programa lo busca en la carpeta desde donde se ejecuta, junto al
.jar y en las dos carpetas de arriba; así que sirve dejarlo **junto al pom.xml** (sigue funcionando con el .jar que está en
`target`) o **junto al .jar** si lo copias a otra computadora. Si no lo encuentra, el mensaje lista las carpetas donde buscó.
Si falta, la venta funciona igual y al cobrar se avisa que no se enviará el comprobante.

## Si algo falla
| Síntoma | Causa probable |
| --- | --- |
| `BUILD FAILURE` por la versión de Java | El JDK con que corre Maven es anterior a 17. En NetBeans: Herramientas > Opciones > Java > Maven, o el JDK del proyecto. |
| El .jar no abre / "Unsupported class file major version" | Se ejecuta con un Java anterior al 24. Revisa con `java -version`. |
| "JavaFX runtime components are missing" | Se está usando el .jar sin `-all` (el que no trae las librerías). |
| Abre en tu computadora pero no en otra con otro sistema operativo | Hay que generarlo de nuevo en ese sistema operativo. |
