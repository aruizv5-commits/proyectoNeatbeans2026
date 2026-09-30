/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.umg.api.model;

/**
 *
 * @author adrir
 */
public class usuario {
    private int id_usuario;
    private String nombre_usuario;
    private String contrasena_hash;
    private String nombre_completo;
    private String rol;
    private String fecha_creacion;
    private boolean status;

    public usuario() {
    }

    public usuario(int id_usuario, String nombre_usuario, String contrasena_hash, String nombre_completo, String rol, String fecha_creacion, boolean status) {
        this.id_usuario = id_usuario;
        this.nombre_usuario = nombre_usuario;
        this.contrasena_hash = contrasena_hash;
        this.nombre_completo = nombre_completo;
        this.rol = rol;
        this.fecha_creacion = fecha_creacion;
        this.status = status;
    }

    public int getId_usuario() {
        return id_usuario;
    }

    public void setId_usuario(int id_usuario) {
        this.id_usuario = id_usuario;
    }

    public String getNombre_usuario() {
        return nombre_usuario;
    }

    public void setNombre_usuario(String nombre_usuario) {
        this.nombre_usuario = nombre_usuario;
    }

    public String getContrasena_hash() {
        return contrasena_hash;
    }

    public void setContrasena_hash(String contrasena_hash) {
        this.contrasena_hash = contrasena_hash;
    }

    public String getNombre_completo() {
        return nombre_completo;
    }

    public void setNombre_completo(String nombre_completo) {
        this.nombre_completo = nombre_completo;
    }

    public String getRol() {
        return rol;
    }

    public void setRol(String rol) {
        this.rol = rol;
    }

    public String getFecha_creacion() {
        return fecha_creacion;
    }

    public void setFecha_creacion(String fecha_creacion) {
        this.fecha_creacion = fecha_creacion;
    }

    public boolean isStatus() {
        return status;
    }

    public void setStatus(boolean status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return "usuario{" + "id_usuario=" + id_usuario + ", nombre_usuario=" + nombre_usuario + ", contrasena_hash=" + contrasena_hash + ", nombre_completo=" + nombre_completo + ", rol=" + rol + ", fecha_creacion=" + fecha_creacion + ", status=" + status + '}';
    }
    
    
}
