package com.umg.api.service;

import com.fasterxml.jackson.databind.JsonNode;
import java.math.BigDecimal;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

/** Reportes de lectura: importes históricos de detalles, nunca precios actuales. */
public final class ReporteVentasService {
    public record Opcion(Integer id,String nombre) { @Override public String toString(){return nombre;} }
    public record Datos(JsonNode ventas,JsonNode detalles,JsonNode inventarios,JsonNode conciertos,JsonNode artistas,Instant consulta) {}
    public record Linea(int venta,int detalle,String fecha,String evento,String artista,String vendedor,
                        String entrada,int cantidad,BigDecimal precio,BigDecimal subtotal) {}
    public record Reporte(List<Linea> lineas,BigDecimal total,int boletos,int ventas,String filtros,String aviso,Instant consulta) {}

    public Datos cargar()throws Exception {
        Datos datos=new Datos(lista("/ventas"),lista("/detalle_ventas"),lista("/inventario_boletos"),
                lista("/concierto"),lista("/artista"),Instant.now());
        conciertos(datos);artistas(datos);vendedores(datos);return datos;
    }
    private JsonNode lista(String ruta)throws Exception {
        JsonNode n=HttpJson.get(ruta);
        if(n==null || !n.isArray())throw new IllegalArgumentException("La API no devolvió la lista: "+ruta);
        return n;
    }
    private static Map<Integer,JsonNode> indice(JsonNode lista,String campo){
        if(lista==null || !lista.isArray())throw new IllegalArgumentException("Falta una lista de datos.");
        Map<Integer,JsonNode> m=new LinkedHashMap<>();
        for(JsonNode n:lista){int id=clave(n,campo);if(m.put(id,n)!=null)throw new IllegalArgumentException("Identificador duplicado: "+campo+" #"+id);}
        return m;
    }
    private static int clave(JsonNode n,String campo){
        try{int valor=Integer.parseInt(n.path(campo).asText(""));if(valor<0)throw new NumberFormatException();return valor;}
        catch(NumberFormatException ex){throw new IllegalArgumentException("Identificador inválido: "+campo);}
    }
    private static int positivo(JsonNode n,String campo){
        int valor=clave(n,campo);if(valor<1)throw new IllegalArgumentException("Valor inválido: "+campo);return valor;
    }
    private static BigDecimal dinero(JsonNode n,String campo){
        if(!n.hasNonNull(campo))throw new IllegalArgumentException("Falta el importe "+campo);
        try{BigDecimal b=new BigDecimal(n.get(campo).asText());if(b.signum()<0)throw new NumberFormatException();return b;}
        catch(NumberFormatException e){throw new IllegalArgumentException("Importe inválido: "+campo);}
    }
    public static List<Opcion> conciertos(Datos d){return opciones(indice(d.conciertos(),"id_concierto"),"titulo_evento","Concierto");}
    public static List<Opcion> artistas(Datos d){return opciones(indice(d.artistas(),"id_artista"),"nombre_artistico","Artista");}
    public static List<Opcion> vendedores(Datos d){
        Map<Integer,Opcion> m=new TreeMap<>();
        for(JsonNode v:d.ventas()){
            int id=clave(v,"id_vendedor");String nombre=v.path("vendedor_nombre").asText("");
            m.put(id,new Opcion(id,nombre.isBlank()?"Vendedor #"+id:nombre+" (#"+id+")"));
        }
        return List.copyOf(m.values());
    }
    private static List<Opcion> opciones(Map<Integer,JsonNode> datos,String campo,String prefijo){
        List<Opcion> opciones=new ArrayList<>();
        datos.forEach((id,n)->opciones.add(new Opcion(id,n.path(campo).asText(prefijo+" #"+id)+" (#"+id+")")));
        opciones.sort(Comparator.comparing(Opcion::nombre));return List.copyOf(opciones);
    }
    public static Reporte filtrar(Datos d,Integer concierto,Integer artista,Integer vendedor,String etiquetas){
        if(d.detalles()==null || !d.detalles().isArray())throw new IllegalArgumentException("Falta la lista de detalles.");
        Map<Integer,JsonNode> vs=indice(d.ventas(),"id_venta"),ins=indice(d.inventarios(),"id_inventario"),
                cs=indice(d.conciertos(),"id_concierto"),ars=indice(d.artistas(),"id_artista");
        Set<Integer> ids=new HashSet<>(),ventasConDetalle=new HashSet<>(),ventasIncluidas=new HashSet<>();
        List<Linea> filas=new ArrayList<>();BigDecimal total=BigDecimal.ZERO;int boletos=0,referenciasAusentes=0;
        for(JsonNode det:d.detalles()){
            int id=clave(det,"id_detalle");if(!ids.add(id))throw new IllegalArgumentException("Detalle duplicado #"+id);
            int idVenta=clave(det,"id_venta");JsonNode venta=vs.get(idVenta);
            if(venta==null)throw new IllegalArgumentException("El detalle #"+id+" referencia una venta ausente. Recarga los datos.");
            ventasConDetalle.add(idVenta);int idVendedor=clave(venta,"id_vendedor");
            if(vendedor!=null && vendedor!=idVendedor)continue;
            JsonNode inv=ins.get(clave(det,"id_inventario"));
            if(inv==null)throw new IllegalArgumentException("Falta el inventario del detalle #"+id+"; no se puede asignar su concierto.");
            int idConcierto=clave(inv,"id_concierto");if(concierto!=null && concierto!=idConcierto)continue;
            JsonNode c=cs.get(idConcierto);Integer idArtista=c==null?null:clave(c,"id_artista");
            if(artista!=null && c==null)throw new IllegalArgumentException("No se puede determinar el artista de un concierto eliminado. Revisa el catálogo.");
            if(artista!=null && !artista.equals(idArtista))continue;
            JsonNode ar=idArtista==null?null:ars.get(idArtista);
            if(c==null || ar==null)referenciasAusentes++;
            int cantidad=positivo(det,"cantidad");BigDecimal precio=dinero(det,"precio_unitario"),sub=dinero(det,"subtotal");
            if(precio.multiply(BigDecimal.valueOf(cantidad)).compareTo(sub)!=0)
                throw new IllegalArgumentException("El detalle #"+id+" tiene importes inconsistentes.");
            String fecha;
            try{fecha=OffsetDateTime.parse(venta.path("fecha_venta").asText()).atZoneSameInstant(ZoneId.of("America/Guatemala"))
                    .format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));}
            catch(Exception e){throw new IllegalArgumentException("Fecha inválida en venta #"+idVenta);}
            String nombre=venta.path("vendedor_nombre").asText("Vendedor #"+idVendedor);
            filas.add(new Linea(idVenta,id,fecha,c==null?"Concierto #"+idConcierto:c.path("titulo_evento").asText(),
                    ar==null?"Artista no disponible":ar.path("nombre_artistico").asText(),nombre,
                    det.path("asiento").asText("Inventario #"+det.path("id_inventario").asText()),cantidad,precio,sub));
            total=total.add(sub);boletos=Math.addExact(boletos,cantidad);ventasIncluidas.add(idVenta);
        }
        filas.sort(Comparator.comparingInt(Linea::venta).thenComparingInt(Linea::detalle));
        long sinDetalles=vs.keySet().stream().filter(id->!ventasConDetalle.contains(id))
                .filter(id->vendedor==null || vs.get(id).path("id_vendedor").asInt()==vendedor).count();
        String aviso="Totales de los detalles incluidos; una venta puede contener varios conciertos. "
                +"Ventas sin detalles en la consulta"+(vendedor!=null?" para este vendedor":"")+": "+sinDetalles+". "
                +(referenciasAusentes>0?"Hay "+referenciasAusentes+" detalle(s) con nombres de catálogo no disponibles. ":"")
                +"Los nombres pertenecen al catálogo actual.";
        return new Reporte(List.copyOf(filas),total,boletos,ventasIncluidas.size(),etiquetas,aviso,d.consulta());
    }
}
