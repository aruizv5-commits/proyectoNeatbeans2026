package com.umg.api.fx.view;

import com.umg.api.fx.util.Async;
import com.umg.api.fx.util.Ui;
import com.umg.api.model.concierto;
import com.umg.api.model.inventarios_boletos;
import com.umg.api.model.localidad;
import com.umg.api.service.conciertoservice;
import com.umg.api.service.inventarios_boletosservice;
import com.umg.api.service.localidadservice;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Asientos e inventario de cada concierto. La API genera los 100 asientos de un concierto de una vez. */
public class InventarioView extends HBox implements Recargable {
    private final Map<Integer, localidad> localidades = new HashMap<>();
    private List<inventarios_boletos> todoInventario = List.of();

    private final ComboBox<concierto> cbConcierto = new ComboBox<>();
    private final TableView<inventarios_boletos> tabla = new TableView<>();
    private final TextField txtPrecio = new TextField();

    public InventarioView() {
        setSpacing(16);
        setPadding(new Insets(14));

        cbConcierto.setPromptText("Selecciona un concierto");
        cbConcierto.setConverter(Ui.convertidor(concierto::getTitulo_evento));
        cbConcierto.setMaxWidth(Double.MAX_VALUE);
        cbConcierto.valueProperty().addListener((o, a, c) -> mostrarInventario());

        Ui.columnas(tabla,
                Ui.col("Localidad", i -> nombreDe(i.getId_localidad()), 120),
                Ui.col("Asiento", i -> asientoDe(i.getId_localidad()), 90),
                Ui.col("Precio", i -> Ui.dinero(i.getPrecio()), 90),
                Ui.col("Disponible", i -> String.valueOf(i.getCantidad_disponible()), 90));

        VBox izq = new VBox(10, new Label("Concierto"), cbConcierto, tabla);
        VBox.setVgrow(tabla, Priority.ALWAYS);
        izq.getStyleClass().add("panel");
        HBox.setHgrow(izq, Priority.ALWAYS);

        Label titulo = new Label("Generar asientos");
        titulo.getStyleClass().add("seccion");
        Label nota = new Label("Se crean 100 asientos (10 filas de 10) con el mismo precio.");
        nota.setWrapText(true);
        Button generar = new Button("Generar asientos");
        generar.getStyleClass().add("boton-primario");
        generar.setOnAction(e -> generarAsientos());

        VBox der = new VBox(12, titulo, nota,
                Ui.form("Precio (" + Ui.MONEDA + ")", txtPrecio),
                generar);
        der.getStyleClass().add("panel");
        der.setPrefWidth(320);
        der.setMinWidth(320);

        getChildren().addAll(izq, der);
    }

    private record Datos(List<concierto> conciertos, List<localidad> localidades, List<inventarios_boletos> inventarios) {}

    @Override
    public void recargar() {
        int idPrevio = cbConcierto.getValue() == null ? -1 : cbConcierto.getValue().getId_concierto();
        Async.run(() -> new Datos(new conciertoservice().getConcierto(),
                        new localidadservice().getLocalidad(),
                        new inventarios_boletosservice().getInventarios_boletos()), d -> {
            localidades.clear();
            for (localidad l : d.localidades()) {
                localidades.put(l.getId_localidad(), l);
            }
            todoInventario = d.inventarios();
            cbConcierto.getItems().setAll(d.conciertos());
            for (concierto c : d.conciertos()) {
                if (c.getId_concierto() == idPrevio) cbConcierto.setValue(c);
            }
            mostrarInventario();
        });
    }

    private String nombreDe(int idLocalidad) {
        localidad l = localidades.get(idLocalidad);
        return l == null ? "" : l.getNombre();
    }

    private String asientoDe(int idLocalidad) {
        localidad l = localidades.get(idLocalidad);
        return l == null ? "" : l.getAsiento();
    }

    private void mostrarInventario() {
        concierto c = cbConcierto.getValue();
        tabla.getItems().setAll(c == null ? List.of()
                : todoInventario.stream().filter(i -> i.getId_concierto() == c.getId_concierto()).toList());
        tabla.refresh();
    }

    private void generarAsientos() {
        concierto c = cbConcierto.getValue();
        if (c == null) {
            Ui.error("Selecciona un concierto.");
            return;
        }
        if (!tabla.getItems().isEmpty()) {
            Ui.error("Este concierto ya tiene asientos.");
            return;
        }
        float precio;
        try {
            precio = Float.parseFloat(txtPrecio.getText().trim().replace(",", "."));
        } catch (NumberFormatException e) {
            Ui.error("El precio debe ser un número.");
            return;
        }
        if (precio <= 0) {
            Ui.error("El precio debe ser mayor que cero.");
            return;
        }
        Async.run(() -> { new localidadservice().generarAsientos(c.getId_concierto(), precio); return true; },
                ok -> {
                    txtPrecio.clear();
                    Ui.info("Se generaron los asientos del concierto.");
                    recargar();
                });
    }
}
