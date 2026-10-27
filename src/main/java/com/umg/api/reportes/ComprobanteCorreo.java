package com.umg.api.reportes;

import com.umg.api.service.ComprobanteService;
import com.umg.api.service.CorreoService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Genera el PDF del comprobante (con los boletos y su QR) y lo envía por Gmail al cliente.
 * Es una llamada bloqueante: ejecutarla con Async.run para no congelar la ventana.
 */
public final class ComprobanteCorreo {
    private ComprobanteCorreo() {}

    public static void enviar(ComprobanteService.Documento venta, String destinatario) throws Exception {
        CorreoService correo = new CorreoService();          // si falta la configuración, avisa antes de generar nada

        Path pdf = Files.createTempFile("comprobante-venta-" + venta.numero() + "-", ".pdf");
        try {
            BoletosPdf.exportar(venta, pdf);
            correo.enviar(destinatario,
                    "Comprobante de tu compra · Venta #" + venta.numero(),
                    cuerpo(venta),
                    pdf,
                    "boletos-venta-" + venta.numero() + ".pdf");
        } finally {
            Files.deleteIfExists(pdf);                       // el PDF temporal no se queda en la computadora
        }
    }

    private static String cuerpo(ComprobanteService.Documento venta) {
        StringBuilder t = new StringBuilder();
        t.append("Hola ").append(venta.cliente()).append(",\n\n");
        t.append("Gracias por tu compra. Adjuntamos el comprobante de la venta #").append(venta.numero())
                .append(" y tus boletos con código QR.\n\n");
        for (var l : venta.lineas()) {
            t.append("  - ").append(l.get("evento")).append(" · ").append(l.get("localidad"))
                    .append(" · ").append(l.get("fecha_evento")).append("\n");
        }
        t.append("\nTotal pagado: Q").append(venta.total().setScale(2, RoundingMode.HALF_UP).toPlainString()).append("\n\n");
        t.append("Presenta el código QR de cada boleto en la entrada.\n");
        t.append("Este es un documento académico, no una factura fiscal.\n\n");
        t.append("Boletería SVB-GUA");
        return t.toString();
    }
}
