package com.umg.api.reportes;

import com.google.zxing.*;
import com.google.zxing.qrcode.QRCodeWriter;
import com.umg.api.config.Sesion;
import com.umg.api.fx.util.Async;
import com.umg.api.fx.util.Ui;
import com.umg.api.service.ComprobanteService;
import javafx.scene.control.TextInputDialog;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.design.*;
import net.sf.jasperreports.engine.type.*;
import java.awt.Color;
import net.sf.jasperreports.engine.data.JRMapCollectionDataSource;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

public final class BoletosPdf {
    private BoletosPdf() {}
    public static String enlace(Map<String,Object> fila) {
        URI base=URI.create(System.getProperty("boletos.public.url","https://proyectoapi2026.onrender.com"));
        String valor=String.valueOf(fila.getOrDefault("qr_url",""));
        // La ruta de detalle ya existe. La ruta visual se utiliza cuando la API entrega qr_url.
        if(valor.isBlank())valor=base.resolve("/api/detalle_ventas/"+fila.get("id_detalle")).toString();
        URI url=URI.create(valor);
        if(!"https".equalsIgnoreCase(url.getScheme()) || url.getHost()==null
                || !url.getHost().equalsIgnoreCase(base.getHost()) || url.getUserInfo()!=null)
            throw new IllegalArgumentException("La API devolvió un enlace QR inválido.");
        return url.toString();
    }
    public static BufferedImage qr(String contenido)throws Exception {
        var matriz=new QRCodeWriter().encode(contenido,BarcodeFormat.QR_CODE,480,480,
                Map.of(EncodeHintType.MARGIN,4,EncodeHintType.CHARACTER_SET,"UTF-8"));
        var imagen=new BufferedImage(480,480,BufferedImage.TYPE_INT_RGB);
        for(int y=0;y<480;y++)for(int x=0;x<480;x++)imagen.setRGB(x,y,matriz.get(x,y)?0:0xffffff);
        return imagen;
    }
    private static JasperPrint llenar(JasperDesign diseno,Map<String,Object> parametros,List<Map<String,Object>> filas)throws Exception {
        var reporte=JasperCompileManager.compileReport(diseno);
        List<Map<String,?>> datos=new ArrayList<>(filas);
        return JasperFillManager.fillReport(reporte,parametros,new JRMapCollectionDataSource(datos));
    }
    public static void exportar(ComprobanteService.Documento venta,Path destino)throws Exception {
        if(venta.lineas().isEmpty())throw new IllegalArgumentException("La venta no contiene boletos.");
        BigDecimal suma=BigDecimal.ZERO;
        int cantidadTotal=0;
        List<Map<String,Object>> tickets=new ArrayList<>();
        Set<Integer> ids=new HashSet<>();
        for(var f:venta.lineas()){
            int cantidad=((Number)f.get("cantidad")).intValue();
            BigDecimal precio=(BigDecimal)f.get("precio"), subtotal=(BigDecimal)f.get("subtotal");
            if(cantidad!=1 || precio.signum()<0 || precio.multiply(BigDecimal.valueOf(cantidad)).compareTo(subtotal)!=0)
                throw new IllegalArgumentException("Esta API vende un asiento por detalle. Revisa cantidades e importes.");
            int id=((Number)f.get("id_detalle")).intValue();
            if(id<1 || !ids.add(id))throw new IllegalArgumentException("Identificador de boleto inválido o duplicado.");
            suma=suma.add(subtotal);cantidadTotal+=cantidad;
            var ticket=new HashMap<String,Object>(f);
            ticket.put("qr",qr(enlace(f)));ticket.put("referencia","Boleto #"+id+" · Venta #"+venta.numero());
            ticket.put("nota",String.valueOf(f.getOrDefault("qr_url","")).isBlank()
                    ?"El QR abre el registro del boleto en la API. Página visual pendiente de activar."
                    :"Escanea el QR para consultar el boleto registrado. No registra el ingreso al evento.");
            tickets.add(ticket);
        }
        if(suma.compareTo(venta.total())!=0)throw new IllegalArgumentException("El total no coincide con los detalles.");
        Map<String,Object> p=new HashMap<>();
        p.put("numero",String.valueOf(venta.numero()));p.put("fecha",venta.fecha());p.put("cliente",venta.cliente());
        p.put("telefono",venta.telefono());p.put("vendedor",venta.vendedor());p.put("observaciones",venta.observaciones());
        p.put("cantidad",cantidadTotal);p.put("total",venta.total());
        JasperPrint documento=llenar(comprobante(),p,venta.lineas());
        JasperPrint boletos=llenar(boleto(),new HashMap<>(),tickets);
        for(var pagina:boletos.getPages())documento.addPage(pagina);
        Path archivo=destino.toAbsolutePath();
        Path temporal=Files.createTempFile(archivo.getParent(),".comprobante-",".pdf");
        try {JasperExportManager.exportReportToPdfFile(documento,temporal.toString());
            Files.move(temporal,archivo,StandardCopyOption.REPLACE_EXISTING);
        }finally{Files.deleteIfExists(temporal);}
    }
    public static void guardar(Window ventana) {
        if (!Sesion.activa()) { Ui.error("Inicia sesión primero."); return; }
        TextInputDialog pregunta = new TextInputDialog();
        pregunta.initOwner(ventana);
        pregunta.setTitle("Boletos con QR");
        pregunta.setHeaderText("Número de una venta ya registrada");
        var entrada = pregunta.showAndWait();
        if (entrada.isEmpty()) return;
        final int id;
        try {
            id = Integer.parseInt(entrada.get().trim());
            if (id < 1) throw new NumberFormatException();
        } catch (NumberFormatException ex) { Ui.error("Número de venta inválido."); return; }
        Integer vendedor = Sesion.esAdmin() ? null : Sesion.actual().getId_usuario();
        FileChooser selector = new FileChooser();
        selector.setInitialFileName("boletos-venta-" + id + ".pdf");
        selector.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF", "*.pdf"));
        var archivo = selector.showSaveDialog(ventana);
        if (archivo == null) return;
        Path destino = archivo.toPath();
        if (!archivo.getName().toLowerCase(Locale.ROOT).endsWith(".pdf"))
            destino = Path.of(archivo.getAbsolutePath() + ".pdf");
        if (Files.exists(destino) && !Ui.confirmar("¿Reemplazar " + destino.getFileName() + "?")) return;
        final Path salida = destino;
        Async.run(() -> {
            exportar(new ComprobanteService().cargar(id, vendedor), salida);
            return salida;
        }, p -> Ui.info("Boletos guardados en: " + p), Ui::error);
    }

