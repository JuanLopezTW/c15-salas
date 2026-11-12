package co.eci.c15.salas.domain;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class EquipoTest {

    @Test
    void miembroSeUneExitosamente() {
        Equipo equipo = Equipo.crear("sala-1", 1, 3);
        equipo.unirMiembro("user-1");
        assertTrue(equipo.getMiembros().contains("user-1"));
        assertFalse(equipo.isFull());
    }

    @Test
    void equipoLlenoLanzaExcepcion() {
        Equipo equipo = Equipo.crear("sala-1", 1, 2);
        equipo.unirMiembro("user-1");
        equipo.unirMiembro("user-2");
        assertTrue(equipo.isFull());
        assertThrows(EquipoLlenoException.class, () -> equipo.unirMiembro("user-3"));
    }

    @Test
    void unirseDoVecesNoAumentaContador() {
        Equipo equipo = Equipo.crear("sala-1", 1, 3);
        equipo.unirMiembro("user-1");
        equipo.unirMiembro("user-1");
        assertEquals(1, equipo.getMiembros().size());
    }

    @Test
    void quitarMiembroLiberaCupo() {
        Equipo equipo = Equipo.crear("sala-1", 1, 2);
        equipo.unirMiembro("user-1");
        equipo.unirMiembro("user-2");
        equipo.quitarMiembro("user-1");
        assertFalse(equipo.isFull());
        assertFalse(equipo.contieneMiembro("user-1"));
    }

    @Test
    void cambiarCupoActualizaElMaximo() {
        Equipo equipo = Equipo.crear("sala-1", 1, 4);
        equipo.unirMiembro("user-1");
        equipo.cambiarCupo(2);
        assertEquals(2, equipo.getCupoMaximo());
    }

    @Test
    void cupoNoPuedeBajarDeLosMiembrosActuales() {
        Equipo equipo = Equipo.crear("sala-1", 1, 4);
        equipo.unirMiembro("user-1");
        equipo.unirMiembro("user-2");
        equipo.unirMiembro("user-3");
        assertThrows(ConfiguracionInvalidaException.class, () -> equipo.cambiarCupo(2));
        assertEquals(4, equipo.getCupoMaximo());
    }
}