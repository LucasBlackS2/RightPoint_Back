package br.com.checkpoint.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GeoUtilsTest {

    @Test
    void mesmoPontoTemDistanciaZero() {
        assertEquals(0.0, GeoUtils.distanciaMetros(-8.2835, -35.9761, -8.2835, -35.9761), 0.001);
    }

    @Test
    void umGrauDeLongitudeNoEquadorTemCercaDe111Km() {
        assertEquals(111_195, GeoUtils.distanciaMetros(0, 0, 0, 1), 50);
    }

    @Test
    void pontosProximosEmCaruaru() {
        // ~0,001 grau de latitude ≈ 111 m
        double d = GeoUtils.distanciaMetros(-8.2835, -35.9761, -8.2845, -35.9761);
        assertEquals(111, d, 2);
    }
}
