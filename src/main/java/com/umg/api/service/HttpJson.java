package com.umg.api.service;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.umg.api.config.ApiConfig;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.entity.EntityBuilder;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.io.entity.EntityUtils;

/**
 * POST JSON que, a diferencia de los servicios CRUD, conserva el mensaje de error del backend
 * ({"message": "..."}), para mostrárselo tal cual al usuario (ej. "Solo quedan 2 boletos en VIP").
 */
final class HttpJson {
    static final ObjectMapper MAPPER = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private HttpJson() {}

    static <T> T post(String url, Object cuerpo, Class<T> tipo) throws Exception {
        try (CloseableHttpClient client = ApiConfig.crearCliente()) {
            HttpPost request = new HttpPost(url);
            request.setEntity(EntityBuilder.create()
                    .setText(MAPPER.writeValueAsString(cuerpo))
                    .setContentType(ContentType.APPLICATION_JSON)
                    .build());

            try (ClassicHttpResponse response = client.execute(request)) {
                int code = response.getCode();
                String body = response.getEntity() == null ? "" : EntityUtils.toString(response.getEntity());
                if (code == 200 || code == 201) {
                    return MAPPER.readValue(body, tipo);
                }
                throw new Exception(mensajeDeError(code, body));
            }
        }
    }

    static JsonNode get(String ruta) throws Exception {
        try (var cliente = ApiConfig.crearCliente();
             var respuesta = cliente.execute(new org.apache.hc.client5.http.classic.methods.HttpGet(
                     ApiConfig.url(ruta.startsWith("/") ? ruta.substring(1) : ruta)))) {
            if (respuesta.getCode() != 200) throw new Exception(mensajeDeError(respuesta.getCode(), ""));
            return MAPPER.readTree(EntityUtils.toString(respuesta.getEntity()));
        }
    }

    private static String mensajeDeError(int code, String body) {
        try {
            JsonNode n = MAPPER.readTree(body);
            for (String k : new String[]{"message", "mensaje", "error"}) {
                if (n.hasNonNull(k) && n.get(k).isTextual()) return n.get(k).asText();
            }
        } catch (Exception ignorado) { /* el cuerpo no es JSON */ }
        return switch (code) {
            case 400 -> "Solicitud inválida.";
            case 401 -> "Usuario o contraseña incorrectos (HTTP 401).";
            case 403 -> "No tienes permiso para esta acción.";
            case 409 -> "La operación entra en conflicto con los datos actuales.";
            default -> "Error del servidor. Código HTTP: " + code;
        };
    }
}
