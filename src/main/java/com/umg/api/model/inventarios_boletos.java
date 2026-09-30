/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.umg.api.model;

/**
 *
 * @author adrir
 */
public class inventarios_boletos {
    
    private int id_inventario;
    private int id_concierto;
    private int id_localidad;
    private float precio;
    private int cantidad_total;
    private int cantidad_disponible;

    public inventarios_boletos() {
    }

    public inventarios_boletos(int id_inventario, int id_concierto, int id_localidad, float precio, int cantidad_total, int cantidad_disponible) {
        this.id_inventario = id_inventario;
        this.id_concierto = id_concierto;
        this.id_localidad = id_localidad;
        this.precio = precio;
        this.cantidad_total = cantidad_total;
        this.cantidad_disponible = cantidad_disponible;
    }

    public int getId_inventario() {
        return id_inventario;
    }

    public void setId_inventario(int id_inventario) {
        this.id_inventario = id_inventario;
    }

    public int getId_concierto() {
        return id_concierto;
    }

    public void setId_concierto(int id_concierto) {
        this.id_concierto = id_concierto;
    }

    public int getId_localidad() {
        return id_localidad;
    }

    public void setId_localidad(int id_localidad) {
        this.id_localidad = id_localidad;
    }

    public float getPrecio() {
        return precio;
    }

    public void setPrecio(float precio) {
        this.precio = precio;
    }

    public int getCantidad_total() {
        return cantidad_total;
    }

    public void setCantidad_total(int cantidad_total) {
        this.cantidad_total = cantidad_total;
    }

    public int getCantidad_disponible() {
        return cantidad_disponible;
    }

    public void setCantidad_disponible(int cantidad_disponible) {
        this.cantidad_disponible = cantidad_disponible;
    }

    @Override
    public String toString() {
        return "inventarios_boletos{" + "id_inventario=" + id_inventario + ", id_concierto=" + id_concierto + ", id_localidad=" + id_localidad + ", precio=" + precio + ", cantidad_total=" + cantidad_total + ", cantidad_disponible=" + cantidad_disponible + '}';
    }
    
    
    
    
}
