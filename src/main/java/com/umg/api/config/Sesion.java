package com.umg.api.config;

import com.umg.api.model.usuario;

/** Usuario que inició sesión y su token (si el backend lo devuelve). */
public final class Sesion {
    private static volatile usuario actual;
    private static volatile String token;

    private Sesion() {}

    public static void iniciar(usuario u, String t) {
        actual = u;
        token = (t == null || t.isBlank()) ? null : t;
    }

    public static void cerrar() {
        actual = null;
        token = null;
    }

    public static usuario actual() { return actual; }
    public static String token() { return token; }
    public static boolean activa() { return actual != null; }

    public static boolean esAdmin() {
        if (actual == null || actual.getRol() == null) return false;
        // acepta "admin", "administrador", "Administrador", etc.
        return actual.getRol().trim().toLowerCase().contains("admin");
    }
}
