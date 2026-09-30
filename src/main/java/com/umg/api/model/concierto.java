/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.umg.api.model;

/**
 *
 * @author adrir
 */
public class concierto {
    private int id_concierto;
    private int id_artista;
    private String titulo_evento;
    private String recinto;
    private String ciudad;
    private String estado;
    private String tipo_evento;
    private String fecha_concierto;
    private String horario;

    public concierto() {
    }

    public concierto(int id_concierto, int id_artista, String titulo_evento, String recinto, String ciudad, String estado, String tipo_evento, String fecha_concierto, String horario) {
        this.id_concierto = id_concierto;
        this.id_artista = id_artista;
        this.titulo_evento = titulo_evento;
        this.recinto = recinto;
        this.ciudad = ciudad;
        this.estado = estado;
        this.tipo_evento = tipo_evento;
        this.fecha_concierto = fecha_concierto;
        this.horario = horario;
    }

    public int getId_concierto() {
        return id_concierto;
    }

    public void setId_concierto(int id_concierto) {
        this.id_concierto = id_concierto;
    }

    public int getId_artista() {
        return id_artista;
    }

    public void setId_artista(int id_artista) {
        this.id_artista = id_artista;
    }

    public String getTitulo_evento() {
        return titulo_evento;
    }

    public void setTitulo_evento(String titulo_evento) {
        this.titulo_evento = titulo_evento;
    }

    public String getRecinto() {
        return recinto;
    }

    public void setRecinto(String recinto) {
        this.recinto = recinto;
    }

    public String getCiudad() {
        return ciudad;
    }

    public void setCiudad(String ciudad) {
        this.ciudad = ciudad;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public String getTipo_evento() {
        return tipo_evento;
    }

    public void setTipo_evento(String tipo_evento) {
        this.tipo_evento = tipo_evento;
    }

    public String getFecha_concierto() {
        return fecha_concierto;
    }

    public void setFecha_concierto(String fecha_concierto) {
        this.fecha_concierto = fecha_concierto;
    }

    public String getHorario() {
        return horario;
    }

    public void setHorario(String horario) {
        this.horario = horario;
    }

    @Override
    public String toString() {
        return "concierto{" + "id_concierto=" + id_concierto + ", id_artista=" + id_artista + ", titulo_evento=" + titulo_evento + ", recinto=" + recinto + ", ciudad=" + ciudad + ", estado=" + estado + ", tipo_evento=" + tipo_evento + ", fecha_concierto=" + fecha_concierto + ", horario=" + horario + '}';
    }
    
    
}
