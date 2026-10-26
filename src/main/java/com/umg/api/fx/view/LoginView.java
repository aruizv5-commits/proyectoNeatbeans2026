package com.umg.api.fx.view;

import com.umg.api.config.Sesion;
import com.umg.api.fx.util.Async;
import com.umg.api.model.usuario;
import com.umg.api.service.authservice;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

import java.util.function.Consumer;

public class LoginView extends VBox {

    public LoginView(Consumer<usuario> alIngresar, String aviso) {
        setAlignment(Pos.CENTER);
        setSpacing(14);
        setPadding(new Insets(40));
        getStyleClass().add("login");

        Label titulo = new Label("Boletería");
        titulo.getStyleClass().add("titulo-login");
        Label sub = new Label("Inicia sesión para acceder a tu punto de venta");
        sub.getStyleClass().add("subtitulo");

        TextField txtUsuario = new TextField();
        txtUsuario.setPromptText("Usuario");
        PasswordField txtClave = new PasswordField();
        txtClave.setPromptText("Contraseña");

        Label lblError = new Label(aviso == null ? "" : aviso);
        lblError.getStyleClass().add("error");
        lblError.setWrapText(true);

        Button btn = new Button("Ingresar");
        btn.getStyleClass().add("boton-primario");
        btn.setDefaultButton(true);          // Enter dispara el login
        btn.setMaxWidth(Double.MAX_VALUE);

        btn.setOnAction(e -> {
            String u = txtUsuario.getText().trim();
            String p = txtClave.getText();
            if (u.isEmpty() || p.isEmpty()) {
                lblError.setText("Escribe tu usuario y contraseña.");
                return;
            }
            btn.setDisable(true);
            btn.setText("Ingresando…");
            lblError.setText("");
            Async.run(
                () -> new authservice().login(u, p),
                r -> {
                    Sesion.iniciar(r.usuario, r.token);
                    alIngresar.accept(r.usuario);
                },
                msg -> {
                    lblError.setText(msg);
                    btn.setDisable(false);
                    btn.setText("Ingresar");
                    txtClave.clear();
                    txtClave.requestFocus();
                });
        });

        txtUsuario.setAccessibleText("Usuario");
        txtClave.setAccessibleText("Contraseña");
        getChildren().addAll(titulo, sub, new Label("Usuario"), txtUsuario,
                new Label("Contraseña"), txtClave, btn, lblError);
    }
}
