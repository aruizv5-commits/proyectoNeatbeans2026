/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.umg.api.model;

/**
 *
 * @author adrir
 */
public class artista {
   
    private int id_artista;
    private String nombre_artistico;
    private String genero_musical;
    private String pais_origen;

    public artista() {
    }

    public artista(int id_artista, String nombre_artistico, String genero_musical, String pais_origen) {
        this.id_artista = id_artista;
        this.nombre_artistico = nombre_artistico;
        this.genero_musical = genero_musical;
        this.pais_origen = pais_origen;
    }

    public int getId_artista() {
        return id_artista;
    }

    public void setId_artista(int id_artista) {
        this.id_artista = id_artista;
    }

    public String getNombre_artistico() {
        return nombre_artistico;
    }

    public void setNombre_artistico(String nombre_artistico) {
        this.nombre_artistico = nombre_artistico;
    }

    public String getGenero_musical() {
        return genero_musical;
    }

    public void setGenero_musical(String genero_musical) {
        this.genero_musical = genero_musical;
    }

    public String getPais_origen() {
        return pais_origen;
    }

    public void setPais_origen(String pais_origen) {
        this.pais_origen = pais_origen;
    }

    @Override
    public String toString() {
        return "artista{" + "id_artista=" + id_artista + ", nombre_artistico=" + nombre_artistico + ", genero_musical=" + genero_musical + ", pais_origen=" + pais_origen + '}';
    }
    
    
}
