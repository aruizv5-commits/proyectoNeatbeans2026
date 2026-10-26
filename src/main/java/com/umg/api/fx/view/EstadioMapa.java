package com.umg.api.fx.view;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

import java.util.List;
import java.util.function.Consumer;

/**
 * Mapa del estadio con los asientos del concierto (VIP, Platea y General).
 *
 * Es independiente del resto de la aplicación: solo dibuja lo que se le pasa con mostrar(...)
 * y avisa con setAlHacerClic(...) cuál asiento se tocó. No consulta la API ni calcula precios.
 *
 * Uso:
 *     EstadioMapa mapa = new EstadioMapa();
 *     mapa.setAlHacerClic(idInventario -> ...);                 // clic en un asiento
 *     mapa.mostrar(List.of(new EstadioMapa.Asiento(...), ...)); // dibujar (se puede llamar las veces que haga falta)
 */
public class EstadioMapa extends VBox {

    /**
     * Un asiento para dibujar.
     *
     * @param id        identificador que se devuelve al hacer clic (aquí, el id_inventario)
     * @param codigo    código del asiento: fila A-J y número 1-10, por ejemplo "A-1" o "J-10"
     * @param localidad nombre de la localidad ("VIP", "Platea", "General"); sirve para el color
     * @param libre     true si se puede vender (si no, se pinta como ocupado)
     * @param elegido   true si ya está en el carrito
     * @param precio    texto que se muestra en el tooltip, por ejemplo "Q1,450.00"
     */
    public record Asiento(int id, String codigo, String localidad, boolean libre, boolean elegido, String precio) {}

    // Colores del mapa de asientos
    private static final String COLOR_VIP = "#f2c14e";
    private static final String COLOR_PLATEA = "#6fa8dc";
    private static final String COLOR_GENERAL = "#93c47d";
    private static final String COLOR_ELEGIDO = "#e8590c";
    private static final String COLOR_OCUPADO = "#cbd5e1";

    // Medidas del dibujo del estadio (después se escala para que quepa en el panel)
    private static final double ESTADIO_ANCHO = 500;
    private static final double ESTADIO_ALTO = 490;
    private static final double ASIENTO = 22;       // tamaño de cada asiento
    private static final double ESPACIO = 3;        // separación entre asientos

    // Cada fila (A a J) es un bloque de 10 asientos: {x, y, 1 = acostado / 0 = parado}
    // A (VIP) y B, C (Platea) van en la cancha, Platea más lejos del escenario.
    // D y E a la izquierda, F y G a la derecha, H, I y J abajo: gradas de General.
    // Detrás del escenario (arriba) es zona cerrada, no se vende.
    private static final double[][] BLOQUES = {
            {189, 176, 1}, {122, 266, 1}, {256, 266, 1},
            {36, 108, 0}, {36, 242, 0},
            {417, 108, 0}, {417, 242, 0},
            {53, 397, 1}, {189, 397, 1}, {325, 397, 1}
    };

    private final Group estadio = new Group();          // dibujo con medidas fijas
    private final Group asientos = new Group();         // los botones de los asientos (se redibujan)
    private final Pane area = new Pane(estadio);        // se encarga de escalar el dibujo
    private Consumer<Integer> alHacerClic = id -> { };

    public EstadioMapa() {
        super(10);

        construirEstadio();
        area.setMinSize(0, 0);
        area.widthProperty().addListener((o, a, b) -> ajustarEstadio());
        area.heightProperty().addListener((o, a, b) -> ajustarEstadio());
        VBox.setVgrow(area, Priority.ALWAYS);

        // FlowPane: si el panel es angosto, la leyenda baja a otra línea en vez de cortarse
        FlowPane leyenda = new FlowPane(10, 4, cuadro(COLOR_VIP, "VIP"), cuadro(COLOR_PLATEA, "Platea"),
                cuadro(COLOR_GENERAL, "General"), cuadro(COLOR_ELEGIDO, "Elegido"), cuadro(COLOR_OCUPADO, "Ocupado"));
        leyenda.setAlignment(Pos.CENTER);
        leyenda.setMinHeight(Region.USE_PREF_SIZE);   // que no se encoja: el mapa es el que cede espacio

        Label ayuda = new Label("Haz clic en un asiento para agregarlo al carrito; si haces clic otra vez, se quita.");
        ayuda.setWrapText(true);
        ayuda.setMinHeight(Region.USE_PREF_SIZE);
        ayuda.getStyleClass().add("subtitulo");

        getChildren().addAll(area, leyenda, ayuda);
    }

