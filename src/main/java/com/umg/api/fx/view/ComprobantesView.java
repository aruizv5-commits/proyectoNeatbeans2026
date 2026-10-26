package com.umg.api.fx.view;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
public class ComprobantesView extends VBox {
    public ComprobantesView() {
        setSpacing(15);setPadding(new Insets(20));
        Label titulo=new Label("Comprobantes y boletos");
        titulo.setStyle("-fx-font-size:20px;-fx-font-weight:bold;");
        Label ayuda=new Label("Guarda el comprobante de una venta existente y sus boletos con QR, sin volver a cobrar.");
        ayuda.setWrapText(true);
        Button guardar=new Button("Guardar comprobante y boletos PDF");
        guardar.getStyleClass().add("boton-primario");
        guardar.setOnAction(e->com.umg.api.reportes.BoletosPdf.guardar(getScene().getWindow()));
        getChildren().addAll(titulo,ayuda,guardar);
    }
}
