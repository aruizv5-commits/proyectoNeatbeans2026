// Archivo nuevo: Proyecto_final_progra2/service/correo-gmail.js
const nodemailer = require('nodemailer');
const {google} = require('googleapis');
module.exports = function crearCorreoGmail(env = process.env) {
  for (const nombre of ['GOOGLE_CLIENT_ID','GOOGLE_CLIENT_SECRET','GOOGLE_REFRESH_TOKEN'])
    if (!env[nombre]) throw new Error('Falta configurar ' + nombre + ' en Render.');
  const oauth = new google.auth.OAuth2(env.GOOGLE_CLIENT_ID,env.GOOGLE_CLIENT_SECRET);
  oauth.setCredentials({refresh_token:env.GOOGLE_REFRESH_TOKEN});
  const gmail = google.gmail({version:'v1',auth:oauth});
  const compositor = nodemailer.createTransport({streamTransport:true,buffer:true,newline:'unix'});
  return {
    async sendMail(mensaje) {
      const preparado = await compositor.sendMail(mensaje);
      const respuesta = await gmail.users.messages.send({userId:'me',requestBody:{raw:preparado.message.toString('base64url')}});
      if (!respuesta.data.id) throw new Error('Gmail no confirmó la aceptación del mensaje.');
      return {accepted:[mensaje.to],messageId:respuesta.data.id};
    }
  };
};
