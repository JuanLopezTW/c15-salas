package co.eci.c15.salas.application;

import co.eci.c15.salas.domain.Equipo;
import co.eci.c15.salas.domain.JugadorSinEquipoException;
import co.eci.c15.salas.domain.PartidaYaIniciadaException;
import co.eci.c15.salas.domain.Sala;
import co.eci.c15.salas.domain.SalaNoEncontradaException;
import co.eci.c15.salas.domain.SalaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MarcarListoUseCaseTest {

    private EquiposEnMemoria equipos;
    private SalaRepository salas;
    private ApplicationEventPublisher events;
    private MarcarListoUseCase useCase;
    private Sala sala;
    private Equipo equipo1;

    @BeforeEach
    void setUp() {
        equipos = new EquiposEnMemoria();
        salas = mock(SalaRepository.class);
        events = mock(ApplicationEventPublisher.class);
        useCase = new MarcarListoUseCase(salas, equipos, events);
        sala = Sala.crear("Sala", "host");
        for (String u : List.of("ana", "beto", "caro")) sala.unirJugador(u);
        when(salas.findById(sala.getId())).thenReturn(Optional.of(sala));
        when(salas.findById("no-existe")).thenReturn(Optional.empty());
        equipo1 = equipos.save(Equipo.crear(sala.getId(), 1, 4));
        equipos.save(Equipo.crear(sala.getId(), 2, 4));
        equipo1.unirMiembro("ana");
        equipo1.unirMiembro("beto");
    }

    @Test
    void marcarListoSeVeEnElDetalleYPublicaEvento() {
        SalaDetalleDto dto = useCase.marcar(sala.getId(), "ana");

        assertEquals(Set.of("ana"), dto.equipos().get(0).listos());
        assertFalse(dto.equipos().get(0).listo());
        verify(events).publishEvent(new SalaActualizadaEvent(sala.getId()));
    }

    @Test
    void cuandoTodosMarcanElEquipoQuedaListo() {
        useCase.marcar(sala.getId(), "ana");
        SalaDetalleDto dto = useCase.marcar(sala.getId(), "beto");

        assertTrue(dto.equipos().get(0).listo());
        assertFalse(dto.equipos().get(1).listo());
    }

    @Test
    void desmarcarQuitaElListo() {
        useCase.marcar(sala.getId(), "ana");
        useCase.marcar(sala.getId(), "beto");

        SalaDetalleDto dto = useCase.desmarcar(sala.getId(), "beto");

        assertEquals(Set.of("ana"), dto.equipos().get(0).listos());
        assertFalse(dto.equipos().get(0).listo());
    }

    @Test
    void repetirMarcarODesmarcarNoPublicaOtroEvento() {
        useCase.marcar(sala.getId(), "ana");
        useCase.marcar(sala.getId(), "ana");
        useCase.desmarcar(sala.getId(), "beto");
        verify(events, times(1)).publishEvent(any(SalaActualizadaEvent.class));
    }

    @Test
    void jugadorSinEquipoNoPuedeMarcarse() {
        assertThrows(JugadorSinEquipoException.class, () -> useCase.marcar(sala.getId(), "caro"));
        assertThrows(JugadorSinEquipoException.class, () -> useCase.desmarcar(sala.getId(), "caro"));
        verifyNoInteractions(events);
    }

    @Test
    void salaInexistenteLanzaExcepcion() {
        assertThrows(SalaNoEncontradaException.class, () -> useCase.marcar("no-existe", "ana"));
    }

    @Test
    void conPartidaIniciadaNoSePuedeCambiarElListo() {
        sala.iniciarPartida();
        assertThrows(PartidaYaIniciadaException.class, () -> useCase.marcar(sala.getId(), "ana"));
    }

    @Test
    void userIdVacioEsInvalido() {
        assertThrows(IllegalArgumentException.class, () -> useCase.marcar(sala.getId(), " "));
        assertThrows(IllegalArgumentException.class, () -> useCase.desmarcar(sala.getId(), null));
    }
}
