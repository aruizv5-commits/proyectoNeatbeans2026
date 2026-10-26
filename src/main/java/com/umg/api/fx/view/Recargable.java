package com.umg.api.fx.view;

/** Vistas que vuelven a pedir sus datos al backend cada vez que se abre su pestaña. */
public interface Recargable {
    void recargar();
}
