package com.umg.api.fx.view;

import com.umg.api.fx.util.Async;
import com.umg.api.fx.util.Ui;
import com.umg.api.model.cliente;
import com.umg.api.service.clienteservice;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/** CRUD de clientes (NIT, contacto). Lo usan vendedores y administradores. */
public class ClientesView extends HBox implements Recargable {
    private final TableView<cliente> tabla = new TableView<>();
    private final TextField txtNombre = new TextField();
    private final TextField txtApellido = new TextField();
    private final TextField txtNit = new TextField();
    private final TextField txtTelefono = new TextField();
    private final TextField txtCorreo = new TextField();
    private final TextField txtDireccion = new TextField();
    private final CheckBox chkActivo = new CheckBox("Activo");
    private cliente seleccionado;

    public ClientesView() {
        setSpacing(16);
        setPadding(new Insets(14));

        Ui.columnas(tabla,
                Ui.col("Nombre", cliente::getNombre, 120),
                Ui.col("Apellido", cliente::getApellido, 120),
                Ui.col("NIT", cliente::getNit, 90),
                Ui.col("Teléfono", cliente::getTelefono, 100),
                Ui.col("Correo", cliente::getCorreo, 170),
                Ui.col("Estado", c -> c.isStatus() ? "Activo" : "Inactivo", 70));
        tabla.getSelectionModel().selectedItemProperty().addListener((o, a, s) -> {
            seleccionado = s;
            if (s != null) cargarForm(s);
        });
        VBox izq = new VBox(tabla);
        VBox.setVgrow(tabla, Priority.ALWAYS);
        izq.getStyleClass().add("panel");
        HBox.setHgrow(izq, Priority.ALWAYS);

        chkActivo.setSelected(true);
        Label titulo = new Label("Cliente");
        titulo.getStyleClass().add("seccion");
        Button nuevo = new Button("Nuevo");
        nuevo.setOnAction(e -> limpiar());
        Button guardar = new Button("Guardar");
        guardar.getStyleClass().add("boton-primario");
        guardar.setOnAction(e -> guardar());
        Button eliminar = new Button("Eliminar");
        eliminar.getStyleClass().add("boton-peligro");
        eliminar.setOnAction(e -> eliminar());
        VBox der = new VBox(12, titulo,
                Ui.form("Nombre", txtNombre, "Apellido", txtApellido, "NIT", txtNit, "Teléfono", txtTelefono,
                        "Correo", txtCorreo, "Dirección", txtDireccion, "", chkActivo),
                new HBox(8, nuevo, guardar, eliminar));
        der.getStyleClass().add("panel");
        der.setPrefWidth(360);
        der.setMinWidth(360);

        getChildren().addAll(izq, der);
    }

    @Override
    public void recargar() {
        Async.run(() -> new clienteservice().getCliente(), tabla.getItems()::setAll);
    }

    private static String vacio(String s) { return s == null ? "" : s; }

    private void cargarForm(cliente c) {
        txtNombre.setText(vacio(c.getNombre()));
        txtApellido.setText(vacio(c.getApellido()));
        txtNit.setText(vacio(c.getNit()));
        txtTelefono.setText(vacio(c.getTelefono()));
        txtCorreo.setText(vacio(c.getCorreo()));
        txtDireccion.setText(vacio(c.getDireccion()));
        chkActivo.setSelected(c.isStatus());
    }

    private void limpiar() {
        tabla.getSelectionModel().clearSelection();
        seleccionado = null;
        for (TextField t : new TextField[]{txtNombre, txtApellido, txtNit, txtTelefono, txtCorreo, txtDireccion}) t.clear();
        chkActivo.setSelected(true);
    }

    private void guardar() {
        String nombre = txtNombre.getText().trim();
        if (nombre.isEmpty()) {
            Ui.error("El nombre es obligatorio.");
            return;
        }
        cliente sel = seleccionado;
        cliente c = new cliente(sel == null ? 0 : sel.getId(), nombre, txtApellido.getText().trim(),
                txtDireccion.getText().trim(), txtCorreo.getText().trim(), txtTelefono.getText().trim(),
                txtNit.getText().trim(), chkActivo.isSelected());
        Async.run(() -> sel == null
                ? new clienteservice().createCliente(c)
                : new clienteservice().updateCliente(sel.getId(), c),
            r -> { limpiar(); recargar(); });
    }

    private void eliminar() {
        cliente sel = seleccionado;
        if (sel == null) { Ui.error("Selecciona un cliente."); return; }
        if (!Ui.confirmar("¿Eliminar a \"" + sel.getNombre() + "\"?")) return;
        Async.run(() -> { new clienteservice().deleteCliente(sel.getId()); return true; },
                ok -> { limpiar(); recargar(); });
    }
}
