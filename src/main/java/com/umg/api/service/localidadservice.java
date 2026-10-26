/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.umg.api.service;

import com.umg.api.config.ApiConfig;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.umg.api.model.localidad;

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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
/**
 *
 * @author adrir
 */
public class localidadservice {
    
     private static final String BASE_URL =
            ApiConfig.url("localidades");

    private static final ObjectMapper mapper = new ObjectMapper()
            .configure(
                    DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES,
                    false
            );

    // ==========================================================
    // GET - listar clientes
    // ==========================================================
    public List<localidad> getLocalidad() throws Exception {

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
                    new TypeReference<List<localidad>>() {}
            );
        }
    }

    // ==========================================================
    // POST - crear cliente
    // ==========================================================
    public localidad createLocalidad(localidad l) throws Exception {

        
        

        try (CloseableHttpClient client = ApiConfig.crearCliente()) {

            HttpPost request = new HttpPost(BASE_URL + "/create");

            String json = mapper.writeValueAsString(l);

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

            return mapper.readValue(is, localidad.class);
        }
    }

    // ==========================================================
    // PUT - modificar cliente
    // ==========================================================
    public localidad updateLocalidad(int id_localidad, localidad l) throws Exception {

        try (CloseableHttpClient client = ApiConfig.crearCliente()) {

            HttpPut request = new HttpPut(BASE_URL + "/update/" + id_localidad);

            String json = mapper.writeValueAsString(l);

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

            return mapper.readValue(is, localidad.class);
        }
    }

    // ==========================================================
    // DELETE - eliminar cliente
    // ==========================================================
    public void deleteLocalidad(int id_localidad) throws Exception {

        try (CloseableHttpClient client = ApiConfig.crearCliente()) {

            HttpDelete request = new HttpDelete(BASE_URL + "/delete/" + id_localidad);

            ClassicHttpResponse response = client.execute(request);

            int statusCode = response.getCode();

            if (statusCode != 200 && statusCode != 204) {
                throw new Exception("Error DELETE. Código HTTP: " + statusCode);
            }
        }
    }

    // ==========================================================
    // POST - generar los asientos de un concierto
    // La API crea 100 asientos (10 filas x 10) con su inventario y el mismo precio
    // ==========================================================
    public void generarAsientos(int id_concierto, float precio) throws Exception {
        Map<String, Object> cuerpo = new LinkedHashMap<>();
        cuerpo.put("id_concierto", id_concierto);
        cuerpo.put("precio", precio);
        cuerpo.put("status", true);      // true = asiento disponible
        HttpJson.post(BASE_URL + "/create", cuerpo, Object.class);
    }
}
