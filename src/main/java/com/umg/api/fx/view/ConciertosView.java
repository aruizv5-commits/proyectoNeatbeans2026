package com.umg.api.fx.view;

import com.umg.api.fx.util.Async;
import com.umg.api.fx.util.Ui;
import com.umg.api.model.artista;
import com.umg.api.model.concierto;
import com.umg.api.service.artistaservice;
import com.umg.api.service.conciertoservice;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ConciertosView extends HBox implements Recargable {
    private static final List<String> ESTADOS = List.of("programado", "activo", "finalizado", "cancelado");

    private final Map<Integer, String> nombreArtista = new HashMap<>();
    private final TableView<concierto> tabla = new TableView<>();
    private final ComboBox<artista> cbArtista = new ComboBox<>();
    private final TextField txtTitulo = new TextField();
    private final TextField txtTipo = new TextField();
    private final DatePicker dpFecha = new DatePicker();
    private final TextField txtHorario = new TextField("20:00");
    private final TextField txtRecinto = new TextField();
    private final TextField txtCiudad = new TextField();
    private final ComboBox<String> cbEstado = new ComboBox<>();
    private concierto seleccionado;

    public ConciertosView() {
        setSpacing(16);
        setPadding(new Insets(14));

        Ui.columnas(tabla,
                Ui.col("Evento", concierto::getTitulo_evento, 170),
                Ui.col("Artista", c -> nombreArtista.get(c.getId_artista()), 120),
                Ui.col("Fecha", c -> Ui.fecha(c.getFecha_concierto()), 90),
                Ui.col("Hora", concierto::getHorario, 60),
                Ui.col("Recinto", concierto::getRecinto, 110),
                Ui.col("Ciudad", concierto::getCiudad, 90),
                Ui.col("Estado", concierto::getEstado, 90));
        tabla.getSelectionModel().selectedItemProperty().addListener((o, a, s) -> {
            seleccionado = s;
            if (s != null) cargarForm(s);
        });
        VBox izq = new VBox(tabla);
        VBox.setVgrow(tabla, Priority.ALWAYS);
        izq.getStyleClass().add("panel");
        HBox.setHgrow(izq, Priority.ALWAYS);

        cbArtista.setConverter(Ui.convertidor(artista::getNombre_artistico));
        cbEstado.getItems().setAll(ESTADOS);
        cbEstado.setValue("programado");
        txtHorario.setPromptText("HH:mm");
        txtTipo.setPromptText("Concierto, Festival…");

        Label titulo = new Label("Concierto");
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
                Ui.form("Artista", cbArtista, "Título del evento", txtTitulo, "Tipo de evento", txtTipo,
                        "Fecha", dpFecha, "Horario (24 h)", txtHorario, "Recinto", txtRecinto,
                        "Ciudad", txtCiudad, "Estado", cbEstado),
                new HBox(8, nuevo, guardar, eliminar));
        der.getStyleClass().add("panel");
        der.setPrefWidth(360);
        der.setMinWidth(360);

        getChildren().addAll(izq, der);
    }

    @Override
    public void recargar() {
        Async.run(() -> new Object[]{ new artistaservice().getArtista(), new conciertoservice().getConcierto() }, r -> {
            @SuppressWarnings("unchecked") List<artista> artistas = (List<artista>) r[0];
            @SuppressWarnings("unchecked") List<concierto> conciertos = (List<concierto>) r[1];
            nombreArtista.clear();
            artistas.forEach(a -> nombreArtista.put(a.getId_artista(), a.getNombre_artistico()));
            cbArtista.getItems().setAll(artistas);
            tabla.getItems().setAll(conciertos);
            if (seleccionado != null) seleccionarArtista(seleccionado.getId_artista());
        });
    }

    private void seleccionarArtista(int id) {
        cbArtista.getItems().stream().filter(a -> a.getId_artista() == id)
                .findFirst().ifPresent(cbArtista::setValue);
    }

    private void cargarForm(concierto c) {
        seleccionarArtista(c.getId_artista());
        txtTitulo.setText(c.getTitulo_evento());
        txtTipo.setText(c.getTipo_evento() == null ? "" : c.getTipo_evento());
        dpFecha.setValue(Ui.parseFecha(c.getFecha_concierto()));
        txtHorario.setText(c.getHorario() == null ? "" : c.getHorario());
        txtRecinto.setText(c.getRecinto());
        txtCiudad.setText(c.getCiudad() == null ? "" : c.getCiudad());
        cbEstado.setValue(c.getEstado());
    }

    private void limpiar() {
        tabla.getSelectionModel().clearSelection();
        seleccionado = null;
        cbArtista.setValue(null);
        txtTitulo.clear();
        txtTipo.clear();
        dpFecha.setValue(null);
        txtHorario.setText("20:00");
        txtRecinto.clear();
        txtCiudad.clear();
        cbEstado.setValue("programado");
    }

    private void guardar() {
        artista art = cbArtista.getValue();
        String titulo = txtTitulo.getText().trim(), recinto = txtRecinto.getText().trim();
        LocalDate fecha = dpFecha.getValue();
        if (art == null || titulo.isEmpty() || recinto.isEmpty() || fecha == null || cbEstado.getValue() == null) {
            Ui.error("Completa artista, título, fecha, recinto y estado.");
            return;
        }
        String horario = txtHorario.getText().trim();
        if (!horario.isEmpty()) {
            try {
                horario = LocalTime.parse(horario).toString();     // normaliza a HH:mm
            } catch (DateTimeParseException e) {
                Ui.error("El horario debe tener el formato HH:mm, por ejemplo 20:30.");
                return;
            }
        }
        concierto sel = seleccionado;
        concierto c = new concierto(sel == null ? 0 : sel.getId_concierto(), art.getId_artista(), titulo, recinto,
                txtCiudad.getText().trim(), cbEstado.getValue(), txtTipo.getText().trim(), fecha.toString(), horario);
        Async.run(() -> sel == null
                ? new conciertoservice().createConcierto(c)
                : new conciertoservice().updateConcierto(sel.getId_concierto(), c),
            r -> { limpiar(); recargar(); });
    }

    private void eliminar() {
        concierto sel = seleccionado;
        if (sel == null) { Ui.error("Selecciona un concierto."); return; }
        if (!Ui.confirmar("¿Eliminar el concierto \"" + sel.getTitulo_evento() + "\"?")) return;
        Async.run(() -> { new conciertoservice().deleteConcierto(sel.getId_concierto()); return true; },
                ok -> { limpiar(); recargar(); });
    }
}