    private static JasperDesign base(String nombre)throws Exception {
        JasperDesign d=new JasperDesign();d.setName(nombre);d.setPageWidth(595);d.setPageHeight(842);
        d.setLeftMargin(40);d.setRightMargin(40);d.setTopMargin(35);d.setBottomMargin(35);d.setColumnWidth(515);
        JRDesignStyle estilo=new JRDesignStyle();estilo.setName("Normal");estilo.setDefault(true);
        estilo.setFontName("DejaVu Sans");estilo.setFontSize(10f);estilo.setForecolor(Color.decode("#243247"));d.addStyle(estilo);
        return d;
    }
    private static JRDesignBand banda(int alto){JRDesignBand b=new JRDesignBand();b.setHeight(alto);return b;}
    private static void dato(JasperDesign d,String nombre,Class<?> tipo,boolean parametro)throws Exception {
        if(parametro){JRDesignParameter p=new JRDesignParameter();p.setName(nombre);p.setValueClass(tipo);d.addParameter(p);}
        else{JRDesignField f=new JRDesignField();f.setName(nombre);f.setValueClass(tipo);d.addField(f);}
    }
    private static void caja(JRDesignBand b,int x,int y,int w,int h,String color){
        JRDesignRectangle r=new JRDesignRectangle();r.setX(x);r.setY(y);r.setWidth(w);r.setHeight(h);r.setRadius(8);
        r.setMode(ModeEnum.OPAQUE);r.setBackcolor(Color.decode(color));r.getLinePen().setLineWidth(0f);b.addElement(r);
    }
    private static void texto(JRDesignBand b,int x,int y,int w,int h,String expresion,int tam,boolean negrita,String color,String patron){
        JRDesignTextField t=new JRDesignTextField();t.setX(x);t.setY(y);t.setWidth(w);t.setHeight(h);
        t.setExpression(new JRDesignExpression(expresion));t.setFontSize((float)tam);t.setBold(negrita);
        t.setForecolor(Color.decode(color));t.setStretchWithOverflow(true);if(patron!=null)t.setPattern(patron);b.addElement(t);
    }
    private static void etiqueta(JRDesignBand b,int x,int y,int w,int h,String valor,int tam,boolean negrita,String color){
        texto(b,x,y,w,h,"\""+valor+"\"",tam,negrita,color,null);
    }
    private static JasperDesign comprobante()throws Exception {
        JasperDesign d=base("comprobante");
        for(String n:List.of("numero","fecha","vendedor","cliente","telefono","observaciones"))dato(d,n,String.class,true);
        dato(d,"cantidad",Integer.class,true);dato(d,"total",BigDecimal.class,true);
        for(String n:List.of("evento","tipo"))dato(d,n,String.class,false);
        dato(d,"cantidad",Integer.class,false);dato(d,"precio",BigDecimal.class,false);dato(d,"subtotal",BigDecimal.class,false);
        JRDesignBand t=banda(290);caja(t,0,0,515,100,"#142438");caja(t,0,95,515,5,"#14B8A6");
        etiqueta(t,20,16,470,18,"BOLETERÍA · CONCIERTOS Y EVENTOS",10,true,"#8BE3D8");
        etiqueta(t,20,40,360,35,"Comprobante de venta",23,true,"#FFFFFF");
        texto(t,390,47,105,25,"\"N.º \" + $P{numero}",14,true,"#FFFFFF",null);
        etiqueta(t,0,120,260,18,"FECHA DE VENTA",8,true,"#65758A");texto(t,0,140,260,25,"$P{fecha}",11,false,"#243247",null);
        etiqueta(t,285,120,230,18,"VENDEDOR",8,true,"#65758A");texto(t,285,140,230,30,"$P{vendedor}",11,false,"#243247",null);
        caja(t,0,180,515,85,"#F0F5F8");etiqueta(t,16,194,300,18,"CLIENTE",8,true,"#65758A");
        texto(t,16,215,300,32,"$P{cliente}",12,true,"#243247",null);
        etiqueta(t,335,194,165,18,"TELÉFONO",8,true,"#65758A");texto(t,335,215,165,25,"$P{telefono}",11,false,"#243247",null);d.setTitle(t);
        JRDesignBand h=banda(36);caja(h,0,0,515,32,"#E7EEF3");
        int[] xs={10,220,310,355,440},ws={205,85,40,75,75};String[] nombres={"EVENTO / CONCIERTO","ENTRADA","CANT.","PRECIO Q","TOTAL Q"};
        for(int i=0;i<5;i++)etiqueta(h,xs[i],10,ws[i],18,nombres[i],8,true,"#243247");d.setColumnHeader(h);
        JRDesignBand fila=banda(62);String[] campos={"evento","tipo","cantidad","precio","subtotal"};
        for(int i=0;i<5;i++)texto(fila,xs[i],9,ws[i],45,"$F{"+campos[i]+"}",9,i==4,"#243247",i>=3?"#,##0.00":null);
        ((JRDesignSection)d.getDetailSection()).addBand(fila);
        JRDesignBand resumen=banda(210);caja(resumen,0,15,515,76,"#142438");
        etiqueta(resumen,18,31,240,18,"TOTAL PAGADO",10,true,"#8BE3D8");
        texto(resumen,18,55,260,22,"$P{cantidad} + \" boleto(s)\"",11,false,"#FFFFFF",null);
        texto(resumen,300,38,195,40,"$P{total}",26,true,"#FFFFFF","'Q' #,##0.00");
        etiqueta(resumen,0,113,515,18,"OBSERVACIONES",8,true,"#65758A");texto(resumen,0,137,515,54,"$P{observaciones}",10,false,"#243247",null);d.setSummary(resumen);
        JRDesignBand pie=banda(35);etiqueta(pie,0,10,420,20,"Comprobante académico · Conserva este documento",8,false,"#65758A");
        texto(pie,460,10,55,20,"\"Pág. \" + $V{PAGE_NUMBER}",8,false,"#65758A",null);d.setPageFooter(pie);return d;
    }
    private static JasperDesign boleto()throws Exception {
        JasperDesign d=base("boletos");
        for(String n:List.of("evento","fecha_evento","recinto","localidad","referencia","nota"))dato(d,n,String.class,false);
        dato(d,"precio",BigDecimal.class,false);dato(d,"qr",java.awt.Image.class,false);
        JRDesignBand b=banda(740);b.setSplitType(SplitTypeEnum.PREVENT);caja(b,0,0,515,100,"#142438");
        etiqueta(b,20,18,475,20,"BOLETERÍA · ENTRADA AL EVENTO",11,true,"#8BE3D8");
        texto(b,20,45,475,45,"$F{evento}",19,true,"#FFFFFF",null);
        texto(b,0,125,515,28,"\"Fecha del evento: \" + $F{fecha_evento}",12,false,"#142438",null);
        texto(b,0,165,515,35,"\"Recinto: \" + $F{recinto}",12,false,"#142438",null);
        texto(b,0,210,515,30,"\"Entrada / asiento: \" + $F{localidad}",12,false,"#142438",null);
        texto(b,0,250,515,30,"$F{precio}",12,true,"#142438","'Precio por boleto: Q' #,##0.00");
        JRDesignImage qr=new JRDesignImage(d);qr.setX(132);qr.setY(310);qr.setWidth(250);qr.setHeight(250);
        qr.setScaleImage(ScaleImageEnum.RETAIN_SHAPE);qr.setExpression(new JRDesignExpression("$F{qr}"));b.addElement(qr);
        texto(b,0,590,515,25,"$F{referencia}",11,true,"#142438",null);
        texto(b,0,640,515,60,"$F{nota}",10,false,"#65758A",null);
        ((JRDesignSection)d.getDetailSection()).addBand(b);return d;
    }
}
