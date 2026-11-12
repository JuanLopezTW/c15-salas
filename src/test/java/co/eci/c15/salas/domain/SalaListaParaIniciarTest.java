package co.eci.c15.salas.domain;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SalaListaParaIniciarTest {

    private Sala sala;
    private Equipo equipo1;
    private Equipo equipo2;
    private Equipo equipo3;

    @BeforeEach
    void setUp() {
        // La regla es general (sirve para salas guardadas antes del tope de 2 equipos de 2)
        sala = Sala.reconstituir("s1", "Sala", "host", Sala.Estado.DISPONIBLE, 3, 4, 5, List.of());
        equipo1 = Equipo.crear(sala.getId(), 1, 4);
        equipo2 = Equipo.crear(sala.getId(), 2, 4);
        equipo3 = Equipo.crear(sala.getId(), 3, 4);
        meter(equipo1, "ana", "beto");
        meter(equipo2, "caro", "dani");
    }

    private void meter(Equipo equipo, String... jugadores) {
        for (String j : jugadores) {
            sala.unirJugador(j);
            equipo.unirMiembro(j);
        }
    }

    private void todosListos(Equipo equipo) {
        equipo.getMiembros().forEach(equipo::marcarListo);
    }

    @Test
    void unaSalaDeDosEquiposDeDosArrancaConLosCuatroListos() {
        Sala nueva = Sala.crear("Nueva", "host");
        Equipo a = Equipo.crear(nueva.getId(), 1, nueva.getJugadoresPorEquipo());
        Equipo b = Equipo.crear(nueva.getId(), 2, nueva.getJugadoresPorEquipo());
        for (String j : List.of("ana", "beto")) { nueva.unirJugador(j); a.unirMiembro(j); a.marcarListo(j); }
        nueva.unirJugador("caro");
        b.unirMiembro("caro");
        b.marcarListo("caro");
        assertFalse(nueva.listaParaIniciar(List.of(a, b)), "un equipo de 1 no arranca");

        nueva.unirJugador("dani");
        b.unirMiembro("dani");
        b.marcarListo("dani");
        assertTrue(nueva.listaParaIniciar(List.of(a, b)));
        assertThrows(SalaLlenaException.class, () -> nueva.unirJugador("eli"));
    }

    @Test
    void listaConDosEquiposListosYElTerceroVacio() {
        todosListos(equipo1);
        todosListos(equipo2);
        assertTrue(sala.listaParaIniciar(List.of(equipo1, equipo2, equipo3)));
    }

    @Test
    void noEstaListaSiUnEquipoConJugadoresNoEstaListo() {
        todosListos(equipo1);
        equipo2.marcarListo("caro");
        assertFalse(sala.listaParaIniciar(List.of(equipo1, equipo2, equipo3)));
    }

    @Test
    void noEstaListaConUnSoloEquipoConJugadores() {
        Sala otra = Sala.crear("Otra", "host");
        Equipo unico = Equipo.crear(otra.getId(), 1, 4);
        for (String j : List.of("ana", "beto")) {
            otra.unirJugador(j);
            unico.unirMiembro(j);
            unico.marcarListo(j);
        }
        assertFalse(otra.listaParaIniciar(List.of(unico, Equipo.crear(otra.getId(), 2, 4))));
    }

    @Test
    void noEstaListaSiHayJugadoresSinEquipo() {
        todosListos(equipo1);
        todosListos(equipo2);
        sala.unirJugador("eli");
        assertFalse(sala.listaParaIniciar(List.of(equipo1, equipo2, equipo3)));
    }

    @Test
    void noEstaListaSiUnEquipoTieneUnSoloJugador() {
        todosListos(equipo1);
        todosListos(equipo2);
        meter(equipo3, "eli");
        equipo3.marcarListo("eli");
        assertFalse(sala.listaParaIniciar(List.of(equipo1, equipo2, equipo3)));
    }

    @Test
    void equiposDesbalanceadosSonValidos() {
        meter(equipo1, "eli", "fer");
        todosListos(equipo1);
        todosListos(equipo2);
        assertTrue(sala.listaParaIniciar(List.of(equipo1, equipo2, equipo3)));
    }

    @Test
    void conLaPartidaIniciadaNoVuelveAEstarLista() {
        todosListos(equipo1);
        todosListos(equipo2);
        sala.iniciarPartida();
        assertFalse(sala.listaParaIniciar(List.of(equipo1, equipo2, equipo3)));
    }
}
