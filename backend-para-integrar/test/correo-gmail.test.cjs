const assert = require('node:assert/strict');
const {google} = require('googleapis');
const original = google.gmail;
let requests = 0;
google.gmail = () => ({users:{messages:{send:async mensaje => {
  requests++;
  assert.equal(mensaje.userId,'me');
  const mime=Buffer.from(mensaje.requestBody.raw,'base64url').toString('utf8');
  assert(mime.includes('cliente@example.invalid'));
  assert(mime.includes('comprobante.pdf'));
  assert(mime.includes('application/pdf'));
  return {data:{id:'simulacion-sin-envio'}};
}}}});
(async()=>{
 try {
  const crear=require('../correo-gmail');
  assert.throws(()=>crear({}),/GOOGLE_CLIENT_ID/);
  const transporte=crear({GOOGLE_CLIENT_ID:'test-only',GOOGLE_CLIENT_SECRET:'test-only',GOOGLE_REFRESH_TOKEN:'test-only'});
  const r=await transporte.sendMail({from:'remitente@example.invalid',to:'cliente@example.invalid',subject:'Prueba local',text:'Prueba',attachments:[{filename:'comprobante.pdf',content:Buffer.from('%PDF-simulado'),contentType:'application/pdf'}]});
  assert.equal(requests,1);assert.equal(r.messageId,'simulacion-sin-envio');
  console.log('PASS: real MIME composition and mocked Gmail HTTPS API, PDF attachment, no real mail.');
 } finally { google.gmail=original; }
})().catch(e=>{console.error(e);process.exitCode=1;});
