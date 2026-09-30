/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.umg.api.model;

/**
 *
 * @author adrir
 */
public class detalle_venta {
    
    private int id_detalle;
    private int id_venta;
    private int id_inventario;
    private int id_cliente;
    private int cantidad;
    private float precio_unitario;
    private float subtotal;

    public detalle_venta() {
    }

    public detalle_venta(int id_detalle, int id_venta, int id_inventario, int id_cliente, int cantidad, float precio_unitario, float subtotal) {
        this.id_detalle = id_detalle;
        this.id_venta = id_venta;
        this.id_inventario = id_inventario;
        this.id_cliente = id_cliente;
        this.cantidad = cantidad;
        this.precio_unitario = precio_unitario;
        this.subtotal = subtotal;
    }

    public int getId_detalle() {
        return id_detalle;
    }

    public void setId_detalle(int id_detalle) {
        this.id_detalle = id_detalle;
    }

    public int getId_venta() {
        return id_venta;
    }

    public void setId_venta(int id_venta) {
        this.id_venta = id_venta;
    }

    public int getId_inventario() {
        return id_inventario;
    }

    public void setId_inventario(int id_inventario) {
        this.id_inventario = id_inventario;
    }

    public int getId_cliente() {
        return id_cliente;
    }

    public void setId_cliente(int id_cliente) {
        this.id_cliente = id_cliente;
    }

    public int getCantidad() {
        return cantidad;
    }

    public void setCantidad(int cantidad) {
        this.cantidad = cantidad;
    }

    public float getPrecio_unitario() {
        return precio_unitario;
    }

    public void setPrecio_unitario(float precio_unitario) {
        this.precio_unitario = precio_unitario;
    }

    public float getSubtotal() {
        return subtotal;
    }

    public void setSubtotal(float subtotal) {
        this.subtotal = subtotal;
    }

    
    
    @Override
    public String toString() {
        return "detalle_venta{" + "id_detalle=" + id_detalle + ", id_venta=" + id_venta + ", id_inventario=" + id_inventario + ", id_cliente=" + id_cliente + ", cantidad=" + cantidad + ", precio_unitario=" + precio_unitario + ", subtotal=" + subtotal + '}';
    }
    
    
}