    /** Qué hacer cuando se toca un asiento libre o elegido; recibe el id del asiento. */
    public void setAlHacerClic(Consumer<Integer> accion) {
        this.alHacerClic = accion == null ? id -> { } : accion;
    }

    /** Dibuja los asientos recibidos (borra los anteriores). Con lista vacía deja solo el estadio. */
    public void mostrar(List<Asiento> lista) {
        asientos.getChildren().clear();

        for (Asiento a : lista) {
            String codigo = a.codigo();
            if (codigo == null || !codigo.contains("-")) continue;
            String[] partes = codigo.split("-");
            int fila;
            int numero;
            try {
                fila = partes[0].trim().toUpperCase().charAt(0) - 'A';
                numero = Integer.parseInt(partes[1].trim());
            } catch (Exception e) {
                continue;
            }
            if (fila < 0 || fila >= BLOQUES.length || numero < 1 || numero > 10) continue;

            // posición dentro del bloque: los de arriba y abajo van acostados (5 x 2), los de los lados parados (2 x 5)
            double[] bloque = BLOQUES[fila];
            int k = numero - 1;
            int columnas = bloque[2] == 1 ? 5 : 2;
            double x = bloque[0] + (k % columnas) * (ASIENTO + ESPACIO);
            double y = bloque[1] + (k / columnas) * (ASIENTO + ESPACIO);

            String zona = zonaDe(a.localidad(), fila);
            String color;
            if (a.elegido()) color = COLOR_ELEGIDO;
            else if (!a.libre()) color = COLOR_OCUPADO;
            else color = colorDe(zona);

            Button b = new Button(String.valueOf(numero));
            b.setPrefSize(ASIENTO, ASIENTO);
            b.setMinSize(ASIENTO, ASIENTO);
            b.setMaxSize(ASIENTO, ASIENTO);
            b.setPadding(Insets.EMPTY);
            b.setStyle("-fx-background-color: " + color + "; -fx-text-fill: #1e293b; "
                    + "-fx-background-radius: 5; -fx-font-size: 10px;");
            b.setTooltip(new Tooltip(codigo + " · " + zona + " · " + a.precio()));
            b.setDisable(!a.libre() && !a.elegido());
            b.setOnAction(e -> alHacerClic.accept(a.id()));
            b.relocate(x, y);

            asientos.getChildren().add(b);
        }
    }

    // Tipo de boleto del asiento: sale del nombre de la localidad (sin importar mayúsculas ni espacios);
    // si el nombre no se reconoce se usa la fila: A = VIP, B y C = Platea, el resto General
    private String zonaDe(String nombre, int fila) {
        if (nombre != null) {
            String n = nombre.trim().toLowerCase();
            if (n.equals("vip")) return "VIP";
            if (n.startsWith("plat")) return "Platea";
            if (n.equals("general")) return "General";
        }
        if (fila == 0) return "VIP";
        if (fila <= 2) return "Platea";
        return "General";
    }

    private String colorDe(String zona) {
        if ("VIP".equals(zona)) return COLOR_VIP;
        if ("Platea".equals(zona)) return COLOR_PLATEA;
        return COLOR_GENERAL;
    }

