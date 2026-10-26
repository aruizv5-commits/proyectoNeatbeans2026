## Parche completo, actualizado y probado

El archivo [0001-comprobantes-correo-qr.patch](parches/0001-comprobantes-correo-qr.patch) contiene la integración ya aplicada sobre `0143e045fd4601bedaff051779815f24137b5bfb`, incluidos módulos, rutas, hook de correo, nombre de vendedor, dependencias/lockfile y pruebas. Si usas el parche, no repitas los pasos manuales equivalentes de abajo.

En un checkout limpio de la API con la versión indicada, aplicar con `git am ruta/al/0001-comprobantes-correo-qr.patch`. Si hay cambios posteriores, revisar y resolver la integración; no forzar ni reemplazar el trabajo de compañeros. Ejecutar `npm ci` y `npm test` antes del despliegue. El parche también corrige el nombre del modelo en el conteo de inventario (`db.i_boletos`) y rechaza IDs de asientos repetidos antes de escribir. No modifica las tarifas.

La cuenta VaneSiam recibió una denegación de escritura del proxy Git incluso después de comunicar la aceptación de la invitación. La integración NO está desplegada en Render por ese motivo. La configuración de Google/Gmail y del secreto QR en Render sigue requiriendo al administrador del servicio.

# Integración en Proyectoapi2026 / Render

La integración se revisó sobre el commit `0143e045fd4601bedaff051779815f24137b5bfb` de la API existente. No son un servidor independiente. No copiar node_modules ni archivos de credenciales. No se modifican reglas de precios, nombres de localidades ni tablas.

## 1. Archivos y dependencias

Copiar `comprobantes.js`, `comprobantes.instance.js` y `correo-gmail.js` en un directorio nuevo `Proyecto_final_progra2/service/` del backend.

Desde la raíz de la API ejecutar:

```sh
npm install --save-exact pdfkit@0.17.2 qrcode@1.5.4
```

Conservar los cambios de package.json y package-lock.json. La API ya incluye nodemailer y googleapis. El módulo no utiliza el antiguo transporte SMTP ni desactiva TLS: Gmail se invoca por HTTPS.

## 2. Configuración de Render

El administrador del servicio debe configurar estos nombres en Environment, sin publicar sus valores en GitHub:

- `BOLETOS_QR_SECRET`: secreto aleatorio estable de al menos 32 bytes. Generarlo con `node -e "console.log(require('node:crypto').randomBytes(32).toString('hex'))"` en su máquina y guardarlo directamente en Render. No enviarlo por chat. Rotarlo invalida los enlaces QR anteriores.
- `PUBLIC_BASE_URL`: `https://proyectoapi2026.onrender.com`
- `MAIL_FROM`: dirección real del buzón Gmail autorizado (puede ser `Boletería <buzon@dominio>`). No inventar un remitente.
- Reutilizar los bindings existentes de `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET`, `GOOGLE_REFRESH_TOKEN` si funcionan. El refresh token debe permitir enviar con Gmail (gmail.send o mail.google.com); si no, el propietario debe volver a autorizar la aplicación. No añadirlos al código.

No hace falta compartir acceso a la cuenta ni valores secretos con el asistente. No se enviaron correos reales durante la preparación.

## 3. Página visual del QR

En `server.js`, después de crear `app` e importar los modelos, antes de iniciar `listen`, agregar:

```js
try {
  require('./Proyecto_final_progra2/service/comprobantes.instance')().registrar(app);
} catch {
  console.warn('Página QR pendiente: revisar BOLETOS_QR_SECRET y PUBLIC_BASE_URL en Render.');
}
```

El QR firmado abre `/api/boletos/ver/:token`. La página muestra evento, fecha, recinto y localidad/asiento. No publica nombre, correo, teléfono, NIT ni credenciales. Un enlace alterado devuelve 404. El texto dice “Boleto registrado”: no afirma que una entrada nunca se haya usado. El control de ingreso requiere implementación adicional.

## 4. Agregar qr_url a los detalles existentes

En `controller/detalle_ventas.controller.js`, agregar fuera de las funciones:

```js
const obtenerComprobantes = require('../service/comprobantes.instance');
function conQr(detalle) {
  const json = typeof detalle.toJSON === 'function' ? detalle.toJSON() : detalle;
  if (!json) return null;
  try { return { ...json, qr_url: obtenerComprobantes().enlace(json) }; }
  catch { return { ...json, qr_url: null }; }
}
```

