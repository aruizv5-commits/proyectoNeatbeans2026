package com.umg.api.fx.view;

import com.umg.api.fx.util.Async;
import com.umg.api.fx.util.Ui;
import com.umg.api.model.artista;
import com.umg.api.service.artistaservice;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/** CRUD de artistas. Es la plantilla del resto de pantallas de administración. */
public class ArtistasView extends HBox implements Recargable {
    private final TableView<artista> tabla = new TableView<>();
    private final TextField txtNombre = new TextField();
    private final TextField txtGenero = new TextField();
    private final TextField txtPais = new TextField();
    private artista seleccionado;

    public ArtistasView() {
        setSpacing(16);
        setPadding(new Insets(14));

        Ui.columnas(tabla,
                Ui.col("Artista", artista::getNombre_artistico, 220),
                Ui.col("Género", artista::getGenero_musical, 140),
                Ui.col("País", artista::getPais_origen, 120));
        tabla.getSelectionModel().selectedItemProperty().addListener((o, a, s) -> {
            seleccionado = s;
            if (s != null) {
                txtNombre.setText(s.getNombre_artistico());
                txtGenero.setText(s.getGenero_musical());
                txtPais.setText(s.getPais_origen() == null ? "" : s.getPais_origen());
            }
        });
        VBox izq = new VBox(tabla);
        VBox.setVgrow(tabla, Priority.ALWAYS);
        izq.getStyleClass().add("panel");
        HBox.setHgrow(izq, Priority.ALWAYS);

        Label titulo = new Label("Artista");
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
                Ui.form("Nombre artístico", txtNombre, "Género musical", txtGenero, "País de origen", txtPais),
                new HBox(8, nuevo, guardar, eliminar));
        der.getStyleClass().add("panel");
        der.setPrefWidth(340);
        der.setMinWidth(340);

        getChildren().addAll(izq, der);
    }

    @Override
    public void recargar() {
        Async.run(() -> new artistaservice().getArtista(), tabla.getItems()::setAll);
    }

    private void limpiar() {
        tabla.getSelectionModel().clearSelection();
        seleccionado = null;
        txtNombre.clear();
        txtGenero.clear();
        txtPais.clear();
    }

    private void guardar() {
        String n = txtNombre.getText().trim(), g = txtGenero.getText().trim(), p = txtPais.getText().trim();
        if (n.isEmpty() || g.isEmpty()) {
            Ui.error("El nombre artístico y el género son obligatorios.");
            return;
        }
        artista sel = seleccionado;
        artista a = new artista(sel == null ? 0 : sel.getId_artista(), n, g, p.isEmpty() ? null : p);
        Async.run(() -> sel == null
                ? new artistaservice().createArtista(a)
                : new artistaservice().updateArtista(sel.getId_artista(), a),
            r -> { limpiar(); recargar(); });
    }

    private void eliminar() {
        artista sel = seleccionado;
        if (sel == null) { Ui.error("Selecciona un artista."); return; }
        if (!Ui.confirmar("¿Eliminar a \"" + sel.getNombre_artistico() + "\"?")) return;
        Async.run(() -> { new artistaservice().deleteartista(sel.getId_artista()); return true; },
                ok -> { limpiar(); recargar(); });
    }
}
