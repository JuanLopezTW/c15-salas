package co.eci.c15.salas.domain;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

class ConfigurarSalaTest {

    @Test
    void unaSalaNuevaEsDeDosEquiposDeDosYDuraCincoMinutos() {
        Sala sala = Sala.crear("Sala 1", "anfitrion-1");
        assertEquals(2, sala.getNumEquipos());
        assertEquals(2, sala.getJugadoresPorEquipo());
        assertEquals(4, sala.getCupoTotal());
        assertEquals(Duration.ofMinutes(5), sala.getDuracion());
    }

    @Test
    void elAnfitrionEligeCuantoDuraLaPartida() {
        for (int minutos : Sala.DURACIONES_MINUTOS) {
            assertEquals(Duration.ofMinutes(minutos), Sala.crear("Sala", "host", minutos).getDuracion());
        }
        assertEquals(Duration.ofMinutes(5), Sala.crear("Sala", "host", null).getDuracion());
    }

    @Test
    void unaDuracionQueNoEstaEnLaListaEsInvalida() {
        assertThrows(ConfiguracionInvalidaException.class, () -> Sala.crear("Sala", "host", 7));
        assertThrows(ConfiguracionInvalidaException.class, () -> Sala.crear("Sala", "host", 30));
        assertThrows(ConfiguracionInvalidaException.class, () -> Sala.crear("Sala", "host", 0));
    }

    @Test
    void anfitrionPuedeConfigurarDentroDeLosTopes() {
        Sala sala = Sala.crear("Sala 1", "anfitrion-1");
        sala.configurar("anfitrion-1", 2, 2);
        assertEquals(2, sala.getNumEquipos());
        assertEquals(2, sala.getJugadoresPorEquipo());
    }

    @Test
    void noAnfitrionNoPuedeCambiarConfiguracion() {
        Sala sala = Sala.crear("Sala 1", "anfitrion-1");
        assertThrows(NoEsAnfitrionException.class, () -> sala.configurar("otro-user", 2, 2));
    }

    @Test
    void menosDeDosEquiposEsInvalido() {
        Sala sala = Sala.crear("Sala 1", "anfitrion-1");
        assertThrows(ConfiguracionInvalidaException.class, () -> sala.configurar("anfitrion-1", 0, 2));
        assertThrows(ConfiguracionInvalidaException.class, () -> sala.configurar("anfitrion-1", 1, 2));
    }

    @Test
    void masDeDosEquiposEsInvalido() {
        Sala sala = Sala.crear("Sala 1", "anfitrion-1");
        assertThrows(ConfiguracionInvalidaException.class, () -> sala.configurar("anfitrion-1", 3, 2));
    }

    @Test
    void menosDeDosJugadoresPorEquipoEsInvalido() {
        Sala sala = Sala.crear("Sala 1", "anfitrion-1");
        assertThrows(ConfiguracionInvalidaException.class, () -> sala.configurar("anfitrion-1", 2, 0));
        assertThrows(ConfiguracionInvalidaException.class, () -> sala.configurar("anfitrion-1", 2, 1));
    }

    @Test
    void masDeDosJugadoresPorEquipoEsInvalido() {
        Sala sala = Sala.crear("Sala 1", "anfitrion-1");
        assertThrows(ConfiguracionInvalidaException.class, () -> sala.configurar("anfitrion-1", 2, 3));
    }

    @Test
    void configuracionPreviaSeConservaAnteValorInvalido() {
        Sala sala = Sala.crear("Sala 1", "anfitrion-1");
        assertThrows(ConfiguracionInvalidaException.class, () -> sala.configurar("anfitrion-1", 3, 5));
        assertEquals(2, sala.getNumEquipos());
        assertEquals(2, sala.getJugadoresPorEquipo());
    }

    @Test
    void noSePuedeConfigurarConPartidaIniciada() {
        Sala sala = Sala.crear("Sala 1", "anfitrion-1");
        sala.iniciarPartida();
        assertThrows(PartidaYaIniciadaException.class, () -> sala.configurar("anfitrion-1", 2, 2));
    }
}
