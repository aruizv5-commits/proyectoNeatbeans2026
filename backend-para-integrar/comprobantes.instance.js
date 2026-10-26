// Ubicación: Proyecto_final_progra2/service/comprobantes.instance.js
let instancia;
module.exports = function obtenerComprobantes() {
  if (!instancia) {
    const crear = require('./comprobantes');
    instancia = crear({
      db: require('../models'),
      secret: process.env.BOLETOS_QR_SECRET,
      baseUrl: process.env.PUBLIC_BASE_URL || 'https://proyectoapi2026.onrender.com',
      from: process.env.MAIL_FROM,
      transporter: {
        sendMail: mensaje => require('./correo-gmail')().sendMail(mensaje)
      }
    });
  }
  return instancia;
};
