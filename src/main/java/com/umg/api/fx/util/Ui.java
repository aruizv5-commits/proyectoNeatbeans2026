package com.umg.api.fx.util;

import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.util.StringConverter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.function.Function;

/** Utilidades de interfaz: diálogos, formatos y armado de tablas/formularios. */
public final class Ui {
    private Ui() {}

    public static final String MONEDA = "Q";
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    // ---------- Diálogos ----------
    public static void info(String mensaje) { alerta(Alert.AlertType.INFORMATION, "Información", mensaje); }
    public static void error(String mensaje) { alerta(Alert.AlertType.ERROR, "Error", mensaje); }

    public static boolean confirmar(String mensaje) {
        Alert a = new Alert(Alert.AlertType.CONFIRMATION, mensaje, ButtonType.OK, ButtonType.CANCEL);
        a.setTitle("Confirmar");
        a.setHeaderText(null);
        return a.showAndWait().filter(b -> b == ButtonType.OK).isPresent();
    }

    private static void alerta(Alert.AlertType tipo, String titulo, String mensaje) {
        Alert a = new Alert(tipo, mensaje, ButtonType.OK);
        a.setTitle(titulo);
        a.setHeaderText(null);
        a.showAndWait();
    }

    // ---------- Formatos ----------
    /** Los modelos usan float; BigDecimal(Float.toString) evita ver 9.989999771 en vez de 9.99. */
    public static BigDecimal dec(float v) { return new BigDecimal(Float.toString(v)); }

    public static String dinero(BigDecimal v) {
        return MONEDA + String.format(Locale.US, "%,.2f", v == null ? BigDecimal.ZERO : v);
    }

    public static String dinero(float v) { return dinero(dec(v)); }

    /** Toma yyyy-MM-dd del texto tal cual lo manda el servidor (sin convertir zonas horarias). */
    public static LocalDate parseFecha(String texto) {
        if (texto == null || texto.length() < 10) return null;
        try {
            return LocalDate.parse(texto.substring(0, 10));
        } catch (RuntimeException e) {
            return null;
        }
    }

    public static String fecha(String texto) {
        LocalDate d = parseFecha(texto);
        return d == null ? (texto == null ? "" : texto) : FECHA.format(d);
    }

    // ---------- Combos ----------
    /** Para mostrar objetos del modelo en un ComboBox sin tocar su toString(). */
    public static <T> StringConverter<T> convertidor(Function<T, String> texto) {
        return new StringConverter<>() {
            @Override public String toString(T o) { return o == null ? "" : texto.apply(o); }
            @Override public T fromString(String s) { return null; }
        };
    }

    // ---------- Tablas y formularios ----------
    public static <S> TableColumn<S, String> col(String titulo, Function<S, String> valor, double ancho) {
        TableColumn<S, String> c = new TableColumn<>(titulo);
        c.setCellValueFactory(d -> {
            String v = valor.apply(d.getValue());
            return new ReadOnlyStringWrapper(v == null ? "" : v);
        });
        c.setPrefWidth(ancho);
        return c;
    }

    @SafeVarargs
    @SuppressWarnings("deprecation")   // CONSTRAINED_RESIZE_POLICY existe en todas las versiones de JavaFX
    public static <S> void columnas(TableView<S> tabla, TableColumn<S, String>... cols) {
        for (TableColumn<S, String> c : cols) tabla.getColumns().add(c);
        tabla.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        tabla.setPlaceholder(new Label("Sin registros"));
    }

    /** form("Etiqueta", control, "Etiqueta", control, ...) -> formulario de 2 columnas. */
    public static GridPane form(Object... pares) {
        GridPane g = new GridPane();
        g.setHgap(10);
        g.setVgap(10);
        g.setPadding(new Insets(4, 0, 4, 0));
        for (int i = 0, fila = 0; i < pares.length; i += 2, fila++) {
            g.add(new Label((String) pares[i]), 0, fila);
            Node n = (Node) pares[i + 1];
            g.add(n, 1, fila);
            GridPane.setHgrow(n, Priority.ALWAYS);
            if (n instanceof Region r) r.setMaxWidth(Double.MAX_VALUE);
        }
        return g;
    }
}
