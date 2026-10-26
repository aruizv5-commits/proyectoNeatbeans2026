package com.umg.api.fx.view;

import com.umg.api.config.Sesion;
import com.umg.api.fx.FxApp;
import com.umg.api.model.*;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import java.lang.reflect.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Function;

/** Datos ficticios y vistas instrumentadas: no llama a la API ni cobra. */
public class PruebaInterfazIntegrada {
    static class VistaPrueba extends VBox implements Recargable {
        int recargas;
        final Node contenido;
        VistaPrueba(Node contenido){this.contenido=contenido;getChildren().add(contenido);VBox.setVgrow(contenido,Priority.ALWAYS);}
        public void recargar(){recargas++;}
    }
    static Object campo(Object o,String n)throws Exception{Field f=o.getClass().getDeclaredField(n);f.setAccessible(true);return f.get(o);}
    static void poner(Object o,String n,Object valor)throws Exception{Field f=o.getClass().getDeclaredField(n);f.setAccessible(true);f.set(o,valor);}
    static void invocar(Object o,String n)throws Exception{Method m=o.getClass().getDeclaredMethod(n);m.setAccessible(true);m.invoke(o);}
    static void verificar(boolean b,String motivo){if(!b)throw new AssertionError(motivo);}
    static usuario usuario(String rol){var u=new usuario();u.setId_usuario(7);u.setNombre_completo("Usuario de prueba");u.setNombre_usuario("prueba");u.setRol(rol);return u;}
    static PuntoVentaView venta()throws Exception{
        var p=new PuntoVentaView();
        ((Map<Integer,String>)campo(p,"nombreArtista")).put(1,"Artista de muestra");
        var nombres=(Map<Integer,String>)campo(p,"nombreLocalidad");nombres.put(1,"VIP");nombres.put(2,"Platea");
        var asientos=(Map<Integer,String>)campo(p,"asientoLocalidad");asientos.put(1,"A-1");asientos.put(2,"B-1");
        ((Set<Integer>)campo(p,"asientosLibres")).addAll(List.of(1,2));
        poner(p,"todoInventario",List.of(new inventarios_boletos(1,1,1,1000,1,1),new inventarios_boletos(2,1,2,500,1,1)));
        var c=new concierto();c.setId_concierto(1);c.setId_artista(1);c.setTitulo_evento("Noche de música");c.setFecha_concierto("2026-12-06T00:00:00Z");c.setRecinto("Auditorio de muestra");c.setEstado("programado");
        ((ObservableList<concierto>)campo(p,"todos")).add(c);
        ((TableView<concierto>)campo(p,"tblConciertos")).getSelectionModel().select(c);
        var elegir=p.getClass().getDeclaredMethod("alternarAsiento",int.class);elegir.setAccessible(true);elegir.invoke(p,1);   // clic en el asiento A-1 del mapa
        var cl=new cliente();cl.setId(1);cl.setNombre("Cliente");cl.setApellido("Muestra");cl.setNit("CF");
        var cb=(ComboBox<cliente>)campo(p,"cbCliente");cb.getItems().add(cl);cb.getSelectionModel().selectFirst();return p;
    }
    static ReportesView reporte()throws Exception {
        var r=new ReportesView();Object datos=Class.forName("PruebaReportes").getMethod("datos").invoke(null);poner(r,"datos",datos);
        for(String n:List.of("concierto","artista","vendedor")){
            var cb=(ComboBox<com.umg.api.service.ReporteVentasService.Opcion>)campo(r,n);
            cb.getItems().add(new com.umg.api.service.ReporteVentasService.Opcion(null,"Todos"));cb.getSelectionModel().selectFirst();
        }
        invocar(r,"aplicar");return r;
    }
    static Scene escena(Parent root,double w,double h){var s=new Scene(root,w,h);var css=MainView.class.getResource("/fx/style.css");if(css!=null)s.getStylesheets().add(css.toExternalForm());return s;}
    static void foto(Scene escena,Path destino)throws Exception{
        escena.getRoot().applyCss();escena.getRoot().lookupAll(".nav-button").forEach(Node::applyCss);escena.getRoot().layout();var imagen=escena.snapshot(null);
        var out=new java.awt.image.BufferedImage((int)imagen.getWidth(),(int)imagen.getHeight(),java.awt.image.BufferedImage.TYPE_INT_ARGB);
        for(int y=0;y<out.getHeight();y++)for(int x=0;x<out.getWidth();x++)out.setRGB(x,y,imagen.getPixelReader().getArgb(x,y));
        javax.imageio.ImageIO.write(out,"png",destino.toFile());
    }
    static void accesible(MainView root,String texto){
        ScrollPane sc=(ScrollPane)((BorderPane)root.getCenter()).getCenter();
        Node viewport=sc.lookup(".viewport");
        Button b=sc.getContent().lookupAll(".button").stream().filter(n->n instanceof Button && ((Button)n).getText().equals(texto)).map(n->(Button)n).findFirst().orElseThrow();
        var v=viewport.localToScene(viewport.getBoundsInLocal());var pos=b.localToScene(b.getBoundsInLocal());
        verificar(pos.getMinX()>=v.getMinX()-2 && pos.getMaxX()<=v.getMaxX()+2 && pos.getMinY()>=v.getMinY()-2 && pos.getMaxY()<=v.getMaxY()+2,"Botón fuera de viewport: "+texto+" "+pos+" / "+v);
    }
    public static void main(String[] args)throws Exception{
        Path dir=Path.of(args.length>0?args[0]:".");Files.createDirectories(dir);var fin=new CompletableFuture<Void>();
        Platform.startup(()->{try{
            Platform.setImplicitExit(false);
            var admin=usuario("administrador");Sesion.iniciar(admin,null);Map<String,VistaPrueba> creadas=new HashMap<>();
            Function<String,Node> factory=id->{try{
                verificar(!creadas.containsKey(id),"Se recreó un módulo: "+id);
                Node vista=switch(id){case "ventas"->venta();case "reportes"->reporte();case "comprobantes"->new ComprobantesView();case "conciertos"->new ConciertosView();default->new Label("Módulo de prueba: "+id);};
                VistaPrueba v=new VistaPrueba(vista);creadas.put(id,v);return v;
            }catch(Exception ex){throw new RuntimeException(ex);}};
            MainView root=new MainView(admin,factory);Stage ventana=new Stage();ventana.setScene(escena(root,1280,720));ventana.show();root.applyCss();root.layout();
            verificar(root.lookupAll(".nav-button").size()==8,"Faltan módulos de administrador");
            foto(ventana.getScene(),dir.resolve("interfaz-ventas.png"));accesible(root,"Cobrar");
            ((ToggleButton)root.lookup("#nav-conciertos")).fire();((ToggleButton)root.lookup("#nav-reportes")).fire();
            verificar(((BorderPane)root.getCenter()).getCenter() instanceof ScrollPane,"No se mostró el módulo");
            verificar(((ToggleButton)root.lookup("#nav-reportes")).isSelected() && !((ToggleButton)root.lookup("#nav-ventas")).isSelected(),"Selección lateral incorrecta");
            foto(ventana.getScene(),dir.resolve("interfaz-reportes.png"));
            ((ToggleButton)root.lookup("#nav-ventas")).fire();
            verificar(creadas.get("ventas").recargas==2,"No se refrescó al regresar");
            var p=(PuntoVentaView)creadas.get("ventas").contenido;
            verificar(((List<?>)campo(p,"carrito")).size()==1,"Se perdió el carrito al navegar");
            verificar(((Label)campo(p,"lblTotal")).getText().contains("1,450"),"Cambió el recargo VIP");
            ((Button)root.lookup("#toggle-menu")).fire();
            ventana.getScene().setRoot(new Pane());ventana.close();ventana=new Stage();ventana.setScene(escena(root,1024,620));ventana.show();root.applyCss();root.layout();
            verificar(ventana.getScene().getWidth()<=1024 && ventana.getScene().getHeight()<=620,"No se aplicó la resolución pequeña");
            foto(ventana.getScene(),dir.resolve("interfaz-compacta.png"));accesible(root,"Cobrar");
            ((ToggleButton)root.lookup("#nav-ventas")).fire();verificar(((ToggleButton)root.lookup("#nav-ventas")).isSelected(),"Se deseleccionó la ruta activa");
            var vendedor=usuario("vendedor");Sesion.iniciar(vendedor,null);
            MainView ventaRoot=new MainView(vendedor,id->new VistaPrueba(new Label("Vista de prueba")));
            ventana.setScene(escena(ventaRoot,1180,700));ventaRoot.applyCss();ventaRoot.layout();
            verificar(ventaRoot.lookupAll(".nav-button").size()==3,"Menú incorrecto para vendedor");
            verificar(ventaRoot.lookup("#nav-usuarios")==null && ventaRoot.lookup("#nav-reportes")==null,"Módulos administrativos visibles para vendedor");
            Field campoStage=FxApp.class.getDeclaredField("stage");campoStage.setAccessible(true);campoStage.set(null,ventana);
            ((Button)ventaRoot.lookup("#logout")).fire();verificar(!Sesion.activa(),"No cerró la sesión");
            verificar(ventana.getScene().getRoot() instanceof LoginView,"No regresó al login");
            var login=(LoginView)ventana.getScene().getRoot();Button ingresar=(Button)login.getChildren().stream().filter(n->n instanceof Button).findFirst().orElseThrow();ingresar.fire();
            verificar(login.getChildren().stream().filter(n->n instanceof Label).map(n->((Label)n).getText()).anyMatch(t->t.equals("Escribe tu usuario y contraseña.")),"Se perdió la validación de login");
            foto(ventana.getScene(),dir.resolve("interfaz-login.png"));ventana.close();
            System.out.println("PASS roles/admin/seller, lazy module navigation, persistent cart and VIP total, compact 1024x620 controls, logout and empty-login validation. Synthetic views; no API calls or sales.");fin.complete(null);
        }catch(Throwable t){fin.completeExceptionally(t);}});
        try{fin.get(60,TimeUnit.SECONDS);}finally{Platform.exit();}
    }
}
