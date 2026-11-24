package com.cenfotec.despacho.componentes;

import com.cenfotec.despacho.mensajeria.BusMensajes;

/**
 * Contrato de un componente del sistema. Los componentes no se conocen entre sí:
 * solo se conectan al bus, reciben mensajes y, si hace falta, publican otros.
 */
public interface Componente {

    /** Nombre con el que se suscribe al bus y aparece en las estadísticas. */
    String nombre();

    /** Se suscribe al bus; desde aquí empieza a recibir mensajes en su propio hilo. */
    void iniciar(BusMensajes bus);

    /** Resumen de una línea del estado del componente, para la interfaz. */
    String estado();
}
