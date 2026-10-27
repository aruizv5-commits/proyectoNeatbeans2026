package com.umg.api.service;

import jakarta.activation.CommandMap;
import jakarta.activation.DataHandler;
import jakarta.activation.MailcapCommandMap;
import jakarta.mail.Authenticator;
import jakarta.mail.internet.AddressException;
import jakarta.mail.AuthenticationFailedException;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.SendFailedException;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import jakarta.mail.util.ByteArrayDataSource;

import java.io.IOException;
import java.io.StringReader;
import java.net.ConnectException;
import java.net.NoRouteToHostException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * Envía correos con Gmail directamente desde el programa (SMTP), sin pasar por la API.
 *
 * Gmail no acepta la contraseña normal de la cuenta: hay que crear una "contraseña de aplicación"
 * (ver docs/CORREO_GMAIL.md). Los datos se leen del archivo correo.properties (junto al pom.xml o junto al
 * .jar; NO se sube a GitHub) o de las variables de entorno GMAIL_USUARIO y GMAIL_CLAVE:
 *
 *     usuario=micuenta@gmail.com
 *     clave=abcdefghijklmnop
 */
public class CorreoService {

    private static final String ARCHIVO = "correo.properties";

    private final String usuario;
    private final String clave;
    private final String host;
    private final int puerto;

    /** Lee la configuración (correo.properties o variables de entorno). */
    public CorreoService() throws Exception {
        Properties p = leerConfiguracion();
        this.usuario = valor(p, "usuario", "GMAIL_USUARIO");
        this.clave = quitarEspacios(valor(p, "clave", "GMAIL_CLAVE"));
        this.host = p.getProperty("host", "smtp.gmail.com").trim();
        this.puerto = Integer.parseInt(p.getProperty("puerto", "587").trim());
        if (usuario == null || clave.isEmpty()) {
            Path archivo = buscarArchivo();
            if (archivo == null) {
                StringBuilder carpetas = new StringBuilder();
                for (Path c : carpetasDeBusqueda()) carpetas.append("\n  ").append(c);
                throw new Exception("No se encontró el archivo " + ARCHIVO + ". Créalo en cualquiera de estas carpetas "
                        + "(la mejor es la del programa: junto al pom.xml o junto al .jar):" + carpetas + "\n\n"
                        + "Debe tener las líneas usuario=... y clave=... (contraseña de aplicación de Gmail).\n"
                        + "Los pasos están en docs/CORREO_GMAIL.md");
            }
            throw new Exception("El archivo " + archivo.toAbsolutePath() + " existe, pero le falta la línea "
                    + "usuario=... o la línea clave=... (una por línea, escritas exactamente así).");
        }
    }

    /** Para pruebas o para usar otro servidor SMTP con los mismos datos. */
    public CorreoService(String usuario, String clave, String host, int puerto) {
        this.usuario = usuario;
        this.clave = quitarEspacios(clave);
        this.host = host;
        this.puerto = puerto;
    }

    /** true si ya existe la configuración del correo (para no intentar enviar si no se ha configurado). */
    public static boolean configurado() {
        try {
            Properties p = leerConfiguracion();
            return valor(p, "usuario", "GMAIL_USUARIO") != null && !quitarEspacios(valor(p, "clave", "GMAIL_CLAVE")).isEmpty();
        } catch (Exception e) {
            return false;
        }
    }

    // ------------------------------------------------------------------ envío

