package com.umg.api.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.umg.api.config.Sesion;
import java.math.BigDecimal;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class ComprobanteService {
    public static class Documento {
        private final int numero;
        private final BigDecimal total;
        private final List<Map<String,Object>> lineas;
        private final String fecha, vendedor, cliente, telefono, observaciones;
        public Documento(int numero, BigDecimal total, List<Map<String,Object>> lineas) {
            this(numero,total,lineas,"No disponible","No disponible","No disponible","No disponible","Sin observaciones.");
        }
        public Documento(int numero, BigDecimal total, List<Map<String,Object>> lineas,
                String fecha, String vendedor, String cliente, String telefono, String observaciones) {
            this.numero=numero;this.total=total;this.lineas=lineas;
            this.fecha=fecha;this.vendedor=vendedor;this.cliente=cliente;this.telefono=telefono;this.observaciones=observaciones;
        }
        public int numero(){return numero;}
        public BigDecimal total(){return total;}
        public List<Map<String,Object>> lineas(){return lineas;}
        public String fecha(){return fecha;}
        public String vendedor(){return vendedor;}
        public String cliente(){return cliente;}
        public String telefono(){return telefono;}
        public String observaciones(){return observaciones;}
    }
    public Documento cargar(int id,Integer vendedorPermitido)throws Exception {
        if(id<1)throw new IllegalArgumentException("Número de venta inválido.");
        JsonNode venta=consultar("/ventas/"+id);
        if(entero(venta,"id_venta")!=id)throw new IllegalArgumentException("La API devolvió otra venta.");
        int idVendedor=entero(venta,"id_vendedor");
        if(vendedorPermitido!=null && idVendedor!=vendedorPermitido)
            throw new SecurityException("Solo puedes consultar tus propias ventas.");
        JsonNode detalles=HttpJson.get("/detalle_ventas");
        if(detalles==null || !detalles.isArray())throw new IllegalArgumentException("La API no devolvió los detalles.");
        List<Map<String,Object>> filas=new ArrayList<>();
        Set<Integer> clientes=new HashSet<>();
        Map<String,JsonNode> cache=new HashMap<>();
        for(JsonNode d:detalles){
            if(d.path("id_venta").asInt(-1)!=id)continue;
            int invId=entero(d,"id_inventario");
            JsonNode inv=catalogo("/inventario_boletos/"+invId,cache);
            JsonNode concierto=catalogo("/concierto/"+entero(inv,"id_concierto"),cache);
            JsonNode localidad=catalogo("/localidades/"+entero(inv,"id_localidad"),cache);
            Map<String,Object> f=new HashMap<>();
            f.put("evento",concierto.path("titulo_evento").asText("Evento sin nombre"));
            String fecha=concierto.path("fecha_concierto").asText("");
            f.put("fecha_evento",fecha.length()>=10?LocalDate.parse(fecha.substring(0,10)).format(DateTimeFormatter.ofPattern("dd/MM/yyyy")):"No disponible");
            f.put("recinto",concierto.path("recinto").asText("No disponible"));
            String tipo=localidad.path("nombre").asText("").trim();
            if(tipo.equalsIgnoreCase("plata"))tipo="Platea";
            f.put("tipo",tipo);
            String asiento=d.path("asiento").asText(localidad.path("asiento").asText(""));
            f.put("asiento",asiento);f.put("localidad",tipo+" "+asiento);
            f.put("precio",importe(d,"precio_unitario"));f.put("subtotal",importe(d,"subtotal"));
            f.put("cantidad",entero(d,"cantidad"));f.put("id_detalle",entero(d,"id_detalle"));
            f.put("id_inventario",invId);f.put("qr_url",d.path("qr_url").asText(""));
            filas.add(f);clientes.add(entero(d,"id_cliente"));
        }
        if(filas.isEmpty())throw new IllegalArgumentException("La venta #"+id+" no tiene detalles.");
        if(clientes.size()!=1)throw new IllegalArgumentException("La venta no tiene un cliente único.");
        JsonNode cliente=consultar("/cliente/"+clientes.iterator().next());
        String vendedor=venta.path("vendedor_nombre").asText("").trim();
        if(vendedor.isEmpty() && Sesion.activa() && Sesion.actual().getId_usuario()==idVendedor){
            vendedor=Sesion.actual().getNombre_completo();
            if(vendedor==null || vendedor.isBlank())vendedor=Sesion.actual().getNombre_usuario();
        }
        if(vendedor==null || vendedor.isBlank())vendedor="Vendedor #"+idVendedor;
        String fecha=OffsetDateTime.parse(venta.path("fecha_venta").asText()).atZoneSameInstant(ZoneId.of("America/Guatemala"))
                .format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
        String nombre=(cliente.path("nombre").asText("")+" "+cliente.path("apellido").asText("")).trim();
        return new Documento(id,importe(venta,"total_venta"),filas,fecha,vendedor,nombre,
                cliente.path("telefono").asText("No registrado"),venta.path("observaciones").asText("Precios finales registrados en la venta."));
    }
    private JsonNode consultar(String ruta)throws Exception {
        JsonNode n=HttpJson.get(ruta);
        if(n==null || !n.isObject() || n.isEmpty())throw new IllegalArgumentException("No se encontró: "+ruta);
        return n;
    }
    private JsonNode catalogo(String ruta,Map<String,JsonNode> cache)throws Exception {
        if(!cache.containsKey(ruta))cache.put(ruta,consultar(ruta));return cache.get(ruta);
    }
    private int entero(JsonNode n,String campo){
        int valor=n.path(campo).asInt(-1);
        if(valor<1)throw new IllegalArgumentException("Falta un valor válido: "+campo);return valor;
    }
    private BigDecimal importe(JsonNode n,String campo){
        if(!n.hasNonNull(campo))throw new IllegalArgumentException("Falta el importe "+campo);
        try{return new BigDecimal(n.get(campo).asText());}
        catch(NumberFormatException e){throw new IllegalArgumentException("Importe inválido: "+campo);}
    }
}
