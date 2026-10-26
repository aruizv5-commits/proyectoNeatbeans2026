import com.umg.api.fx.view.ReportesView;
import com.umg.api.config.Sesion;
import com.umg.api.model.usuario;
import com.umg.api.service.ReporteVentasService;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import java.lang.reflect.*;import java.util.concurrent.*;
public class PruebaInterfazReportes {
 static Object campo(Object o,String n)throws Exception {Field f=o.getClass().getDeclaredField(n);f.setAccessible(true);return f.get(o);}
 public static void main(String[] args)throws Exception {
  var datos=PruebaReportes.datos();var fin=new CompletableFuture<Void>();
  Platform.startup(()->{try{
   var u=new usuario();u.setRol("administrador");u.setId_usuario(7);Sesion.iniciar(u,null);
   var v=new ReportesView();Field d=ReportesView.class.getDeclaredField("datos");d.setAccessible(true);d.set(v,datos);
   for(String nombre:new String[]{"concierto","artista","vendedor"}){
    var cb=(ComboBox<ReporteVentasService.Opcion>)campo(v,nombre);
    cb.getItems().add(new ReporteVentasService.Opcion(null,"Todos"));cb.getSelectionModel().selectFirst();
   }
   var aplicar=ReportesView.class.getDeclaredMethod("aplicar");aplicar.setAccessible(true);aplicar.invoke(v);
   var tabla=(TableView<?>)campo(v,"tabla");if(tabla.getItems().size()!=3)throw new AssertionError();
   var cb=(ComboBox<ReporteVentasService.Opcion>)campo(v,"concierto");
   cb.getItems().add(new ReporteVentasService.Opcion(10,"Concierto A"));cb.getSelectionModel().selectLast();
   if(!((Button)campo(v,"pdf")).isDisable() || !tabla.getItems().isEmpty())throw new AssertionError("Stale export remains enabled");
   aplicar.invoke(v);if(tabla.getItems().size()!=2)throw new AssertionError();
   if(!((Label)campo(v,"total")).getText().contains("1,500"))throw new AssertionError();
   var escena=new Scene(v,1200,650);var css=ReportesView.class.getResource("/fx/style.css");if(css!=null)escena.getStylesheets().add(css.toExternalForm());
   Stage stage=new Stage();stage.setScene(escena);stage.show();v.applyCss();v.layout();
   var imagen=escena.snapshot(null);var out=new java.awt.image.BufferedImage((int)imagen.getWidth(),(int)imagen.getHeight(),java.awt.image.BufferedImage.TYPE_INT_ARGB);
   for(int y=0;y<out.getHeight();y++)for(int x=0;x<out.getWidth();x++)out.setRGB(x,y,imagen.getPixelReader().getArgb(x,y));
   if(args.length>0)javax.imageio.ImageIO.write(out,"png",new java.io.File(args[0]));
   stage.close();Sesion.cerrar();System.out.println("PASS actual JavaFX filter controls, selected rows, total and stale-export invalidation. No API writes.");fin.complete(null);
  }catch(Throwable t){fin.completeExceptionally(t);}});
  try{fin.get(30,TimeUnit.SECONDS);}finally{Platform.exit();}
 }
}
