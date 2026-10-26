// Archivo nuevo: Proyecto_final_progra2/service/comprobantes.js
const crypto = require('node:crypto');
const PDFDocument = require('pdfkit');
const QRCode = require('qrcode');

module.exports = function crearComprobantes({ db, secret, baseUrl, transporter, from }) {
  if (!secret || Buffer.byteLength(secret) < 32) throw new Error('BOLETOS_QR_SECRET debe tener al menos 32 bytes.');
  const base = new URL(baseUrl);
  if (base.protocol !== 'https:' || base.username || base.password || base.search || base.hash)
    throw new Error('PUBLIC_BASE_URL debe ser una dirección HTTPS pública.');
  const origen = base.origin;
  const escapar = v => String(v ?? '').replace(/[&<>"']/g, c => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
  const dinero = v => {
    const n = Number(v);
    if (v == null || !Number.isFinite(n) || n < 0) throw new Error('Importe inválido.');
    return Math.round(n * 100);
  };
  const quetzales = centavos => 'Q' + (centavos / 100).toLocaleString('en-US', {minimumFractionDigits:2,maximumFractionDigits:2});
  const fechaVenta = v => new Intl.DateTimeFormat('es-GT', {timeZone:'America/Guatemala',dateStyle:'long',timeStyle:'short'}).format(new Date(v));
  const fechaEvento = v => {
    const s = String(v instanceof Date ? v.toISOString() : v).slice(0, 10);
    if (!/^\d{4}-\d{2}-\d{2}$/.test(s)) return 'Fecha no disponible';
    return s.split('-').reverse().join('/');
  };
  const firma = texto => crypto.createHmac('sha256', secret).update(texto).digest('base64url');
  function enlace(detalle) {
    const id = Number(detalle.id_detalle);
    if (!Number.isSafeInteger(id) || id < 1) throw new Error('Detalle inválido.');
    const payload = Buffer.from(JSON.stringify({version:1,detalle:id})).toString('base64url');
    return origen + '/api/boletos/ver/' + payload + '.' + firma(payload);
  }
  function verificar(token) {
    if (typeof token !== 'string' || token.length > 500) throw new Error('QR inválido.');
    const partes = token.split('.');
    if (partes.length !== 2) throw new Error('QR inválido.');
    const [payload, valor] = partes;
    if (!/^[A-Za-z0-9_-]+$/.test(payload) || !/^[A-Za-z0-9_-]{43}$/.test(valor)) throw new Error('QR inválido.');
    const esperado = Buffer.from(firma(payload));
    const recibido = Buffer.from(valor);
    if (esperado.length !== recibido.length || !crypto.timingSafeEqual(esperado, recibido)) throw new Error('QR inválido.');
    const datos = JSON.parse(Buffer.from(payload,'base64url').toString('utf8'));
    if (datos.version !== 1 || !Number.isSafeInteger(datos.detalle) || datos.detalle < 1) throw new Error('QR inválido.');
    return datos.detalle;
  }
  async function uno(modelo, id, opciones) {
    const dato = await modelo.findByPk(id, opciones);
    if (!dato) throw new Error('Falta un registro asociado al boleto.');
    return dato;
  }
  async function linea(d) {
    const inv = await uno(db.i_boletos,d.id_inventario);
    const evento = await uno(db.concierto,inv.id_concierto);
    const localidad = await uno(db.localidades,inv.id_localidad);
    const cantidad = Number(d.cantidad);
    if (cantidad !== 1) throw new Error('Esta API vende un asiento por detalle; revisa la cantidad.');
    const precio = dinero(d.precio_unitario), subtotal = dinero(d.subtotal);
    if (precio !== subtotal) throw new Error('Detalle inconsistente.');
    return {id:Number(d.id_detalle),evento:evento.titulo_evento,fecha:fechaEvento(evento.fecha_concierto),
      recinto:evento.recinto,tipo:localidad.nombre,asiento:d.asiento || localidad.asiento,
      cantidad,precio,subtotal,qr_url:enlace(d)};
  }
  async function datosVenta(id) {
    const venta = await uno(db.ventas,id);
    const detalles = await db.d_ventas.findAll({where:{id_venta:id},order:[['id_detalle','ASC']]});
    if (!detalles.length) throw new Error('La venta no tiene detalles.');
    const clientes = new Set(detalles.map(d => Number(d.id_cliente)));
    if (clientes.size !== 1) throw new Error('La venta no tiene un cliente único.');
    const cliente = await uno(db.cliente,[...clientes][0],{attributes:['id','nombre','apellido','correo','telefono']});
    const vendedor = await uno(db.usuario,venta.id_vendedor,{attributes:['id_usuario','nombre_completo','nombre_usuario']});
    const lineas = await Promise.all(detalles.map(linea));
    const total = dinero(venta.total_venta);
    if (lineas.reduce((s,l) => s+l.subtotal,0) !== total) throw new Error('El total no coincide con sus detalles.');
    return {numero:Number(venta.id_venta),fecha:fechaVenta(venta.fecha_venta),
      cliente:[cliente.nombre,cliente.apellido].filter(Boolean).join(' '),telefono:cliente.telefono,
      correo:cliente.correo,vendedor:vendedor.nombre_completo || vendedor.nombre_usuario,
      observaciones:'Precios finales registrados en la venta.',lineas,total};
  }
  function html(d) {
    return `<html lang="es"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><title>Comprobante ${d.numero}</title></head>
    <body style="font-family:Arial,sans-serif;color:#142438;background:#f0f5f8;padding:24px"><main style="max-width:760px;margin:auto;background:white;padding:28px;border-radius:14px">
    <h1>Comprobante de venta #${d.numero}</h1><p><b>Fecha de venta:</b> ${escapar(d.fecha)}</p>
    <p><b>Vendedor:</b> ${escapar(d.vendedor)}</p><p><b>Cliente:</b> ${escapar(d.cliente)}<br><b>Teléfono:</b> ${escapar(d.telefono)}</p>
    <table cellpadding="9" style="width:100%;border-collapse:collapse"><tr style="background:#e7eef3"><th>Evento</th><th>Entrada</th><th>Cant.</th><th>Precio</th><th>Total</th></tr>
    ${d.lineas.map(l=>`<tr><td>${escapar(l.evento)}</td><td>${escapar(l.tipo)} ${escapar(l.asiento)}</td><td>${l.cantidad}</td><td>${quetzales(l.precio)}</td><td>${quetzales(l.subtotal)}</td></tr>`).join('')}</table>
    <h2>Total pagado: ${quetzales(d.total)}</h2><p>Cantidad de boletos: ${d.lineas.length}</p><p><b>Observaciones:</b> ${escapar(d.observaciones)}</p>
    <p>Se adjunta tu comprobante y los boletos con QR en PDF. Documento académico, no fiscal.</p></main></body></html>`;
  }
  async function pdf(d) {
    const imagenes = await Promise.all(d.lineas.map(l => QRCode.toBuffer(l.qr_url,{type:'png',width:360,margin:4})));
    return new Promise((resolve,reject)=> {
      const doc = new PDFDocument({size:'A4',margin:40});
      const chunks=[];doc.on('data',b=>chunks.push(b));doc.on('end',()=>resolve(Buffer.concat(chunks)));doc.on('error',reject);
      const cabecera = titulo => {
        doc.rect(40,35,515,80).fill('#142438');doc.fillColor('#ffffff').font('Helvetica-Bold').fontSize(21).text(titulo,58,61,{width:480});
        doc.fillColor('#142438').font('Helvetica').fontSize(11);doc.y=140;
      };
      const espacio = h => {if(doc.y+h>745){doc.addPage();cabecera('Comprobante · continuación');}};
      cabecera('Comprobante de venta #' + d.numero);
      doc.text('Fecha de venta: '+d.fecha).moveDown(0.6);
      doc.text('Vendedor: '+d.vendedor).moveDown(0.6);
      doc.text('Cliente: '+d.cliente).moveDown(0.6);
      doc.text('Teléfono: '+d.telefono).moveDown();
      for (const l of d.lineas) {
        espacio(135);doc.font('Helvetica-Bold').text(l.evento,{width:500});doc.font('Helvetica');
        doc.text('Tipo de entrada: '+l.tipo+' · Asiento: '+l.asiento,{width:500});
        doc.text('Cantidad: '+l.cantidad+' · Precio por boleto: '+quetzales(l.precio)+' · Subtotal: '+quetzales(l.subtotal),{width:500}).moveDown();
      }
      espacio(130);doc.font('Helvetica-Bold').fontSize(18).text('Total pagado: '+quetzales(d.total));
      doc.font('Helvetica').fontSize(11).text('Cantidad de boletos: '+d.lineas.length).moveDown();
      doc.text('Observaciones: '+d.observaciones,{width:500});
      d.lineas.forEach((l,i)=> {
        doc.addPage();cabecera('Boleto · '+l.evento);
        doc.font('Helvetica-Bold').fontSize(16).text(l.evento,{width:500});doc.font('Helvetica').fontSize(12).moveDown();
        doc.text('Fecha del evento: '+l.fecha).text('Recinto: '+l.recinto).moveDown();
        doc.text('Entrada: '+l.tipo+' · Asiento: '+l.asiento).text('Precio por boleto: '+quetzales(l.precio));
        doc.image(imagenes[i],180,330,{width:230});
        doc.fontSize(10).text('Escanea el QR para consultar el boleto registrado.',40,590,{width:515,align:'center'});
        doc.text('La consulta no registra el ingreso al evento.',40,610,{width:515,align:'center'});
      });
      doc.end();
    });
  }
  async function enviar(id) {
    if (!transporter || !from) throw new Error('Falta configurar el remitente del correo.');
    const d = await datosVenta(id);
    if (typeof d.correo !== 'string' || !/^[^\s@<>]+@[^\s@<>]+\.[^\s@<>]+$/.test(d.correo)) throw new Error('El cliente no tiene un correo válido.');
    const adjunto = await pdf(d);
    const resultado = await transporter.sendMail({from,to:d.correo,subject:'Comprobante de venta #'+d.numero,
      text:'Comprobante de venta #'+d.numero+'. Total pagado: '+quetzales(d.total)+'. Consulta el PDF adjunto.',html:html(d),
      attachments:[{filename:'comprobante-venta-'+d.numero+'.pdf',content:adjunto,contentType:'application/pdf'}]});
    if (!resultado.accepted || !resultado.accepted.length) throw new Error('El servidor de correo no aceptó el destinatario.');
    return {estado:'aceptado',messageId:resultado.messageId};
  }
  function registrar(app) {
    app.get('/api/boletos/ver/:token',async(req,res)=> {
      res.set({'Cache-Control':'no-store','Referrer-Policy':'no-referrer','X-Content-Type-Options':'nosniff',
        'Content-Security-Policy':"default-src 'none'; style-src 'unsafe-inline'; frame-ancestors 'none'"});
      try {
        const id=verificar(req.params.token);
        const detalle=await uno(db.d_ventas,id);
        await uno(db.ventas,detalle.id_venta);
        const l=await linea(detalle);
        res.type('html').send(`<!doctype html><html lang="es"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><title>Boleto registrado</title></head><body style="font-family:Arial;background:#f0f5f8;color:#142438;padding:24px"><main style="max-width:600px;margin:auto;background:white;padding:28px;border-radius:14px"><h1>Boleto registrado</h1><h2>${escapar(l.evento)}</h2><p>Fecha del evento: ${escapar(l.fecha)}</p><p>Recinto: ${escapar(l.recinto)}</p><p>Entrada: ${escapar(l.tipo)} · Asiento: ${escapar(l.asiento)}</p><p>Boleto #${l.id}</p><hr><p>Esta consulta no confirma si el boleto ya se utilizó ni registra el ingreso.</p></main></body></html>`);
      } catch { res.status(404).type('html').send('<h1>Boleto no disponible</h1><p>El enlace es incorrecto o el registro ya no existe.</p>'); }
    });
  }
  return {enlace,verificar,datosVenta,html,pdf,enviar,registrar};
};
