const assert=require('node:assert/strict');const crypto=require('node:crypto');
const crear=require('../comprobantes');
const express=require('express');
const venta={id_venta:8,id_vendedor:2,fecha_venta:'2026-10-25T20:00:00.000Z',total_venta:1225};
const detalles=[{id_detalle:21,id_inventario:700,id_cliente:1,asiento:'J-10',cantidad:1,precio_unitario:500,subtotal:500,id_venta:8},{id_detalle:22,id_inventario:701,id_cliente:1,asiento:'A-1',cantidad:1,precio_unitario:725,subtotal:725,id_venta:8}];
const db={ventas:{findByPk:async()=>venta},d_ventas:{findAll:async()=>detalles,findByPk:async id=>detalles.find(d=>d.id_detalle===id)},
 i_boletos:{findByPk:async id=>({id_concierto:1,id_localidad:id})},concierto:{findByPk:async()=>({titulo_evento:'Evento de ejemplo <script>alert(1)</script>',fecha_concierto:'2026-12-06T00:00:00.000Z',recinto:'Recinto de ejemplo'})},
 localidades:{findByPk:async id=>({nombre:id===700?'General':'VIP'})},
 cliente:{findByPk:async()=>({nombre:'Cliente',apellido:'Ejemplo',correo:'cliente@example.invalid',telefono:'0000-0000'})},
 usuario:{findByPk:async(_id,options)=>{assert(!options.attributes.includes('contrasena_hash'));return {nombre_completo:'Vendedor ejemplo'};}}};
let enviados=0;
const api=crear({db,secret:crypto.randomBytes(32).toString('hex'),baseUrl:'https://proyectoapi2026.onrender.com',from:'Boleteria <sender@example.invalid>',
 transporter:{sendMail:async m=>{enviados++;assert.equal(m.to,'cliente@example.invalid');assert(!m.bcc);assert(!m.html.includes('<script>'));assert.equal(m.attachments[0].content.subarray(0,4).toString(),'%PDF');
 
 return {accepted:[m.to],messageId:'mock-only'};}}});
(async()=>{
 const url=api.enlace(detalles[0]);const token=url.split('/').pop();assert.equal(api.verificar(token),21);
 assert.throws(()=>api.verificar(token.slice(0,-1)+(token.endsWith('A')?'B':'A')));
 const d=await api.datosVenta(8);assert.equal(d.total,122500);assert.equal(d.lineas.length,2);assert.equal(d.lineas[0].fecha,'06/12/2026');
 assert(!url.includes('cliente'));assert(!url.includes('0000'));
 const app=express();api.registrar(app);const server=app.listen(0,'127.0.0.1');await new Promise(r=>server.once('listening',r));
 try{const base='http://127.0.0.1:'+server.address().port;
  const r=await fetch(base+'/api/boletos/ver/'+token);assert.equal(r.status,200);const html=await r.text();assert(!html.includes('cliente@example.invalid'));assert(!html.includes('<script>'));assert(html.includes('06/12/2026'));
  assert.equal((await fetch(base+'/api/boletos/ver/invalido')).status,404);
 }finally{server.close();}
 const estado=await api.enviar(8);assert.equal(estado.estado,'aceptado');assert.equal(enviados,1);
 venta.total_venta=1000;await assert.rejects(()=>api.enviar(8));assert.equal(enviados,1);
 console.log('PASS: signed QR rejects tampering; real Express route; public page hides contact data; dates; receipt PDF; simulated mail; inconsistent totals blocked. No real mail or database access.');
})().catch(e=>{console.error(e);process.exitCode=1;});
