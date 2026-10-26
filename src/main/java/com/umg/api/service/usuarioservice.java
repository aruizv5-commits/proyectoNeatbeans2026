/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.umg.api.service;

import com.umg.api.config.ApiConfig;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.umg.api.model.usuario;

import org.apache.hc.client5.http.classic.methods.HttpDelete;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.classic.methods.HttpPut;

import org.apache.hc.client5.http.entity.EntityBuilder;

import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;

import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.ContentType;

import java.io.InputStream;
import java.util.List;
/**
 *
 * @author adrir
 */
public class usuarioservice {
    
     private static final String BASE_URL =
            ApiConfig.url("usuario");

    private static final ObjectMapper mapper = new ObjectMapper()
            .configure(
                    DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES,
                    false
            );

    // ==========================================================
    // GET - listar clientes
    // ==========================================================
    public List<usuario> getUsuario() throws Exception {

        try (CloseableHttpClient client = ApiConfig.crearCliente()) {

            HttpGet request = new HttpGet(BASE_URL+"/");

            ClassicHttpResponse response = client.execute(request);

            int statusCode = response.getCode();

            if (statusCode != 200) {
                throw new Exception("Error GET. Código HTTP: " + statusCode);
            }

            InputStream is = response.getEntity().getContent();

            return mapper.readValue(
                    is,
                    new TypeReference<List<usuario>>() {}
            );
        }
    }

    // ==========================================================
    // POST - crear cliente
    // ==========================================================
    public usuario createUsuario(usuario u) throws Exception {

        
        u.setFecha_creacion(java.time.Instant.now().toString());

        try (CloseableHttpClient client = ApiConfig.crearCliente()) {

            HttpPost request = new HttpPost(BASE_URL + "/create");

            String json = mapper.writeValueAsString(u);

            request.setEntity(
                    EntityBuilder.create()
                            .setText(json)
                            .setContentType(ContentType.APPLICATION_JSON)
                            .build()
            );

            ClassicHttpResponse response = client.execute(request);

            int statusCode = response.getCode();

            if (statusCode != 200 && statusCode != 201) {
                throw new Exception("Error POST. Código HTTP: " + statusCode);
            }

            InputStream is = response.getEntity().getContent();

            return mapper.readValue(is, usuario.class);
        }
    }

    // ==========================================================
    // PUT - modificar cliente
    // ==========================================================
    public usuario updateUsuario(int id_usuario, usuario u) throws Exception {

        try (CloseableHttpClient client = ApiConfig.crearCliente()) {

            HttpPut request = new HttpPut(BASE_URL + "/update/" + id_usuario);

            String json = mapper.writeValueAsString(u);

            request.setEntity(
                    EntityBuilder.create()
                            .setText(json)
                            .setContentType(ContentType.APPLICATION_JSON)
                            .build()
            );

            ClassicHttpResponse response = client.execute(request);

            int statusCode = response.getCode();

            if (statusCode != 200) {
                throw new Exception("Error PUT. Código HTTP: " + statusCode);
            }

            InputStream is = response.getEntity().getContent();

            return mapper.readValue(is, usuario.class);
        }
    }

    // ==========================================================
    // DELETE - eliminar cliente
    // ==========================================================
    public void deleteUsuario(int id_usuario) throws Exception {

        try (CloseableHttpClient client = ApiConfig.crearCliente()) {

            HttpDelete request = new HttpDelete(BASE_URL + "/delete/" + id_usuario);

            ClassicHttpResponse response = client.execute(request);

            int statusCode = response.getCode();

            if (statusCode != 200 && statusCode != 204) {
                throw new Exception("Error DELETE. Código HTTP: " + statusCode);
            }
        }
    }
}
