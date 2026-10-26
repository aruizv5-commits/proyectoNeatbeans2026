package com.umg.api.fx.view;

import com.umg.api.config.Sesion;
import com.umg.api.fx.util.Async;
import com.umg.api.fx.util.Ui;
import com.umg.api.model.usuario;
import com.umg.api.service.usuarioservice;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public class UsuariosView extends HBox implements Recargable {
    private final TableView<usuario> tabla = new TableView<>();
    private final TextField txtUsuario = new TextField();
    private final TextField txtNombre = new TextField();
    private final PasswordField txtClave = new PasswordField();
    private final ComboBox<String> cbRol = new ComboBox<>();

    public UsuariosView() {
        setSpacing(16);
        setPadding(new Insets(14));

        Ui.columnas(tabla,
                Ui.col("Usuario", usuario::getNombre_usuario, 140),
                Ui.col("Nombre completo", usuario::getNombre_completo, 220),
                Ui.col("Rol", usuario::getRol, 110),
                Ui.col("Estado", u -> u.isStatus() ? "Activo" : "Inactivo", 80));
        VBox izq = new VBox(tabla);
        VBox.setVgrow(tabla, Priority.ALWAYS);
        izq.getStyleClass().add("panel");
        HBox.setHgrow(izq, Priority.ALWAYS);

        cbRol.getItems().setAll("vendedor", "administrador");
        cbRol.setValue("vendedor");

        Label titulo = new Label("Nuevo usuario");
        titulo.getStyleClass().add("seccion");
        Button crear = new Button("Crear usuario");
        crear.getStyleClass().add("boton-primario");
        crear.setOnAction(e -> crear());
        Button eliminar = new Button("Eliminar seleccionado");
        eliminar.getStyleClass().add("boton-peligro");
        eliminar.setOnAction(e -> eliminar());
        VBox der = new VBox(12, titulo,
                Ui.form("Usuario", txtUsuario, "Nombre completo", txtNombre, "Contraseña", txtClave, "Rol", cbRol),
                new HBox(8, crear, eliminar));
        der.getStyleClass().add("panel");
        der.setPrefWidth(360);
        der.setMinWidth(360);

        getChildren().addAll(izq, der);
    }

    @Override
    public void recargar() {
        Async.run(() -> new usuarioservice().getUsuario(), tabla.getItems()::setAll);
    }

    private void crear() {
        String u = txtUsuario.getText().trim(), n = txtNombre.getText().trim(), p = txtClave.getText();
        if (u.isEmpty() || n.isEmpty() || p.length() < 6) {
            Ui.error("Completa usuario y nombre. La contraseña debe tener al menos 6 caracteres.");
            return;
        }
        // El modelo solo tiene el campo contrasena_hash. La API lo guarda tal como llega
        // y el login compara ese mismo valor con lo que escribe el usuario.
        usuario nuevo = new usuario(0, u, p, n, cbRol.getValue(), null, true);
        Async.run(() -> new usuarioservice().createUsuario(nuevo), r -> {
            txtUsuario.clear();
            txtNombre.clear();
            txtClave.clear();
            cbRol.setValue("vendedor");
            recargar();
        });
    }

    private void eliminar() {
        usuario sel = tabla.getSelectionModel().getSelectedItem();
        if (sel == null) { Ui.error("Selecciona un usuario."); return; }
        if (sel.getId_usuario() == Sesion.actual().getId_usuario()) {
            Ui.error("No puedes eliminar tu propio usuario.");
            return;
        }
        if (!Ui.confirmar("¿Eliminar al usuario \"" + sel.getNombre_usuario() + "\"?")) return;
        Async.run(() -> { new usuarioservice().deleteUsuario(sel.getId_usuario()); return true; }, ok -> recargar());
    }
}
