package com.umg.api.fx.view;

import com.umg.api.config.Sesion;
import com.umg.api.fx.util.Async;
import com.umg.api.fx.util.Ui;
import com.umg.api.reportes.ReporteVentasPdf;
import com.umg.api.service.ReporteVentasService;
import com.umg.api.service.ReporteVentasService.Opcion;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import java.nio.file.*;
import java.util.*;

/** Módulo de consulta para administración; no escribe en la API. */
public class ReportesView extends VBox implements Recargable {
    private final ComboBox<Opcion> concierto=new ComboBox<>(),artista=new ComboBox<>(),vendedor=new ComboBox<>();
    private final Button aplicar=new Button("Aplicar filtros"),recargar=new Button("Actualizar datos"),pdf=new Button("Guardar reporte PDF");
    private final Label estado=new Label("Actualiza los datos para consultar ventas."),total=new Label(),aviso=new Label();
    private final TableView<ReporteVentasService.Linea> tabla=new TableView<>();
    private ReporteVentasService.Datos datos;
    private ReporteVentasService.Reporte reporte;
    private boolean ocupado;
    public ReportesView(){
        setPadding(new Insets(18));setSpacing(12);
        Label titulo=new Label("Reportes de ventas");titulo.setStyle("-fx-font-size:20px;-fx-font-weight:bold;");
        HBox filtros=new HBox(12,selector("Concierto",concierto),selector("Artista",artista),selector("Vendedor",vendedor));
        aplicar.setOnAction(e->aplicar());recargar.setOnAction(e->recargar());pdf.setOnAction(e->exportar());pdf.getStyleClass().add("boton-primario");
        Ui.columnas(tabla,Ui.col("Venta",l->String.valueOf(l.venta()),65),Ui.col("Fecha",ReporteVentasService.Linea::fecha,130),
                Ui.col("Concierto",ReporteVentasService.Linea::evento,175),Ui.col("Artista",ReporteVentasService.Linea::artista,140),
                Ui.col("Vendedor",ReporteVentasService.Linea::vendedor,110),Ui.col("Asiento",ReporteVentasService.Linea::entrada,80),
                Ui.col("Cant.",l->String.valueOf(l.cantidad()),55),Ui.col("Precio",l->Ui.dinero(l.precio()),100),Ui.col("Importe",l->Ui.dinero(l.subtotal()),100));
        tabla.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        tabla.setPlaceholder(new Label("Sin resultados para estos filtros."));VBox.setVgrow(tabla,Priority.ALWAYS);
        estado.setWrapText(true);aviso.setWrapText(true);total.setStyle("-fx-font-size:18px;-fx-font-weight:bold;");
        for(ComboBox<Opcion> cb:List.of(concierto,artista,vendedor))cb.valueProperty().addListener((o,a,b)->invalidar());
        getChildren().addAll(titulo,filtros,new HBox(10,aplicar,recargar,pdf),estado,tabla,total,aviso);controles();
    }
    private VBox selector(String nombre,ComboBox<Opcion> cb){cb.setMaxWidth(Double.MAX_VALUE);VBox v=new VBox(5,new Label(nombre),cb);HBox.setHgrow(v,Priority.ALWAYS);v.setPrefWidth(250);return v;}
    private void controles(){for(var cb:List.of(concierto,artista,vendedor))cb.setDisable(ocupado||datos==null);aplicar.setDisable(ocupado||datos==null);recargar.setDisable(ocupado);pdf.setDisable(ocupado||reporte==null);}
    private void invalidar(){reporte=null;tabla.getItems().clear();total.setText("");aviso.setText("");if(datos!=null)estado.setText("Pulsa Aplicar filtros para consultar esta selección.");controles();}
    private void llenar(ComboBox<Opcion> cb,List<Opcion> opciones,String todos){cb.getItems().setAll(new Opcion(null,todos));cb.getItems().addAll(opciones);cb.getSelectionModel().selectFirst();}
    @Override public void recargar(){
        if(ocupado)return;
        if(!Sesion.esAdmin()){Ui.error("Este módulo requiere una sesión de administrador.");return;}
        ocupado=true;reporte=null;tabla.getItems().clear();total.setText("");aviso.setText("");estado.setText("Consultando ventas y catálogos…");controles();
        Async.run(()->new ReporteVentasService().cargar(),d->{
            datos=d;llenar(concierto,ReporteVentasService.conciertos(d),"Todos los conciertos");llenar(artista,ReporteVentasService.artistas(d),"Todos los artistas");
            llenar(vendedor,ReporteVentasService.vendedores(d),"Todos los vendedores");ocupado=false;aplicar();
        },msg->{ocupado=false;datos=null;estado.setText("No se pudieron actualizar los datos. Pulsa Actualizar para reintentar.");controles();Ui.error(msg);});
    }
    private Integer id(ComboBox<Opcion> cb){return cb.getValue()==null?null:cb.getValue().id();}
    private void aplicar(){
        if(datos==null||ocupado)return;
        if(!Sesion.esAdmin()){Ui.error("Inicia sesión como administrador.");return;}
        try{
            String filtros=concierto.getValue()+" · "+artista.getValue()+" · "+vendedor.getValue();
            reporte=ReporteVentasService.filtrar(datos,id(concierto),id(artista),id(vendedor),filtros);
            tabla.getItems().setAll(reporte.lineas());total.setText("Total: "+Ui.dinero(reporte.total())+" · "+reporte.boletos()+" boleto(s) · "+reporte.ventas()+" venta(s)");
            estado.setText(reporte.lineas().isEmpty()?"Sin resultados para estos filtros.":reporte.lineas().size()+" detalle(s) incluidos.");aviso.setText(reporte.aviso());
        }catch(Exception ex){reporte=null;tabla.getItems().clear();total.setText("");aviso.setText("");estado.setText("No se pudo calcular el reporte.");Ui.error(ex.getMessage());}
        controles();
    }
    private void exportar(){
        if(reporte==null||ocupado)return;
        if(!Sesion.esAdmin()){Ui.error("Inicia sesión como administrador.");return;}
        FileChooser fc=new FileChooser();fc.setTitle("Guardar reporte de ventas");fc.setInitialFileName("reporte-ventas.pdf");fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF","*.pdf"));
        var archivo=fc.showSaveDialog(getScene().getWindow());if(archivo==null)return;Path ruta=archivo.toPath();
        if(!archivo.getName().toLowerCase(Locale.ROOT).endsWith(".pdf"))ruta=Path.of(archivo.getAbsolutePath()+".pdf");
        if(Files.exists(ruta)&&!Ui.confirmar("¿Reemplazar "+ruta.getFileName()+"?"))return;
        final var salida=ruta;final var copia=reporte;ocupado=true;controles();
        Async.run(()->{ReporteVentasPdf.exportar(copia,salida);return salida;},p->{ocupado=false;controles();Ui.info("Reporte guardado en: "+p);},msg->{ocupado=false;controles();Ui.error(msg);});
    }
}
