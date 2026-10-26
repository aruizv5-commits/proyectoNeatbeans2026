package com.umg.api.service;

import com.umg.api.model.usuario;
import java.util.List;

/**
 * Inicio de sesión. La API no tiene una ruta de login, entonces se pide la lista de usuarios
 * (GET /api/usuario) y se compara el nombre de usuario y la contraseña.
 */
public class authservice {

    public static class LoginRespuesta {
        public String token;          // la API no maneja token
        public usuario usuario;
    }

    public LoginRespuesta login(String nombreUsuario, String contrasena) throws Exception {
        List<usuario> lista = new usuarioservice().getUsuario();
        for (usuario u : lista) {
            if (nombreUsuario.equals(u.getNombre_usuario()) && contrasena.equals(u.getContrasena_hash())) {
                LoginRespuesta r = new LoginRespuesta();
                r.usuario = u;
                return r;
            }
        }
        throw new Exception("Usuario o contraseña incorrectos.");
    }
}
