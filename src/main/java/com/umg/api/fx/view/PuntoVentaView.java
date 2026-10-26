package com.umg.api.fx.view;

import com.umg.api.config.Sesion;
import com.umg.api.fx.util.Async;
import com.umg.api.fx.util.Ui;
import com.umg.api.model.artista;
import com.umg.api.model.cliente;
import com.umg.api.model.concierto;
import com.umg.api.model.inventarios_boletos;
import com.umg.api.model.localidad;
import com.umg.api.model.ventas;
import com.umg.api.service.artistaservice;
import com.umg.api.service.checkoutservice;
import com.umg.api.service.clienteservice;
import com.umg.api.service.conciertoservice;
import com.umg.api.service.inventarios_boletosservice;
import com.umg.api.service.localidadservice;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Pantalla del vendedor:  [1] elegir concierto (filtros por artista/fecha)
 *                         [2] elegir localidad + cantidad  ->  [3] carrito  ->  Cobrar.
 * Una venta puede mezclar varias localidades y hasta varios conciertos.
 */
public class PuntoVentaView extends HBox implements Recargable {

    /** Estados de concierto que permiten vender. */
    private static final Set<String> VENDIBLES = Set.of("programado", "activo");

    /** Todo lo que necesita la pantalla, pedido junto en una sola tarea de fondo. */
    private record Datos(List<artista> artistas, List<concierto> conciertos, List<localidad> localidades,
                         List<inventarios_boletos> inventarios, List<cliente> clientes) {}

    private final Map<Integer, String> nombreArtista = new HashMap<>();
    private final Map<Integer, String> nombreLocalidad = new HashMap<>();
    private final Map<Integer, String> asientoLocalidad = new HashMap<>();
    private final Set<Integer> asientosLibres = new java.util.HashSet<>();
    private List<inventarios_boletos> todoInventario = List.of();

    // [1] Conciertos
    private final ObservableList<concierto> todos = FXCollections.observableArrayList();
    private final FilteredList<concierto> filtrados = new FilteredList<>(todos);
    private final ComboBox<artista> cbArtista = new ComboBox<>();
    private final DatePicker dpFecha = new DatePicker();
    private final TableView<concierto> tblConciertos = new TableView<>();

    // [2] Localidades del concierto seleccionado
    private final ObservableList<inventarios_boletos> inventario = FXCollections.observableArrayList();
    private final TableView<inventarios_boletos> tblInventario = new TableView<>();
    private final Label lblEvento = new Label("Selecciona un concierto");
    private concierto conciertoActual;

    // [3] Carrito
    record Linea(concierto concierto, inventarios_boletos inv, String localidad, BigDecimal precio, int cantidad) {
        BigDecimal subtotal() { return precio.multiply(BigDecimal.valueOf(cantidad)); }
    }
    private final ObservableList<Linea> carrito = FXCollections.observableArrayList();
    private final TableView<Linea> tblCarrito = new TableView<>();
    private final ComboBox<cliente> cbCliente = new ComboBox<>();
    private final Label lblTotal = new Label();
    private final SimpleBooleanProperty cobrando = new SimpleBooleanProperty(false);

    public PuntoVentaView() {
        setSpacing(14);
        setPadding(new Insets(14));

        VBox izq = panelConciertos();
        VBox centro = panelLocalidades();
        VBox der = panelCarrito();
        izq.setPrefWidth(360);
        izq.setMinWidth(280);
        centro.setPrefWidth(290);
        centro.setMinWidth(245);
        der.setPrefWidth(340);
        der.setMinWidth(300);
        for (VBox p : List.of(izq, centro, der)) HBox.setHgrow(p, Priority.ALWAYS);
        getChildren().addAll(izq, centro, der);

        actualizarTotal();
    }

    // ------------------------------------------------------------------ paneles

