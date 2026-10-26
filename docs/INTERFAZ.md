# Integrar el diseño recibido sin perder las funciones actuales

El ZIP de referencia `proyectoNeatbeans2026interfaz.zip` contiene una aplicación Swing con formularios .form y otra clase de sesión. El proyecto que ya comprobó ventas, comprobantes y reportes es JavaFX. La integración publicada adapta el menú lateral, la paleta azul, el estilo de tablas y la presentación del login a JavaFX. No reemplaza servicios ni modelos por los del ZIP Swing, ni agrega AbsoluteLayout.

## Qué quedó integrado

- Menú lateral azul con los módulos existentes. Administrador: Punto de venta, Clientes, Comprobantes y QR, Artistas, Conciertos, Localidades e inventario, Usuarios y Reportes de ventas. Vendedor: Punto de venta, Clientes y Comprobantes y QR.
- Encabezado del módulo, usuario de sesión y cierre de sesión.
- Botón ☰ para contraer/expandir el menú. Las opciones contraídas conservan tooltip y nombre accesible.
- Tablas con cabecera azul, filas alternadas y selección clara, inspiradas en Estilos.java del ZIP.
- Vistas conservadas al navegar: no se reconstruye ni vacía el carrito al cambiar de módulo. Se mantiene la actualización de datos que ya existía.
- Contenedores con desplazamiento y dimensiones de ventana adaptadas al área disponible de pantalla. Punto de venta ajustado para mantener accesibles Agregar al carrito y Cobrar.

## Copiar los cambios a tu NetBeans actual, sin descargar un ZIP

1. Cierra la aplicación en ejecución. Copia tu carpeta actual como respaldo, porque puede contener cambios locales que no estén en GitHub.
2. Abre cada archivo de esta lista en GitHub, pulsa copiar código o Raw y copia su contenido completo.
3. En tu proyecto actual de NetBeans, abre el mismo archivo existente, selecciona todo y pega el código nuevo. No crees paquetes llamados como los archivos y no pegues las palabras Java/JavaJava.

| Archivo existente | Paquete / ubicación |
| --- | --- |
| [MainView.java](../src/main/java/com/umg/api/fx/view/MainView.java) | com.umg.api.fx.view |
| [LoginView.java](../src/main/java/com/umg/api/fx/view/LoginView.java) | com.umg.api.fx.view |
| [PuntoVentaView.java](../src/main/java/com/umg/api/fx/view/PuntoVentaView.java) | com.umg.api.fx.view |
| [ReportesView.java](../src/main/java/com/umg/api/fx/view/ReportesView.java) | com.umg.api.fx.view |
| [FxApp.java](../src/main/java/com/umg/api/fx/FxApp.java) | com.umg.api.fx |
| [style.css](../src/main/resources/fx/style.css) | src/main/resources/fx/style.css; copiar desde la pestaña Archivos si no aparece en Paquetes fuente |

4. Guarda los seis archivos. No reemplaces pom.xml, Esqueleto.java, servicios, modelos ni tus clases de comprobantes con los archivos del ZIP Swing. La clase principal del ZIP Swing solo imprime Hello World; la de este proyecto inicia JavaFX.
5. Ejecuta Limpiar y construir, después Ejecutar proyecto. Si abres un JAR viejo, seguirá apareciendo la interfaz anterior.
6. Inicia sesión como administrador y comprueba los ocho módulos del menú lateral. En esta versión los reportes ya están registrados: no agregues la vieja línea agregar(tabs, ...) de la interfaz con pestañas.
7. Selecciona un concierto, agrega un asiento al carrito, cambia a Clientes y regresa a Punto de venta. El carrito debe conservar el asiento. Esta prueba no requiere pulsar Cobrar.
8. Comprueba Reportes y exporta el PDF de una venta/consulta existente. El diseño del PDF se conserva; este cambio es de la ventana de la aplicación.
9. Prueba cerrar sesión y entrar como vendedor. No deben aparecer los módulos administrativos. Esto es control de navegación del cliente; la autorización del servidor es una tarea independiente.

Para pantallas pequeñas, contrae el menú con ☰. Si una tabla o formulario necesita más espacio, utiliza sus barras de desplazamiento. Maximizar también permite ver más columnas; no es necesario modificar la resolución de Windows.

## Verificación realizada

- Build/package de Maven; no se agregaron dependencias.
- Prueba JavaFX con datos ficticios: ocho módulos para administrador y tres para vendedor, navegación y selección activa, conservación del carrito y del total VIP Q1,450, cierre de sesión y validación del login vacío.
- Ventana real de prueba de 1024×620 con menú contraído: Agregar al carrito y Cobrar quedan dentro del área visible.
- Comparación de código: desde recargar() hasta el final de PuntoVentaView se conserva exactamente la lógica anterior de datos, filtros, tarifas, carrito y cobro.
- Sin llamadas a la API, ventas ni correos durante estas pruebas de interfaz. Las capturas muestran datos ficticios.

La prueba reproducible está en `verificacion/PruebaInterfazIntegrada.java`. Compilar junto con PruebaReportes.java con el JAR construido en el classpath; ejecutar `com.umg.api.fx.view.PruebaInterfazIntegrada carpeta-capturas` con una pantalla JavaFX disponible.

## Sigue pendiente del backend

Esta integración no activa el envío automático de correo ni la página visual del QR. Los cambios de API y Render siguen separados en backend-para-integrar. Los comprobantes, boletos y reportes locales conservan su funcionamiento actual.
