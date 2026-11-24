package com.cenfotec.despacho.datos;

import com.cenfotec.despacho.modelo.Calle;
import com.cenfotec.despacho.modelo.Incidente;
import com.cenfotec.despacho.modelo.Interseccion;
import com.cenfotec.despacho.modelo.Llamada;
import com.cenfotec.despacho.modelo.Unidad;

import java.util.List;

public class DatosIniciales {
    private List<Unidad> unidades;
    private List<Incidente> incidentes;
    private List<Llamada> llamadas;
    private List<Interseccion> intersecciones;
    private List<Calle> calles;

    public List<Unidad> getUnidades() { return unidades; }
    public List<Incidente> getIncidentes() { return incidentes; }
    public List<Llamada> getLlamadas() { return llamadas; }
    public List<Interseccion> getIntersecciones() { return intersecciones; }
    public List<Calle> getCalles() { return calles; }
}