    private VBox panelConciertos() {
        Label titulo = new Label("1. Concierto");
        titulo.getStyleClass().add("seccion");

        cbArtista.setPromptText("Todos los artistas");
        cbArtista.setConverter(Ui.convertidor(artista::getNombre_artistico));
        cbArtista.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(cbArtista, Priority.ALWAYS);
        dpFecha.setPromptText("Fecha");
        Button limpiar = new Button("✕");
        limpiar.setOnAction(e -> { cbArtista.setValue(null); dpFecha.setValue(null); });
        cbArtista.valueProperty().addListener((o, a, b) -> aplicarFiltros());
        dpFecha.valueProperty().addListener((o, a, b) -> aplicarFiltros());
        HBox filtros = new HBox(8, cbArtista, dpFecha, limpiar);

        Ui.columnas(tblConciertos,
                Ui.col("Evento", concierto::getTitulo_evento, 150),
                Ui.col("Artista", c -> nombreArtista.get(c.getId_artista()), 110),
                Ui.col("Fecha", c -> Ui.fecha(c.getFecha_concierto())
                        + (c.getHorario() == null || c.getHorario().isBlank() ? "" : " " + c.getHorario()), 110),
                Ui.col("Recinto", concierto::getRecinto, 100));
        SortedList<concierto> ordenados = new SortedList<>(filtrados);
        ordenados.comparatorProperty().bind(tblConciertos.comparatorProperty());
        tblConciertos.setItems(ordenados);
        tblConciertos.getSelectionModel().selectedItemProperty().addListener((o, a, c) -> mostrarInventario(c));
        VBox.setVgrow(tblConciertos, Priority.ALWAYS);

        VBox p = new VBox(10, titulo, filtros, tblConciertos);
        p.getStyleClass().add("panel");
        return p;
    }

    private VBox panelLocalidades() {
        Label titulo = new Label("2. Asiento");
        titulo.getStyleClass().add("seccion");
        lblEvento.setWrapText(true);

        Ui.columnas(tblInventario,
                Ui.col("Localidad", i -> nombreLocalidad.get(i.getId_localidad()), 110),
                Ui.col("Asiento", i -> asientoLocalidad.get(i.getId_localidad()), 80),
                Ui.col("Precio", i -> Ui.dinero(precioFinal(i)), 80));
        tblInventario.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        tblInventario.getColumns().get(0).setMinWidth(68);
        tblInventario.getColumns().get(1).setMinWidth(48);
        tblInventario.getColumns().get(2).setMinWidth(92);
        tblInventario.setItems(inventario);
        VBox.setVgrow(tblInventario, Priority.ALWAYS);

        Button btnAgregar = new Button("Agregar al carrito");
        btnAgregar.getStyleClass().add("boton-primario");
        btnAgregar.disableProperty().bind(tblInventario.getSelectionModel().selectedItemProperty().isNull());
        btnAgregar.setOnAction(e -> agregarAlCarrito());
        HBox acciones = new HBox(10, btnAgregar);
        acciones.setAlignment(Pos.CENTER_LEFT);

        VBox p = new VBox(10, titulo, lblEvento, tblInventario, acciones);
        p.getStyleClass().add("panel");
        return p;
    }