    /**
     * Envía un correo de texto con un archivo adjunto (puede ser null).
     *
     * @param para           correo del destinatario
     * @param adjunto        archivo a adjuntar (por ejemplo, el PDF del comprobante)
     * @param nombreAdjunto  nombre con el que le llega al cliente, por ejemplo "comprobante-venta-5.pdf"
     */
    public void enviar(String para, String asunto, String texto, Path adjunto, String nombreAdjunto) throws Exception {
        Thread.currentThread().setContextClassLoader(CorreoService.class.getClassLoader());
        registrarManejadores();

        Properties props = new Properties();
        props.put("mail.smtp.host", host);
        props.put("mail.smtp.port", String.valueOf(puerto));
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");      // Gmail en el puerto 587 usa STARTTLS
        props.put("mail.smtp.starttls.required", "true");    // nunca mandar la clave sin cifrar
        props.put("mail.smtp.ssl.trust", host);
        props.put("mail.smtp.connectiontimeout", "20000");   // si no hay internet, que falle en 20 s y no se quede colgado
        props.put("mail.smtp.timeout", "30000");
        props.put("mail.smtp.writetimeout", "30000");

        Session sesion = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(usuario, clave);
            }
        });

        try {
            MimeMessage mensaje = new MimeMessage(sesion);
            mensaje.setFrom(new InternetAddress(usuario, "Boletería SVB-GUA", "UTF-8"));
            mensaje.setRecipient(Message.RecipientType.TO, new InternetAddress(para, true));
            mensaje.setSubject(asunto, "UTF-8");

            MimeMultipart partes = new MimeMultipart();

            MimeBodyPart cuerpo = new MimeBodyPart();
            cuerpo.setText(texto, "UTF-8");
            partes.addBodyPart(cuerpo);

            if (adjunto != null) {
                MimeBodyPart archivo = new MimeBodyPart();
                archivo.setDataHandler(new DataHandler(new ByteArrayDataSource(Files.readAllBytes(adjunto), "application/pdf")));
                archivo.setFileName(nombreAdjunto);
                partes.addBodyPart(archivo);
            }

            mensaje.setContent(partes);
            Transport.send(mensaje);

        } catch (AuthenticationFailedException e) {
            throw new Exception("Gmail rechazó el usuario o la contraseña de aplicación. Revisa " + ARCHIVO
                    + " (debe ser una contraseña de aplicación, no la contraseña normal de la cuenta).");
        } catch (AddressException e) {
            throw new Exception("El correo del destinatario no es válido: " + para);
        } catch (SendFailedException e) {
            throw new Exception("No se pudo enviar el correo a " + para + ": " + e.getMessage());
        } catch (MessagingException e) {
            e.printStackTrace();   // el detalle técnico completo queda en la ventana negra si se ejecuta con java -jar
            Throwable causa = e;
            while (causa.getCause() != null) causa = causa.getCause();
            if (causa instanceof UnknownHostException || causa instanceof ConnectException
                    || causa instanceof SocketTimeoutException || causa instanceof NoRouteToHostException) {
                throw new Exception("No se pudo conectar con Gmail. Revisa tu conexión a internet y que la red permita el puerto "
                        + puerto + " (algunas redes de universidades lo bloquean; prueba con otra red).");
            }
            throw new Exception("No se pudo enviar el correo.\nDetalle técnico: "
                    + causa.getClass().getSimpleName() + ": " + causa.getMessage());
        } catch (IOException e) {
            throw new Exception("No se pudo leer el archivo a adjuntar: " + e.getMessage());
        }
    }

    // ------------------------------------------------------------------ configuración

    // Carpetas donde se busca correo.properties: desde donde se ejecuta el programa, junto al .jar (o a las clases)
    // y las dos carpetas de arriba (si el .jar está en target/, así se encuentra el archivo junto al pom.xml)
    private static List<Path> carpetasDeBusqueda() {
        List<Path> carpetas = new ArrayList<>();
        carpetas.add(Path.of("").toAbsolutePath());
        try {
            Path programa = Path.of(CorreoService.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            Path carpeta = Files.isDirectory(programa) ? programa : programa.getParent();
            for (int i = 0; i < 3 && carpeta != null; i++, carpeta = carpeta.getParent()) {
                if (!carpetas.contains(carpeta)) carpetas.add(carpeta);
            }
        } catch (Exception e) {
            // si no se puede saber dónde está el programa, solo se usa la carpeta actual
        }
        return carpetas;
    }

    // En Windows el Bloc de notas suele guardar "correo.properties.txt" sin que se vea el .txt: también se acepta
    private static Path buscarArchivo() {
        for (Path carpeta : carpetasDeBusqueda()) {
            for (String nombre : new String[]{ARCHIVO, ARCHIVO + ".txt"}) {
                Path archivo = carpeta.resolve(nombre);
                if (Files.exists(archivo)) return archivo;
            }
        }
        return null;
    }

    private static Properties leerConfiguracion() throws IOException {
        Properties p = new Properties();
        Path archivo = buscarArchivo();
        if (archivo != null) {
            String texto = Files.readString(archivo, StandardCharsets.UTF_8);
            if (texto.startsWith("\uFEFF")) texto = texto.substring(1);   // marca invisible que agrega el Bloc de notas
            p.load(new StringReader(texto));
        }
        return p;
    }

    // el valor del archivo; si no está, el de la variable de entorno
    private static String valor(Properties p, String clave, String variableDeEntorno) {
        String v = p.getProperty(clave);
        if (v == null || v.isBlank()) v = System.getenv(variableDeEntorno);
        if (v != null) v = v.replace("\"", "").trim();     // por si escribieron la clave entre comillas
        return v == null || v.isBlank() ? null : v;
    }

    // Google muestra la contraseña de aplicación en grupos de 4 letras separados por espacios
    private static String quitarEspacios(String s) {
        return s == null ? "" : s.replace(" ", "");
    }

    // Dentro de un .jar único (todas las librerías juntas) puede perderse la lista de "manejadores" de Jakarta Activation
    // y el envío falla con "no object DCH for MIME type multipart/mixed". Se registran a mano (solo si existen).
    private static void registrarManejadores() {
        try {
            Class.forName("org.eclipse.angus.mail.handlers.multipart_mixed");
            String base = "x-java-content-handler=org.eclipse.angus.mail.handlers.";
            MailcapCommandMap mapa = (MailcapCommandMap) CommandMap.getDefaultCommandMap();
            mapa.addMailcap("text/plain;; " + base + "text_plain");
            mapa.addMailcap("text/html;; " + base + "text_html");
            mapa.addMailcap("text/xml;; " + base + "text_xml");
            mapa.addMailcap("message/rfc822;; " + base + "message_rfc822");
            mapa.addMailcap("multipart/*;; " + base + "multipart_mixed; x-java-fallback-entry=true");
            CommandMap.setDefaultCommandMap(mapa);
        } catch (Exception | LinkageError e) {
            // si no se puede, se sigue con la configuración normal
        }
    }
}
