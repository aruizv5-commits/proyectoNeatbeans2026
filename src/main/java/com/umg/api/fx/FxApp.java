package com.umg.api.fx;

import com.umg.api.fx.view.LoginView;
import com.umg.api.fx.view.MainView;
import com.umg.api.model.usuario;
import javafx.application.Application;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.Screen;

/** Aplicación JavaFX: cambia entre la pantalla de login y la ventana principal. */
public class FxApp extends Application {
    private static Stage stage;

    @Override
    public void start(Stage primaryStage) {
        stage = primaryStage;
        stage.setTitle("Boletería — Venta en puntos físicos");
        mostrarLogin(null);
        stage.show();
    }

    public static void mostrarLogin(String aviso) {
        stage.setMinWidth(0);
        stage.setMinHeight(0);
        stage.setMaximized(false);
        cambiarEscena(new LoginView(FxApp::mostrarPrincipal, aviso), 500, 550);
        stage.centerOnScreen();
    }

    public static void mostrarPrincipal(usuario u) {
        var pantalla = Screen.getPrimary().getVisualBounds();
        double ancho = Math.min(1440, pantalla.getWidth() - 40);
        double alto = Math.min(850, pantalla.getHeight() - 40);
        cambiarEscena(new MainView(u), ancho, alto);
        stage.setMinWidth(Math.min(1024, ancho));
        stage.setMinHeight(Math.min(620, alto));
        stage.centerOnScreen();
    }

    private static void cambiarEscena(Parent raiz, double ancho, double alto) {
        Scene escena = new Scene(raiz, ancho, alto);
        var css = FxApp.class.getResource("/fx/style.css");
        if (css != null) escena.getStylesheets().add(css.toExternalForm());
        stage.setScene(escena);
        stage.sizeToScene();
    }
}
