package com.umg.api.config;

import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.HttpHeaders;

/**
 * Configuración central de la API (Node/Express).
 *
 * URL base, en orden de prioridad:
 *   1. -Dapi.url=http://localhost:3000/api            (propiedad de la JVM)
 *   2. variable de entorno BOLETOS_API_URL
 *   3. https://proyectoapi2026.onrender.com/api       (por defecto, API en Render)
 *
 * En NetBeans: clic derecho al proyecto > Properties > Run > VM Options.
 */
public final class ApiConfig {

    private static final String BASE = resolverBase();

    private ApiConfig() {}

    private static String resolverBase() {
        String url = System.getProperty("api.url");
        if (url == null || url.isBlank()) url = System.getenv("BOLETOS_API_URL");
        if (url == null || url.isBlank()) url = "https://proyectoapi2026.onrender.com/api";
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }

    /** URL completa de un recurso, por ejemplo url("artista") -> https://proyectoapi2026.onrender.com/api/artista */
    public static String url(String recurso) {
        return BASE + "/" + recurso;
    }

    /**
     * Cliente HTTP que agrega "Authorization: Bearer ..." cuando hay sesión iniciada.
     * Si el backend no usa token, simplemente no se envía nada.
     */
    public static CloseableHttpClient crearCliente() {
        return HttpClients.custom()
                .addRequestInterceptorFirst((request, entity, context) -> {
                    String token = Sesion.token();
                    if (token != null && !request.containsHeader(HttpHeaders.AUTHORIZATION)) {
                        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token);
                    }
                })
                .build();
    }
}
