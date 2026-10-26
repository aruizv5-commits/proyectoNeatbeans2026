import com.umg.api.service.ComprobanteService;
import com.umg.api.reportes.BoletosPdf;
import java.math.BigDecimal;import java.nio.file.*;import java.util.*;
public class PruebaJava {
 public static void main(String[] args)throws Exception {
  List<Map<String,Object>> filas=new ArrayList<>();
  for(int i=0;i<2;i++){
   Map<String,Object> f=new HashMap<>();f.put("evento","Todos somos ángeles");f.put("fecha_evento","06/12/2026");f.put("recinto","Recinto de ejemplo");
   f.put("tipo",i==0?"General":"VIP");f.put("asiento",i==0?"J-10":"A-1");f.put("localidad",f.get("tipo")+" "+f.get("asiento"));
   var precio=new BigDecimal(i==0?"500":"725");f.put("precio",precio);f.put("subtotal",precio);f.put("cantidad",1);f.put("id_detalle",21+i);f.put("qr_url","");filas.add(f);
  }
  var venta=new ComprobanteService.Documento(8,new BigDecimal("1225"),filas,"25/10/2026 14:00","Vendedor ejemplo","Cliente ejemplo","0000-0000","MUESTRA: datos ficticios.");
  BoletosPdf.exportar(venta,Path.of("comprobante-ejemplo.pdf"));
  assert BoletosPdf.enlace(filas.get(0)).equals("https://proyectoapi2026.onrender.com/api/detalle_ventas/21");
  try{var mal=new HashMap<>(filas.get(0));mal.put("qr_url","https://otro.example/");BoletosPdf.enlace(mal);throw new AssertionError();}catch(IllegalArgumentException expected){}
  System.out.println("PASS: receipt + two tickets, real API fallback URL, external QR host rejected.");
 }
}
