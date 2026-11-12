package co.eci.c15.salas.application;

import co.eci.c15.common.events.EventosEntreServicios;
import co.eci.c15.common.events.PartidaIniciadaEvent;
import co.eci.c15.salas.domain.Equipo;
import co.eci.c15.salas.domain.Sala;
import co.eci.c15.salas.domain.SalaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class IniciarPartidaUseCaseTest {

    private EquiposEnMemoria equipos;
    private SalaRepository salas;
    private ApplicationEventPublisher events;
    private EventosEntreServicios entreServicios;
    private IniciarPartidaUseCase useCase;
    private Sala sala;
    private Equipo equipo1;
    private Equipo equipo2;

    @BeforeEach
    void setUp() {
        equipos = new EquiposEnMemoria();
        salas = mock(SalaRepository.class);
        events = mock(ApplicationEventPublisher.class);
        entreServicios = mock(EventosEntreServicios.class);
        useCase = new IniciarPartidaUseCase(salas, equipos, events, entreServicios);
        sala = Sala.crear("Sala", "host", 10);
        when(salas.findById(sala.getId())).thenReturn(Optional.of(sala));
        when(salas.findById("no-existe")).thenReturn(Optional.empty());
        equipo1 = equipos.save(Equipo.crear(sala.getId(), 1, 2));
        equipo2 = equipos.save(Equipo.crear(sala.getId(), 2, 2));
        listos(equipo1, "beto", "ana");
        listos(equipo2, "caro", "dani");
    }

    private void listos(Equipo equipo, String... jugadores) {
        for (String j : jugadores) {
            sala.unirJugador(j);
            equipo.unirMiembro(j);
            equipo.marcarListo(j);
        }
    }

    @Test
    void iniciaLaPartidaYPublicaElEventoConLosEquiposYLaDuracionElegida() {
        Optional<String> matchId = useCase.iniciar(sala.getId());

        assertEquals(Optional.of(sala.getId()), matchId);
        assertEquals(Sala.Estado.EN_PARTIDA, sala.getEstado());
        verify(salas).save(sala);

        ArgumentCaptor<PartidaIniciadaEvent> evento = ArgumentCaptor.forClass(PartidaIniciadaEvent.class);
        verify(entreServicios).publicar(evento.capture());
        assertEquals(sala.getId(), evento.getValue().matchId());
        assertEquals(Map.of(equipo1.getId(), List.of("ana", "beto"), equipo2.getId(), List.of("caro", "dani")),
                evento.getValue().equipos());
        assertEquals(Duration.ofMinutes(10), evento.getValue().duracion());
        verify(events).publishEvent(new SalaActualizadaEvent(sala.getId()));
    }

    @Test
    void siLaSalaYaNoEstaListaNoInicia() {
        equipo2.desmarcarListo("dani");

        assertTrue(useCase.iniciar(sala.getId()).isEmpty());
        assertEquals(Sala.Estado.DISPONIBLE, sala.getEstado());
        verify(salas, never()).save(any());
        verifyNoInteractions(events, entreServicios);
    }

    @Test
    void noIniciaDosVeces() {
        useCase.iniciar(sala.getId());
        clearInvocations(events, entreServicios);

        assertTrue(useCase.iniciar(sala.getId()).isEmpty());
        verifyNoInteractions(events, entreServicios);
    }

    @Test
    void salaInexistenteNoInicia() {
        assertTrue(useCase.iniciar("no-existe").isEmpty());
        assertFalse(useCase.listaParaIniciar("no-existe"));
    }

    @Test
    void listaParaIniciarRevisaLosEquiposDeLaSala() {
        assertTrue(useCase.listaParaIniciar(sala.getId()));
        equipo1.desmarcarListo("ana");
        assertFalse(useCase.listaParaIniciar(sala.getId()));
    }
}