En `findAll`, cambiar `res.send(data)` por `res.send(data.map(conQr))`.
En `findOne`, cambiar `res.send(data)` por `res.send(data ? conQr(data) : null)`.

La API conserva el array de detalles y sus campos actuales; qr_url es un campo adicional. Con autenticación futura, proteger también la emisión/listado de enlaces. El enlace QR es para lectura del boleto, no una credencial de administrador ni una validación de ingreso.

## 5. Correo automático después de registrar una venta

En `create`, conservar la validación, el cálculo de precios y la escritura de detalles/inventario. Después de:

```js
await ventaID.update({total_venta:Nuevototal});
```

Eliminar el bloque antiguo que comienza con el comentario del envío de correo y llama a `mail_rover`. Eliminar también su antigua respuesta `return res.status(201).send(resultados)` y sustituir ambas cosas por:

```js
let correoEstado = 'pendiente';
try {
  await obtenerComprobantes().enviar(ventaID.id_venta);
  correoEstado = 'aceptado';
} catch {
  console.error('Comprobante pendiente de correo para venta', ventaID.id_venta,
    ': revisar configuración Gmail y datos de la venta. No volver a cobrar.');
}
res.set('X-Comprobante-Correo', correoEstado);
return res.status(201).send(resultados.map(d => ({
  ...conQr(d),
  comprobante_correo: correoEstado
})));
```

Eliminar las importaciones y funciones antiguas que solo usaba ese bloque: `transporter`, `nodemailer`, `google`, `OAuth2`, `accountTransport`, `_SERVER`, `_ID_APP_1`, `head`, `footer` y `mail_rover`. Así no se ejecuta el antiguo correo ni su BCC ajeno. No eliminar `db`, `Detalle_ventas` ni `Op`.

El nuevo mensaje va únicamente al correo que pertenece al cliente de la venta en la BD. Adjunta un PDF con el comprobante y un boleto/QR por página. No acepta un destinatario enviado desde el cliente Java. No agrega BCC. Usa los importes cobrados, sin recalcular tarifas.

`aceptado` significa que Gmail aceptó el envío, no prueba de entrega en bandeja de entrada. Un fallo de correo conserva la venta y devuelve `pendiente`; no repetir la compra para reenviar. Esta implementación no contiene una cola de reintentos ni un endpoint público de reenvío.

## 6. Nombre del vendedor

Para que un administrador imprima también el nombre de otros vendedores, en `ventas.controller.js`, dentro de `findOne`, cambiar el callback de `then(data => ...)` por:

```js
.then(async data => {
  if (!data) return res.send(null);
  const vendedor = await db.usuario.findByPk(data.id_vendedor, {
    attributes: ['nombre_completo', 'nombre_usuario']
  });
  return res.send({
    ...data.toJSON(),
    vendedor_nombre: vendedor
      ? (vendedor.nombre_completo || vendedor.nombre_usuario)
      : `Vendedor #${data.id_vendedor}`
  });
})
```

No incluir contrasena_hash ni retornar la fila de usuario completa. El frontend usa el usuario de sesión si coincide con el vendedor; si falta el nombre remoto, muestra Vendedor #ID, sin inventar un nombre.

## 7. Publicar y verificar

1. Revisar y publicar los cambios del backend con el propietario de Proyectoapi2026; desplegar en Render.
2. Consultar `GET /api/detalle_ventas/21`: debe devolver qr_url. Abrir esa URL y verificar que contiene el boleto correcto; no contiene datos del comprador.
3. En NetBeans, generar PDF de una venta existente. Esto no envía correo ni crea una venta.
4. Para probar el envío, usar una venta de prueba acordada y un cliente con correo de prueba controlado. Verificar respuesta comprobante_correo=aceptado y recepción del adjunto; revisar también spam. No usar ventas de clientes reales sin autorización.
5. Verificar un fallo de correo sin repetir la compra: la venta debe persistir y quedar como pendiente. No confundir aceptado con entregado.

La lectura de QR no modifica aforo ni marca ingreso. La transacción de venta existente de la API sigue siendo responsabilidad del backend; estos módulos no la vuelven atómica ni sustituyen autenticación/roles del servidor.

## Pruebas locales del módulo (sin correo real)

En esta carpeta, ejecutar `npm ci` y `npm test`. Las pruebas levantan Express solo en loopback, usan datos ficticios, generan el PDF en memoria y simulan tanto el transporte como Gmail HTTPS. No usan la BD compartida ni credenciales reales.
