package com.umg.api.service;

import com.umg.api.config.ApiConfig;
import com.umg.api.model.ventas;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Registrar una venta. La API lo hace en dos pasos:
 *   1. POST /api/ventas/create          -> crea la venta con total 0
 *   2. POST /api/detalle_ventas/create  -> agrega los asientos (id_inventario); la API calcula el total,
 *                                          descuenta el inventario y marca el asiento como ocupado
 * Cada id_inventario es un asiento, por eso cada boleto es de cantidad 1.
 */
public class checkoutservice {

    public record Item(int id_inventario, int cantidad) {}

    public ventas registrarVenta(int idVendedor, Integer idCliente, List<Item> items) throws Exception {
        if (idCliente == null) {
            throw new Exception("Selecciona un cliente para la venta.");
        }

        // 1. crear la venta
        ventas nueva = new ventas(0, idVendedor, Instant.now().toString(), 0);
        ventas creada = new ventasservice().createVentas(nueva);

        // 2. agregar los asientos a la venta
        List<Integer> ids = new ArrayList<>();
        for (Item it : items) {
            ids.add(it.id_inventario());
        }
        Map<String, Object> cuerpo = new LinkedHashMap<>();
        cuerpo.put("id_venta", creada.getId_venta());
        cuerpo.put("id_cliente", idCliente);
        cuerpo.put("id_inventario", ids);
        HttpJson.post(ApiConfig.url("detalle_ventas") + "/create", cuerpo, Object.class);

        // la API ya actualizó el total, se vuelve a pedir la venta para mostrarlo
        for (ventas v : new ventasservice().getVentas()) {
            if (v.getId_venta() == creada.getId_venta()) {
                return v;
            }
        }
        return creada;
    }
}