    private VBox panelCarrito() {
        Label titulo = new Label("3. Carrito");
        titulo.getStyleClass().add("seccion");

        Ui.columnas(tblCarrito,
                Ui.col("Evento", l -> l.concierto().getTitulo_evento(), 120),
                Ui.col("Localidad", Linea::localidad, 90),
                Ui.col("Cant.", l -> String.valueOf(l.cantidad()), 50),
                Ui.col("Subtotal", l -> Ui.dinero(l.subtotal()), 90));
        tblCarrito.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        tblCarrito.getColumns().get(0).setMinWidth(64);
        tblCarrito.getColumns().get(1).setMinWidth(64);
        tblCarrito.getColumns().get(2).setMinWidth(46);
        tblCarrito.getColumns().get(3).setMinWidth(92);
        tblCarrito.setItems(carrito);
        VBox.setVgrow(tblCarrito, Priority.ALWAYS);
        carrito.addListener((ListChangeListener<Linea>) c -> actualizarTotal());

        Button quitar = new Button("Quitar");
        quitar.disableProperty().bind(tblCarrito.getSelectionModel().selectedItemProperty().isNull());
        quitar.setOnAction(e -> carrito.remove(tblCarrito.getSelectionModel().getSelectedItem()));
        Button vaciar = new Button("Vaciar");
        vaciar.disableProperty().bind(Bindings.isEmpty(carrito));
        vaciar.setOnAction(e -> carrito.clear());
        HBox botonesCarrito = new HBox(8, quitar, vaciar);

        cbCliente.setPromptText("Selecciona un cliente");
        cbCliente.setConverter(Ui.convertidor(c ->
                (c.getNombre() + " " + (c.getApellido() == null ? "" : c.getApellido())).trim()
                        + (c.getNit() == null || c.getNit().isBlank() ? "" : "  ·  NIT " + c.getNit())));
        cbCliente.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(cbCliente, Priority.ALWAYS);
        Button sinCliente = new Button("✕");
        sinCliente.setOnAction(e -> cbCliente.setValue(null));
        Label etiquetaCliente = new Label("Cliente:");
        etiquetaCliente.setMinWidth(Region.USE_PREF_SIZE);
        cbCliente.setMinWidth(0);
        HBox filaCliente = new HBox(8, etiquetaCliente, cbCliente, sinCliente);
        filaCliente.setAlignment(Pos.CENTER_LEFT);

        lblTotal.getStyleClass().add("total");
        Region esp = new Region();
        HBox.setHgrow(esp, Priority.ALWAYS);
        Button cobrar = new Button("Cobrar");
        cobrar.getStyleClass().add("boton-primario");
        cobrar.disableProperty().bind(Bindings.isEmpty(carrito).or(cobrando));
        cobrar.setOnAction(e -> cobrar());
        HBox pie = new HBox(10, new Label("Total:"), lblTotal, esp, cobrar);
        pie.setAlignment(Pos.CENTER_LEFT);

        VBox p = new VBox(10, titulo, tblCarrito, botonesCarrito, filaCliente, pie);
        p.getStyleClass().add("panel");
        return p;
    }

    // ------------------------------------------------------------------ datos

    @Override
    public void recargar() {
        Integer idSeleccionado = conciertoActual == null ? null : conciertoActual.getId_concierto();
        Integer idArtistaFiltro = cbArtista.getValue() == null ? null : cbArtista.getValue().getId_artista();
        Integer idCliente = cbCliente.getValue() == null ? null : cbCliente.getValue().getId();

        Async.run(() -> new Datos(
                new artistaservice().getArtista(),
                new conciertoservice().getConcierto(),
                new localidadservice().getLocalidad(),
                new inventarios_boletosservice().getInventarios_boletos(),
                new clienteservice().getCliente()), d -> {

            nombreArtista.clear();
            d.artistas().forEach(a -> nombreArtista.put(a.getId_artista(), a.getNombre_artistico()));
            nombreLocalidad.clear();
            d.localidades().forEach(l -> nombreLocalidad.put(l.getId_localidad(), l.getNombre()));
            asientoLocalidad.clear();
            asientosLibres.clear();
            for (localidad l : d.localidades()) {
                asientoLocalidad.put(l.getId_localidad(), l.getAsiento());
                if (l.isEstado()) asientosLibres.add(l.getId_localidad());   // estado true = asiento libre
            }
            todoInventario = d.inventarios();

            cbArtista.getItems().setAll(d.artistas());
            cbArtista.getItems().stream().filter(a -> Objects.equals(a.getId_artista(), idArtistaFiltro))
                    .findFirst().ifPresent(cbArtista::setValue);
            cbCliente.getItems().setAll(d.clientes());
            cbCliente.getItems().stream().filter(c -> Objects.equals(c.getId(), idCliente))
                    .findFirst().ifPresent(cbCliente::setValue);

            todos.setAll(d.conciertos().stream()
                    .filter(c -> c.getEstado() != null && VENDIBLES.contains(c.getEstado().trim().toLowerCase()))
                    .toList());
            tblConciertos.refresh();
            tblInventario.refresh();

            concierto reseleccion = todos.stream()
                    .filter(c -> idSeleccionado != null && c.getId_concierto() == idSeleccionado)
                    .findFirst().orElse(null);
            if (reseleccion != null) tblConciertos.getSelectionModel().select(reseleccion);
            else mostrarInventario(tblConciertos.getSelectionModel().getSelectedItem());
        });
    }

