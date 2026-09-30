/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.umg.api.model;

/**
 *
 * @author adrir
 */
public class ventas {
    
    private int id_venta;
    private int id_vendedor;
    private String fecha_venta;
    private float total_venta;

    public ventas() {
    }

    public ventas(int id_venta, int id_vendedor, String fecha_venta, float total_venta) {
        this.id_venta = id_venta;
        this.id_vendedor = id_vendedor;
        this.fecha_venta = fecha_venta;
        this.total_venta = total_venta;
    }

    public int getId_venta() {
        return id_venta;
    }

    public void setId_venta(int id_venta) {
        this.id_venta = id_venta;
    }

    public int getId_vendedor() {
        return id_vendedor;
    }

    public void setId_vendedor(int id_vendedor) {
        this.id_vendedor = id_vendedor;
    }

    public String getFecha_venta() {
        return fecha_venta;
    }

    public void setFecha_venta(String fecha_venta) {
        this.fecha_venta = fecha_venta;
    }

    public float getTotal_venta() {
        return total_venta;
    }

    public void setTotal_venta(float total_venta) {
        this.total_venta = total_venta;
    }

    @Override
    public String toString() {
        return "ventas{" + "id_venta=" + id_venta + ", id_vendedor=" + id_vendedor + ", fecha_venta=" + fecha_venta + ", total_venta=" + total_venta + '}';
    }
}
