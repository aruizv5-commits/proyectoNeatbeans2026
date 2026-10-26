# Agregar los reportes en tu proyecto actual de NetBeans

Puedes integrar esta función copiando código desde GitHub; no necesitas descargar un ZIP ni modificar la API.

## Tres archivos nuevos

Crear estas clases en los paquetes indicados y copiar el contenido completo de los archivos del repositorio (usar el botón para copiar código o la vista Raw):

1. `com.umg.api.service.ReporteVentasService` — [código](../src/main/java/com/umg/api/service/ReporteVentasService.java).
2. `com.umg.api.reportes.ReporteVentasPdf` — [código](../src/main/java/com/umg/api/reportes/ReporteVentasPdf.java).
3. `com.umg.api.fx.view.ReportesView` — [código](../src/main/java/com/umg/api/fx/view/ReportesView.java).

Se usan las dependencias Jackson, JavaFX y JasperReports ya incluidas. Las clases nuevas admiten código Java 17, pero las bibliotecas JavaFX de este proyecto requieren un JDK reciente según README.

## Menú lateral de la versión actual

Si ya copiaste la interfaz lateral de [INTERFAZ.md](INTERFAZ.md), Reportes está registrado dentro de MainView. No agregues la línea de pestañas que aparece debajo; corresponde exclusivamente a la interfaz anterior.

## Una línea en MainView (solo versión anterior con pestañas)

En `MainView.java`, dentro del bloque `if (Sesion.esAdmin())`, después de agregar Usuarios, incorporar:

```java
agregar(tabs, "Reportes", new ReportesView());
```

No reemplazar todo MainView si tiene otras modificaciones locales.

## Consultar y exportar

Guardar, cerrar las ventanas antiguas y ejecutar Clean and Build / Run desde NetBeans. Entrar como administrador y abrir Reportes. Al entrar se consultan los datos de la API. Elegir Todos o una opción en cada filtro y pulsar Aplicar filtros. Los filtros se combinan entre sí. Actualizar datos vuelve a consultar la API y reinicia los filtros.

Guardar reporte PDF exporta exactamente el resultado aplicado. Cambiar una selección deshabilita la exportación y limpia el resultado anterior hasta volver a pulsar Aplicar filtros. El archivo también puede representar una consulta sin resultados, con total cero y sin filas ficticias.

El total corresponde a los detalles incluidos, no al total completo de cada compra. Esto evita contar otros conciertos de una compra mixta. Las ventas distintas no se suman entre reportes de distintos conciertos: una misma venta puede aparecer en ambos. Los reportes realizan únicamente GET y no reenvían correos ni cobran.

El módulo aparece en la interfaz del administrador; esto no sustituye la autorización en el servidor, que sigue siendo un pendiente independiente. Para no consumir el endpoint de usuarios con credenciales, el vendedor puede mostrarse como Vendedor #ID.

## Pruebas reproducibles con datos ficticios

Desde la raíz del proyecto, después de compilar:

```sh
java -cp target/proyecto-1.0-SNAPSHOT-all.jar verificacion/PruebaReportes.java resultados-prueba
```

Prueba concierto, artista, vendedor y combinaciones; compra con varios conciertos; precios históricos; ID 0 de artista; resultado vacío; detalles duplicados y referencias ausentes. Genera PDFs de muestra y de paginación. No consulta ni escribe en la API.

La prueba de interfaz `verificacion/PruebaInterfazReportes.java` usa los mismos datos ficticios, requiere pantalla JavaFX y verifica que un filtro nuevo invalide el PDF anterior. Compilarla junto con PruebaReportes y ejecutarla con el JAR del proyecto en el classpath. No requiere credenciales reales.

El ejemplo publicado es ficticio; el PDF y la captura no representan las ventas actuales de la base compartida.
