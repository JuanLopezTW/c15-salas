package co.eci.c15.salas.domain;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SalaTest {

    @Test
    void creaSalaValida() {
        Sala sala = Sala.crear("Sala 1", "user-1");
        assertEquals("Sala 1", sala.getNombre());
        assertEquals("user-1", sala.getAnfitrionId());
        assertEquals(Sala.Estado.DISPONIBLE, sala.getEstado());
        assertTrue(sala.isDisponible());
    }

    @Test
    void rechazaNombreVacio() {
        assertThrows(IllegalArgumentException.class, () -> Sala.crear("", "user-1"));
        assertThrows(IllegalArgumentException.class, () -> Sala.crear(null, "user-1"));
    }

    @Test
    void iniciarPartidaCambiaEstado() {
        Sala sala = Sala.crear("Sala 1", "user-1");
        sala.iniciarPartida();
        assertEquals(Sala.Estado.EN_PARTIDA, sala.getEstado());
        assertFalse(sala.isDisponible());
    }

    @Test
    void retirarJugadorLoSacaSoloEnElLobby() {
        Sala sala = Sala.crear("Sala", "host");
        sala.unirJugador("ana");
        assertTrue(sala.esAnfitrion("host"));
        assertFalse(sala.esAnfitrion("ana"));

        sala.retirarJugador("ana");
        assertFalse(sala.contieneJugador("ana"));

        sala.unirJugador("ana");
        sala.iniciarPartida();
        assertThrows(PartidaYaIniciadaException.class, () -> sala.retirarJugador("ana"));
    }
}
