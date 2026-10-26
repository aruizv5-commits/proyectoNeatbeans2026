package com.umg.api.reportes;

import com.umg.api.service.ReporteVentasService;
import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.data.JRMapCollectionDataSource;
import net.sf.jasperreports.engine.design.*;
import net.sf.jasperreports.engine.type.*;
import java.awt.Color;
import java.math.BigDecimal;
import java.nio.file.*;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

public final class ReporteVentasPdf {
    private ReporteVentasPdf(){}
    public static void exportar(ReporteVentasService.Reporte reporte,Path destino)throws Exception {
        List<Map<String,?>> filas=new ArrayList<>();
        for(var l:reporte.lineas()){
            Map<String,Object> f=new HashMap<>();f.put("venta",l.venta());f.put("fecha",l.fecha().replace(" ","\n"));
            f.put("evento",l.evento());f.put("artista",l.artista());f.put("vendedor",l.vendedor());f.put("entrada",l.entrada());
            f.put("cantidad",l.cantidad());f.put("precio",l.precio());f.put("subtotal",l.subtotal());filas.add(f);
        }
        Map<String,Object> parametros=new HashMap<>();parametros.put("filtros",reporte.filtros());parametros.put("aviso",reporte.aviso());
        parametros.put("consulta",reporte.consulta().atZone(ZoneId.of("America/Guatemala")).format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
        parametros.put("total",reporte.total());parametros.put("boletos",reporte.boletos());parametros.put("ventas",reporte.ventas());
        var documento=JasperFillManager.fillReport(JasperCompileManager.compileReport(diseno()),parametros,new JRMapCollectionDataSource(filas));
        Path archivo=destino.toAbsolutePath(),temporal=Files.createTempFile(archivo.getParent(),".reporte-",".pdf");
        try{JasperExportManager.exportReportToPdfFile(documento,temporal.toString());Files.move(temporal,archivo,StandardCopyOption.REPLACE_EXISTING);}
        finally{Files.deleteIfExists(temporal);}
    }
    private static JasperDesign diseno()throws Exception {
        JasperDesign d=new JasperDesign();d.setName("reporte_ventas");d.setPageWidth(842);d.setPageHeight(595);
        d.setLeftMargin(40);d.setRightMargin(40);d.setTopMargin(30);d.setBottomMargin(25);d.setColumnWidth(762);
        d.setWhenNoDataType(WhenNoDataTypeEnum.ALL_SECTIONS_NO_DETAIL);
        JRDesignStyle s=new JRDesignStyle();s.setName("Normal");s.setDefault(true);s.setFontName("DejaVu Sans");s.setFontSize(9f);d.addStyle(s);
        for(String n:List.of("filtros","aviso","consulta"))parametro(d,n,String.class);
        parametro(d,"total",BigDecimal.class);parametro(d,"boletos",Integer.class);parametro(d,"ventas",Integer.class);
        for(String n:List.of("fecha","evento","artista","vendedor","entrada"))campo(d,n,String.class);
        campo(d,"venta",Integer.class);campo(d,"cantidad",Integer.class);campo(d,"precio",BigDecimal.class);campo(d,"subtotal",BigDecimal.class);
        JRDesignBand titulo=banda(135);caja(titulo,0,0,762,62,"#142438");
        texto(titulo,18,13,720,35,"\"Reporte de ventas\"",23,true,"#FFFFFF",null);
        texto(titulo,0,75,762,32,"$P{filtros}",10,false,"#142438",null);
        texto(titulo,0,111,762,20,"\"Datos consultados: \" + $P{consulta} + \" · Hora de Guatemala\"",9,false,"#65758A",null);d.setTitle(titulo);
        int[] x={0,42,132,290,405,500,562,602,677},w={40,88,156,113,93,60,38,73,85};
        String[] nombres={"Venta","Fecha","Evento / concierto","Artista","Vendedor","Asiento","Cant.","Precio Q","Importe Q"};
        String[] campos={"venta","fecha","evento","artista","vendedor","entrada","cantidad","precio","subtotal"};
        JRDesignBand cabecera=banda(28);caja(cabecera,0,0,762,26,"#E7EEF3");
        for(int i=0;i<x.length;i++)texto(cabecera,x[i]+3,7,w[i]-5,18,"\""+nombres[i]+"\"",8,true,"#142438",null);d.setColumnHeader(cabecera);
        JRDesignBand fila=banda(56);fila.setSplitType(SplitTypeEnum.STRETCH);
        for(int i=0;i<x.length;i++)texto(fila,x[i]+3,5,w[i]-5,48,"$F{"+campos[i]+"}",8,i==8,"#243247",i>=7?"#,##0.00":null);
        ((JRDesignSection)d.getDetailSection()).addBand(fila);
        JRDesignBand resumen=banda(80);caja(resumen,0,10,762,58,"#142438");
        texto(resumen,18,22,390,30,"$P{ventas} + \" venta(s) · \" + $P{boletos} + \" boleto(s) incluidos\"",12,true,"#8BE3D8",null);
        texto(resumen,480,22,264,32,"$P{total}",22,true,"#FFFFFF","'Total Q' #,##0.00");d.setSummary(resumen);
        JRDesignBand pie=banda(45);texto(pie,0,0,690,40,"$P{aviso}",7,false,"#65758A",null);
        texto(pie,706,10,56,20,"\"Pág. \" + $V{PAGE_NUMBER}",8,false,"#65758A",null);d.setPageFooter(pie);return d;
    }
    private static JRDesignBand banda(int h){JRDesignBand b=new JRDesignBand();b.setHeight(h);return b;}
    private static void parametro(JasperDesign d,String n,Class<?> c)throws Exception{JRDesignParameter p=new JRDesignParameter();p.setName(n);p.setValueClass(c);d.addParameter(p);}
    private static void campo(JasperDesign d,String n,Class<?> c)throws Exception{JRDesignField f=new JRDesignField();f.setName(n);f.setValueClass(c);d.addField(f);}
    private static void caja(JRDesignBand b,int x,int y,int w,int h,String color){JRDesignRectangle r=new JRDesignRectangle();r.setX(x);r.setY(y);r.setWidth(w);r.setHeight(h);r.setMode(ModeEnum.OPAQUE);r.setBackcolor(Color.decode(color));r.getLinePen().setLineWidth(0f);b.addElement(r);}
    private static void texto(JRDesignBand b,int x,int y,int w,int h,String expr,int tam,boolean bold,String color,String patron){
        JRDesignTextField t=new JRDesignTextField();t.setX(x);t.setY(y);t.setWidth(w);t.setHeight(h);t.setFontSize((float)tam);t.setBold(bold);
        t.setForecolor(Color.decode(color));t.setExpression(new JRDesignExpression(expr));t.setStretchWithOverflow(true);if(patron!=null)t.setPattern(patron);b.addElement(t);
    }
}
