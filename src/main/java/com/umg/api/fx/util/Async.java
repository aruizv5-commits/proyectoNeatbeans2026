package com.umg.api.fx.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.umg.api.config.Sesion;
import com.umg.api.fx.FxApp;
import javafx.concurrent.Task;

import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.function.Consumer;

/**
 * Ejecuta las llamadas a los servicios (bloqueantes, lanzan Exception) fuera del hilo de JavaFX
 * y entrega el resultado en el hilo de la interfaz, para que la ventana nunca se congele.
 */
public final class Async {
    private Async() {}

    public static <T> void run(Callable<T> trabajo, Consumer<T> alExito) {
        run(trabajo, alExito, Ui::error);
    }

    public static <T> void run(Callable<T> trabajo, Consumer<T> alExito, Consumer<String> alFallar) {
        Task<T> tarea = new Task<>() {
            @Override protected T call() throws Exception { return trabajo.call(); }
        };
        tarea.setOnSucceeded(e -> alExito.accept(tarea.getValue()));
        tarea.setOnFailed(e -> {
            Throwable ex = tarea.getException();
            String msg = ex.getMessage() == null ? ex.toString() : ex.getMessage();

            // Los servicios reportan "Código HTTP: 401" cuando el token venció
            if (Sesion.activa() && msg.contains("Código HTTP: 401")) {
                Sesion.cerrar();
                FxApp.mostrarLogin("Tu sesión expiró. Inicia sesión de nuevo.");
                return;
            }
            if (ex instanceof JsonProcessingException) {
                msg = "El servidor respondió algo inesperado: " + msg;
            } else if (ex instanceof IOException) {
                msg = "No se pudo conectar con el servidor. Revisa tu conexión a internet.";
            }
            alFallar.accept(msg);
        });
        Thread hilo = new Thread(tarea, "api-worker");
        hilo.setDaemon(true);
        hilo.start();
    }
}