    private void aplicarFiltros() {
        artista a = cbArtista.getValue();
        LocalDate f = dpFecha.getValue();
        filtrados.setPredicate(c -> {
            if (a != null && c.getId_artista() != a.getId_artista()) return false;
            if (f != null && !f.equals(Ui.parseFecha(c.getFecha_concierto()))) return false;
            return true;
        });
    }

    private void mostrarInventario(concierto c) {
        conciertoActual = c;
        if (c == null) {
            inventario.clear();
            lblEvento.setText("Selecciona un concierto");
            return;
        }
        lblEvento.setText(c.getTitulo_evento() + "\n" + Ui.fecha(c.getFecha_concierto())
                + (c.getHorario() == null ? "" : " " + c.getHorario()) + " · " + c.getRecinto());
        inventario.setAll(todoInventario.stream()
                .filter(i -> i.getId_concierto() == c.getId_concierto()
                        && i.getCantidad_disponible() > 0
                        && asientosLibres.contains(i.getId_localidad())).toList());
    }

    // ------------------------------------------------------------------ carrito

    private int enCarrito(int idInventario) {
        return carrito.stream().filter(l -> l.inv().getId_inventario() == idInventario)
                .mapToInt(Linea::cantidad).sum();
    }

    // La API cobra un recargo según la localidad: VIP +45 % y "Plata" +25 %
    private BigDecimal precioFinal(inventarios_boletos inv) {
        BigDecimal precio = Ui.dec(inv.getPrecio());
        String nombre = nombreLocalidad.get(inv.getId_localidad());
        if ("VIP".equals(nombre)) {
            return precio.multiply(new BigDecimal("1.45"));
        }
        if ("Plata".equals(nombre)) {
            return precio.multiply(new BigDecimal("1.25"));
        }
        return precio;
    }

    private void agregarAlCarrito() {
        inventarios_boletos inv = tblInventario.getSelectionModel().getSelectedItem();
        if (inv == null || conciertoActual == null) return;
        String loc = nombreLocalidad.getOrDefault(inv.getId_localidad(), "Localidad " + inv.getId_localidad())
                + " " + asientoLocalidad.getOrDefault(inv.getId_localidad(), "");

        // cada asiento se vende una sola vez
        if (enCarrito(inv.getId_inventario()) > 0) {
            Ui.error("Ese asiento ya está en el carrito.");
            return;
        }
        carrito.add(new Linea(conciertoActual, inv, loc.trim(), precioFinal(inv), 1));
    }

    private BigDecimal total() {
        return carrito.stream().map(Linea::subtotal).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private void actualizarTotal() {
        lblTotal.setText(Ui.dinero(total()));
    }

    private void cobrar() {
        if (carrito.isEmpty()) return;
        if (cbCliente.getValue() == null) {
            Ui.error("Selecciona el cliente de la venta.");
            return;
        }
        int boletos = carrito.stream().mapToInt(Linea::cantidad).sum();
        if (!Ui.confirmar(boletos + " boleto(s) por " + Ui.dinero(total()) + "\n¿Confirmar la venta?")) return;

        int idVendedor = Sesion.actual().getId_usuario();
        Integer idCliente = cbCliente.getValue() == null ? null : cbCliente.getValue().getId();
        List<checkoutservice.Item> items = carrito.stream()
                .map(l -> new checkoutservice.Item(l.inv().getId_inventario(), l.cantidad())).toList();

        cobrando.set(true);   // evita doble clic = doble venta
        Async.run(() -> new checkoutservice().registrarVenta(idVendedor, idCliente, items),
                (ventas v) -> {
                    cobrando.set(false);
                    carrito.clear();
                    Ui.info("Venta #" + v.getId_venta() + " registrada.\nTotal cobrado: " + Ui.dinero(v.getTotal_venta()));
                    recargar();
                },
                msg -> {
                    cobrando.set(false);
                    Ui.error(msg);
                    recargar();   // refresca disponibilidad por si otro vendedor agotó boletos
                });
    }
}
