import com.fasterxml.jackson.databind.*;
import com.umg.api.service.ReporteVentasService;
import com.umg.api.reportes.ReporteVentasPdf;
import java.math.*;import java.nio.file.*;import java.time.*;import java.util.*;
public class PruebaReportes {
 public static ReporteVentasService.Datos datos()throws Exception {
  var m=new ObjectMapper();
  return new ReporteVentasService.Datos(
   m.readTree("""
   [{"id_venta":1,"id_vendedor":7,"fecha_venta":"2026-10-25T20:00:00Z","total_venta":3000},
    {"id_venta":2,"id_vendedor":8,"fecha_venta":"2026-10-25T21:00:00Z","total_venta":500},
    {"id_venta":3,"id_vendedor":7,"fecha_venta":"2026-10-25T22:00:00Z","total_venta":0}]
   """),
   m.readTree("""
   [{"id_detalle":1,"id_venta":1,"id_inventario":1,"cantidad":1,"precio_unitario":1000,"subtotal":1000,"asiento":"A-1"},
    {"id_detalle":2,"id_venta":1,"id_inventario":2,"cantidad":1,"precio_unitario":2000,"subtotal":2000,"asiento":"B-1"},
    {"id_detalle":3,"id_venta":2,"id_inventario":3,"cantidad":1,"precio_unitario":500,"subtotal":500,"asiento":"A-2"}]
   """),
   m.readTree("""
   [{"id_inventario":1,"id_concierto":10,"precio":99999},{"id_inventario":2,"id_concierto":20,"precio":99999},{"id_inventario":3,"id_concierto":10,"precio":99999}]
   """),
   m.readTree("""
   [{"id_concierto":10,"id_artista":100,"titulo_evento":"Concierto de muestra A"},{"id_concierto":20,"id_artista":200,"titulo_evento":"Concierto de muestra B"}]
   """),
   m.readTree("""
   [{"id_artista":100,"nombre_artistico":"Artista de ejemplo A"},{"id_artista":200,"nombre_artistico":"Artista de ejemplo B"}]
   """),Instant.parse("2026-10-25T22:00:00Z"));
 }
 static void igual(Object a,Object b){if(!Objects.equals(a,b))throw new AssertionError(a+" != "+b);}
 static void monto(ReporteVentasService.Reporte r,String n){if(r.total().compareTo(new BigDecimal(n))!=0)throw new AssertionError(r.total());}
 public static void main(String[] args)throws Exception {
  var d=datos();
  var todos=ReporteVentasService.filtrar(d,null,null,null,"Todos los conciertos · Todos los artistas · Todos los vendedores");
  igual(todos.lineas().get(0).fecha(),"25/10/2026 14:00");
  monto(todos,"3500");igual(todos.boletos(),3);igual(todos.ventas(),2);if(!todos.aviso().contains(": 1."))throw new AssertionError(todos.aviso());
  var c=ReporteVentasService.filtrar(d,10,null,null,"Concierto de muestra A · Todos los artistas · Todos los vendedores");monto(c,"1500");igual(c.ventas(),2);
  monto(ReporteVentasService.filtrar(d,null,200,null,"Artista B"),"2000");
  monto(ReporteVentasService.filtrar(d,null,null,7,"Vendedor 7"),"3000");
  monto(ReporteVentasService.filtrar(d,10,100,7,"Filtros combinados"),"1000");
  var vacio=ReporteVentasService.filtrar(d,10,200,null,"Sin coincidencias");monto(vacio,"0");igual(vacio.lineas().size(),0);
  var cero=new ObjectMapper().readTree("[{\"id_artista\":0,\"nombre_artistico\":\"Artista cero\"}]");
  var conCero=new ReporteVentasService.Datos(d.ventas(),d.detalles(),d.inventarios(),d.conciertos(),cero,d.consulta());
  igual(ReporteVentasService.artistas(conCero).get(0).id(),0);
  monto(ReporteVentasService.filtrar(conCero,null,null,null,"Artista cero en catálogo"),"3500");
  var dup=d.detalles().deepCopy();((com.fasterxml.jackson.databind.node.ArrayNode)dup).add(dup.get(0));
  try{ReporteVentasService.filtrar(new ReporteVentasService.Datos(d.ventas(),dup,d.inventarios(),d.conciertos(),d.artistas(),d.consulta()),null,null,null,"");throw new AssertionError("Duplicate accepted");}catch(IllegalArgumentException expected){}
  var roto=d.inventarios().deepCopy();((com.fasterxml.jackson.databind.node.ArrayNode)roto).remove(0);
  try{ReporteVentasService.filtrar(new ReporteVentasService.Datos(d.ventas(),d.detalles(),roto,d.conciertos(),d.artistas(),d.consulta()),null,null,null,"");throw new AssertionError("Missing inventory accepted");}catch(IllegalArgumentException expected){}
  var dir=Path.of(args.length>0?args[0]:".");Files.createDirectories(dir);
  ReporteVentasPdf.exportar(c,dir.resolve("reporte-ventas-ejemplo.pdf"));
  ReporteVentasPdf.exportar(vacio,dir.resolve("reporte-sin-resultados.pdf"));
  var muchas=new ArrayList<ReporteVentasService.Linea>();for(int i=0;i<70;i++)muchas.add(todos.lineas().get(i%3));
  ReporteVentasPdf.exportar(new ReporteVentasService.Reporte(muchas,new BigDecimal("81500"),70,2,"Prueba de paginación",todos.aviso(),d.consulta()),dir.resolve("reporte-paginado.pdf"));
  System.out.println("PASS concert/artist/seller/combined filters, mixed-event sale allocation, historical prices, empty results, duplicate/orphan guards and PDF exports. Synthetic data only.");
 }
}