    // Parte fija del estadio: gradas, zona cerrada, cancha con el escenario y letras de los bloques
    private void construirEstadio() {
        Rectangle gradas = new Rectangle(0, 0, ESTADIO_ANCHO, ESTADIO_ALTO);
        gradas.setArcWidth(140);
        gradas.setArcHeight(140);
        gradas.setFill(Color.web("#f8fafc"));
        gradas.setStroke(Color.web("#cbd5e1"));
        estadio.getChildren().add(gradas);

        // detrás del escenario no se venden boletos
        Rectangle cerrada = new Rectangle(108, 40, 284, 60);
        cerrada.setArcWidth(24);
        cerrada.setArcHeight(24);
        cerrada.setFill(Color.web("#cbd5e1"));
        Label textoCerrada = new Label("ZONA CERRADA · sin venta");
        textoCerrada.setPrefSize(284, 60);
        textoCerrada.setAlignment(Pos.CENTER);
        textoCerrada.relocate(108, 40);
        textoCerrada.setStyle("-fx-text-fill: #64748b; -fx-font-size: 11px; -fx-font-weight: bold;");
        estadio.getChildren().addAll(cerrada, textoCerrada);

        // gradas de General: un panel detrás de cada bloque, como las secciones de un estadio
        double largo = 5 * (ASIENTO + ESPACIO) - ESPACIO;
        double corto = 2 * (ASIENTO + ESPACIO) - ESPACIO;
        for (int i = 3; i < BLOQUES.length; i++) {
            boolean acostado = BLOQUES[i][2] == 1;
            Rectangle seccion = new Rectangle(BLOQUES[i][0] - 5, BLOQUES[i][1] - 5,
                    (acostado ? largo : corto) + 10, (acostado ? corto : largo) + 10);
            seccion.setArcWidth(12);
            seccion.setArcHeight(12);
            seccion.setFill(Color.web("#e2e8f0"));
            estadio.getChildren().add(seccion);
        }

        // cancha con franjas
        for (int i = 0; i < 8; i++) {
            Rectangle franja = new Rectangle(108 + i * 35.5, 112, 35.5, 256);
            franja.setFill(Color.web(i % 2 == 0 ? "#5a9e57" : "#4a8a47"));
            estadio.getChildren().add(franja);
        }
        Rectangle bordeCancha = new Rectangle(118, 122, 264, 236);
        bordeCancha.setFill(Color.TRANSPARENT);
        bordeCancha.setStroke(Color.WHITE);
        estadio.getChildren().add(bordeCancha);

        Label escenario = new Label("ESCENARIO");
        escenario.setPrefSize(180, 36);
        escenario.setAlignment(Pos.CENTER);
        escenario.relocate(160, 124);
        escenario.setStyle("-fx-background-color: #334155; -fx-text-fill: white; "
                + "-fx-font-weight: bold; -fx-background-radius: 10;");
        estadio.getChildren().addAll(escenario,
                puntoCardinal("NORTE", 250, 15, 0), puntoCardinal("SUR", 250, 475, 0),
                puntoCardinal("OESTE", 16, 245, -90), puntoCardinal("ESTE", 484, 245, 90));

        // letra de cada bloque (la fila A-J); sobre la cancha va en blanco
        for (int i = 0; i < BLOQUES.length; i++) {
            Label letra = new Label(String.valueOf((char) ('A' + i)));
            letra.relocate(BLOQUES[i][0], BLOQUES[i][1] - 15);
            letra.setStyle("-fx-text-fill: " + (i < 3 ? "white" : "#475569")
                    + "; -fx-font-size: 11px; -fx-font-weight: bold;");
            estadio.getChildren().add(letra);
        }

        estadio.getChildren().add(asientos);   // al final para que quede encima
    }

    private Label puntoCardinal(String texto, double x, double y, double giro) {
        Label l = new Label(texto);
        l.setPrefSize(60, 16);
        l.setAlignment(Pos.CENTER);
        l.relocate(x - 30, y - 8);
        l.setRotate(giro);
        l.setStyle("-fx-text-fill: #475569; -fx-font-size: 11px; -fx-font-weight: bold;");
        return l;
    }

    // Escala el estadio para que quepa en el espacio disponible y lo centra
    private void ajustarEstadio() {
        double ancho = area.getWidth();
        double alto = area.getHeight();
        double escala = Math.min(ancho / ESTADIO_ANCHO, alto / ESTADIO_ALTO);
        if (escala <= 0) return;
        estadio.setScaleX(escala);
        estadio.setScaleY(escala);
        estadio.setLayoutX((ancho - ESTADIO_ANCHO) / 2);
        estadio.setLayoutY((alto - ESTADIO_ALTO) / 2);
    }

    // cuadrito de color con su texto, para la leyenda del mapa
    private HBox cuadro(String color, String texto) {
        Label muestra = new Label();
        muestra.setPrefSize(14, 14);
        muestra.setMinSize(14, 14);
        muestra.setStyle("-fx-background-color: " + color + "; -fx-background-radius: 4;");
        HBox h = new HBox(4, muestra, new Label(texto));
        h.setAlignment(Pos.CENTER_LEFT);
        return h;
    }
}
