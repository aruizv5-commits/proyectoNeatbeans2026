/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.umg.api.model;

/**
 *
 * @author adrir
 */
public class localidad {
    
    private int id_localidad;
    private int nombre;

    public localidad() {
    }

    public localidad(int id_localidad, int nombre) {
        this.id_localidad = id_localidad;
        this.nombre = nombre;
    }

    public int getId_localidad() {
        return id_localidad;
    }

    public void setId_localidad(int id_localidad) {
        this.id_localidad = id_localidad;
    }

    public int getNombre() {
        return nombre;
    }

    public void setNombre(int nombre) {
        this.nombre = nombre;
    }

    @Override
    public String toString() {
        return "localidad{" + "id_localidad=" + id_localidad + ", nombre=" + nombre + '}';
    }
    
    
}
