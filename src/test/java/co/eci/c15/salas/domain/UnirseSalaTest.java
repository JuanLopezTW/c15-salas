package co.eci.c15.salas.domain;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class UnirseSalaTest {

    @Test
    void jugadorSeUneExitosamente() {
        Sala sala = Sala.crear("Sala 1", "anfitrion-1");
        sala.unirJugador("user-1");
        assertTrue(sala.getJugadores().contains("user-1"));
    }

    @Test
    void salaLlenaLanzaExcepcion() {
        Sala sala = Sala.crear("Sala 1", "anfitrion-1");
        sala.configurar("anfitrion-1", 2, 2); // cupo = 4
        for (int i = 1; i <= 4; i++) sala.unirJugador("user-" + i);
        assertThrows(SalaLlenaException.class, () -> sala.unirJugador("user-5"));
    }

    @Test
    void jugadorQueYaEstaNoCuentaComoNuevoAunqueLaSalaEsteLlena() {
        Sala sala = Sala.crear("Sala 1", "anfitrion-1");
        sala.configurar("anfitrion-1", 2, 2);
        for (int i = 1; i <= 4; i++) sala.unirJugador("user-" + i);
        assertDoesNotThrow(() -> sala.unirJugador("user-1"));
    }

    @Test
    void partidaIniciadaNoPermiteUnirse() {
        Sala sala = Sala.crear("Sala 1", "anfitrion-1");
        sala.iniciarPartida();
        assertThrows(PartidaYaIniciadaException.class, () -> sala.unirJugador("user-1"));
    }

    @Test
    void unirseDoVecesNoAumentaContador() {
        Sala sala = Sala.crear("Sala 1", "anfitrion-1");
        sala.unirJugador("user-1");
        sala.unirJugador("user-1");
        assertEquals(1, sala.getJugadores().size());
    }

    @Test
    void userIdVacioEsInvalido() {
        Sala sala = Sala.crear("Sala 1", "anfitrion-1");
        assertThrows(IllegalArgumentException.class, () -> sala.unirJugador(null));
        assertThrows(IllegalArgumentException.class, () -> sala.unirJugador(" "));
    }
}