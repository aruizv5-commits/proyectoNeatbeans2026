package com.umg.api.fx.view;

import com.umg.api.config.Sesion;
import com.umg.api.fx.util.Async;
import com.umg.api.fx.util.Ui;
import com.umg.api.reportes.ComprobanteCorreo;
import com.umg.api.service.ComprobanteService;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.VBox;

public class ComprobantesView extends VBox {
    private final Button enviar = new Button("Enviar comprobante por correo (Gmail)");

    public ComprobantesView() {
        setSpacing(15);setPadding(new Insets(20));
        Label titulo=new Label("Comprobantes y boletos");
        titulo.setStyle("-fx-font-size:20px;-fx-font-weight:bold;");
        Label ayuda=new Label("Guarda el comprobante de una venta existente y sus boletos con QR, sin volver a cobrar.");
        ayuda.setWrapText(true);
        Button guardar=new Button("Guardar comprobante y boletos PDF");
        guardar.getStyleClass().add("boton-primario");
        guardar.setOnAction(e->com.umg.api.reportes.BoletosPdf.guardar(getScene().getWindow()));

        Label ayudaCorreo=new Label("O envía el comprobante y los boletos al correo del cliente desde tu cuenta de Gmail.");
        ayudaCorreo.setWrapText(true);
        enviar.getStyleClass().add("boton-primario");
        enviar.setOnAction(e->enviarPorCorreo());

        getChildren().addAll(titulo,ayuda,guardar,ayudaCorreo,enviar);
    }

    // 1) pide el número de venta  2) la carga  3) pide/confirma el correo  4) genera el PDF y lo envía
    private void enviarPorCorreo() {
        if (!Sesion.activa()) { Ui.error("Inicia sesión primero."); return; }
        TextInputDialog pregunta = new TextInputDialog();
        pregunta.initOwner(getScene().getWindow());
        pregunta.setTitle("Enviar comprobante");
        pregunta.setHeaderText("Número de una venta ya registrada");
        var entrada = pregunta.showAndWait();
        if (entrada.isEmpty()) return;
        final int id;
        try {
            id = Integer.parseInt(entrada.get().trim());
            if (id < 1) throw new NumberFormatException();
        } catch (NumberFormatException ex) { Ui.error("Número de venta inválido."); return; }

        Integer vendedor = Sesion.esAdmin() ? null : Sesion.actual().getId_usuario();
        Async.run(() -> new ComprobanteService().cargar(id, vendedor), this::pedirCorreo);
    }

    private void pedirCorreo(ComprobanteService.Documento venta) {
        // el correo del cliente ya viene escrito; se puede corregir antes de enviar
        TextInputDialog pregunta = new TextInputDialog(venta.correo());
        pregunta.initOwner(getScene().getWindow());
        pregunta.setTitle("Enviar comprobante");
        pregunta.setHeaderText("Venta #" + venta.numero() + " · " + venta.cliente());
        pregunta.setContentText("Enviar a este correo:");
        var entrada = pregunta.showAndWait();
        if (entrada.isEmpty()) return;
        String correo = entrada.get().trim();
        if (!correo.contains("@") || correo.contains(" ")) { Ui.error("Escribe un correo válido."); return; }

        enviar.setDisable(true);
        enviar.setText("Enviando…");
        Async.run(() -> { ComprobanteCorreo.enviar(venta, correo); return correo; },
                c -> {
                    restaurarBoton();
                    Ui.info("Comprobante enviado a " + c + ".");
                },
                msg -> {
                    restaurarBoton();
                    Ui.error(msg);
                });
    }

    private void restaurarBoton() {
        enviar.setDisable(false);
        enviar.setText("Enviar comprobante por correo (Gmail)");
    }
}
