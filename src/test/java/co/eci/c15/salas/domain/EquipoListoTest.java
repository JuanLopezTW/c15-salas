package co.eci.c15.salas.domain;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class EquipoListoTest {

    private Equipo equipo;

    @BeforeEach
    void setUp() {
        equipo = Equipo.crear("sala-1", 1, 4);
        equipo.unirMiembro("ana");
        equipo.unirMiembro("beto");
    }

    @Test
    void miembroMarcaYDesmarcaListo() {
        equipo.marcarListo("ana");
        assertTrue(equipo.isMiembroListo("ana"));
        assertEquals(Set.of("ana"), equipo.getListos());

        equipo.desmarcarListo("ana");
        assertFalse(equipo.isMiembroListo("ana"));
    }

    @Test
    void quienNoEsMiembroNoPuedeMarcarListo() {
        assertThrows(JugadorSinEquipoException.class, () -> equipo.marcarListo("intruso"));
        assertThrows(JugadorSinEquipoException.class, () -> equipo.desmarcarListo("intruso"));
    }

    @Test
    void equipoListoCuandoTodosSusMiembrosLoEstan() {
        equipo.marcarListo("ana");
        assertFalse(equipo.isListo());

        equipo.marcarListo("beto");
        assertTrue(equipo.isListo());
    }

    @Test
    void unNuevoMiembroSinMarcarQuitaElListoDelEquipo() {
        equipo.marcarListo("ana");
        equipo.marcarListo("beto");
        equipo.unirMiembro("caro");
        assertFalse(equipo.isListo());
    }

    @Test
    void conMenosDelMinimoDeJugadoresNoEstaListo() {
        Equipo solo = Equipo.crear("sala-1", 2, 4);
        solo.unirMiembro("ana");
        solo.marcarListo("ana");
        assertFalse(solo.isListo());
    }

    @Test
    void equipoVacioNoEstaListo() {
        assertFalse(Equipo.crear("sala-1", 2, 4).isListo());
    }

    @Test
    void salirDelEquipoQuitaElListo() {
        equipo.marcarListo("ana");
        equipo.quitarMiembro("ana");
        assertFalse(equipo.isMiembroListo("ana"));

        equipo.unirMiembro("ana");
        assertFalse(equipo.isMiembroListo("ana"));
    }

    @Test
    void reconstituirIgnoraListosQueNoSonMiembros() {
        Equipo leido = Equipo.reconstituir("e1", "sala-1", 1, 4, List.of("ana", "beto"), List.of("ana", "fantasma"));
        assertEquals(Set.of("ana"), leido.getListos());
    }
}
