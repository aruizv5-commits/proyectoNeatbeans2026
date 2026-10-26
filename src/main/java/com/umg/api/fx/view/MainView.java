package com.umg.api.fx.view;

import com.umg.api.config.Sesion;
import com.umg.api.fx.FxApp;
import com.umg.api.model.usuario;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import java.util.*;
import java.util.function.Function;

/** Navegación JavaFX inspirada en el menú lateral Swing recibido. */
public class MainView extends BorderPane {
    private record Modulo(String id,String titulo,String icono) {}
    private final VBox lateral=new VBox(18);
    private final Label marca=new Label("Boletería"),submarca=new Label("CONCIERTOS Y EVENTOS"),pie=new Label("Venta en puntos físicos");
    private final Label titulo=new Label();
    private final ToggleGroup grupo=new ToggleGroup();
    private final Map<String,ToggleButton> botones=new LinkedHashMap<>();
    private final Map<String,ScrollPane> vistas=new HashMap<>();
    private final List<Modulo> modulos=new ArrayList<>();
    private final Function<String,Node> fabrica;
    private boolean compacto;

    public MainView(usuario u){this(u,MainView::crearVista);}

    // Permite verificar navegación con vistas de prueba, sin llamadas a la API.
    MainView(usuario u,Function<String,Node> fabrica){
        this.fabrica=Objects.requireNonNull(fabrica);
        getStyleClass().add("app-shell");
        modulos.add(new Modulo("ventas","Punto de venta","V"));
        modulos.add(new Modulo("clientes","Clientes","C"));
        modulos.add(new Modulo("comprobantes","Comprobantes y QR","B"));
        if(Sesion.esAdmin()){
            modulos.add(new Modulo("artistas","Artistas","A"));
            modulos.add(new Modulo("conciertos","Conciertos","E"));
            modulos.add(new Modulo("inventario","Localidades e inventario","L"));
            modulos.add(new Modulo("usuarios","Usuarios","U"));
            modulos.add(new Modulo("reportes","Reportes de ventas","R"));
        }
        marca.getStyleClass().add("sidebar-brand");submarca.getStyleClass().add("sidebar-subtitle");
        VBox identidad=new VBox(6,marca,submarca);identidad.getStyleClass().add("sidebar-identity");
        VBox enlaces=new VBox(6);
        for(Modulo modulo:modulos){
            ToggleButton boton=new ToggleButton(modulo.titulo());
            boton.setId("nav-"+modulo.id());boton.setAccessibleText(modulo.titulo());
            boton.setTooltip(new Tooltip(modulo.titulo()));boton.setToggleGroup(grupo);
            boton.setMaxWidth(Double.MAX_VALUE);boton.setWrapText(true);boton.setAlignment(Pos.CENTER_LEFT);
            Label icono=new Label(modulo.icono());icono.getStyleClass().add("nav-icon");boton.setGraphic(icono);
            boton.getStyleClass().add("nav-button");boton.setOnAction(e->mostrar(modulo));
            botones.put(modulo.id(),boton);enlaces.getChildren().add(boton);
        }
        ScrollPane menu=new ScrollPane(enlaces);menu.setFitToWidth(true);menu.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        menu.getStyleClass().add("sidebar-menu");VBox.setVgrow(menu,Priority.ALWAYS);
        pie.getStyleClass().add("sidebar-footer");
        lateral.getStyleClass().add("sidebar");lateral.getChildren().addAll(identidad,menu,pie);anchoLateral();setLeft(lateral);

        Button plegar=new Button("☰");plegar.setId("toggle-menu");plegar.setAccessibleText("Expandir o contraer menú");
        plegar.setTooltip(new Tooltip("Expandir o contraer menú"));plegar.getStyleClass().add("menu-toggle");
        plegar.setOnAction(e->{compacto=!compacto;anchoLateral();});
        titulo.getStyleClass().add("workspace-title");
        Region espacio=new Region();HBox.setHgrow(espacio,Priority.ALWAYS);
        String nombre=u.getNombre_completo()==null||u.getNombre_completo().isBlank()?u.getNombre_usuario():u.getNombre_completo();
        Label persona=new Label(nombre);persona.getStyleClass().add("user-name");
        Label rol=new Label(Sesion.esAdmin()?"Administrador":"Vendedor");rol.getStyleClass().add("role-badge");
        Button salir=new Button("Cerrar sesión");salir.setId("logout");salir.getStyleClass().add("logout-button");
        salir.setOnAction(e->{Sesion.cerrar();FxApp.mostrarLogin(null);});
        HBox cabecera=new HBox(14,plegar,titulo,espacio,persona,rol,salir);
        cabecera.setAlignment(Pos.CENTER_LEFT);cabecera.getStyleClass().add("workspace-header");
        BorderPane trabajo=new BorderPane();trabajo.setTop(cabecera);setCenter(trabajo);
        // El área central pertenece al contenedor de trabajo, no al menú.
        trabajo.setId("workspace");
        mostrar(modulos.get(0));
    }
    private void anchoLateral(){
        double ancho=compacto?72:220;lateral.setPrefWidth(ancho);lateral.setMinWidth(ancho);lateral.setMaxWidth(ancho);
        marca.setText(compacto?"B":"Boletería");submarca.setVisible(!compacto);submarca.setManaged(!compacto);
        pie.setVisible(!compacto);pie.setManaged(!compacto);
        for(Modulo m:modulos){ToggleButton b=botones.get(m.id());b.setText(compacto?"":m.titulo());}
    }
    private void mostrar(Modulo modulo){
        ScrollPane pagina=vistas.get(modulo.id());
        if(pagina==null){
            Node vista=Objects.requireNonNull(fabrica.apply(modulo.id()));
            pagina=new ScrollPane(vista);pagina.setFitToWidth(true);pagina.setFitToHeight(true);
            pagina.setPannable(false);pagina.getStyleClass().add("workspace-scroll");
            pagina.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);pagina.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
            vistas.put(modulo.id(),pagina);
        }
        ((BorderPane)getCenter()).setCenter(pagina);titulo.setText(modulo.titulo());botones.get(modulo.id()).setSelected(true);
        if(pagina.getContent() instanceof Recargable r)r.recargar();
    }
    private static Node crearVista(String id){
        return switch(id){
            case "ventas"->new PuntoVentaView();case "clientes"->new ClientesView();case "comprobantes"->new ComprobantesView();
            case "artistas"->new ArtistasView();case "conciertos"->new ConciertosView();case "inventario"->new InventarioView();
            case "usuarios"->new UsuariosView();case "reportes"->new ReportesView();
            default->throw new IllegalArgumentException("Módulo desconocido: "+id);
        };
    }
}
