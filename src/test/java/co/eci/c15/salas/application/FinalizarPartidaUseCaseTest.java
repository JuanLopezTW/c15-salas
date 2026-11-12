package co.eci.c15.salas.application;

import co.eci.c15.common.events.EventosEntreServicios;
import co.eci.c15.common.events.SalaCerradaEvent;
import co.eci.c15.salas.domain.Equipo;
import co.eci.c15.salas.domain.Sala;
import co.eci.c15.salas.domain.SalaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class FinalizarPartidaUseCaseTest {

    private EquiposEnMemoria equipos;
    private SalaRepository salas;
    private ApplicationEventPublisher events;
    private EventosEntreServicios entreServicios;
    private FinalizarPartidaUseCase useCase;
    private Sala sala;

    @BeforeEach
    void setUp() {
        equipos = new EquiposEnMemoria();
        salas = mock(SalaRepository.class);
        events = mock(ApplicationEventPublisher.class);
        entreServicios = mock(EventosEntreServicios.class);
        useCase = new FinalizarPartidaUseCase(salas, equipos, events, entreServicios);
        sala = Sala.crear("Sala", "host");
        sala.iniciarPartida();
        when(salas.findById(sala.getId())).thenReturn(Optional.of(sala));
        when(salas.findById("no-existe")).thenReturn(Optional.empty());
    }

    @Test
    void borraLaSalaYSusEquiposYAvisa() {
        equipos.save(Equipo.crear(sala.getId(), 1, 4));
        equipos.save(Equipo.crear(sala.getId(), 2, 4));
        Equipo deOtraSala = equipos.save(Equipo.crear("otra-sala", 1, 4));

        assertTrue(useCase.ejecutar(sala.getId()));

        verify(salas).delete(sala.getId());
        assertTrue(equipos.findBySalaId(sala.getId()).isEmpty());
        assertTrue(equipos.findById(deOtraSala.getId()).isPresent());
        verify(events).publishEvent(new SalaActualizadaEvent(sala.getId()));
        verify(entreServicios).publicar(new SalaCerradaEvent(sala.getId()));
    }

    @Test
    void salaInexistenteNoHaceNada() {
        assertFalse(useCase.ejecutar("no-existe"));

        verify(salas, never()).delete(any());
        verifyNoInteractions(events, entreServicios);
    }
}
