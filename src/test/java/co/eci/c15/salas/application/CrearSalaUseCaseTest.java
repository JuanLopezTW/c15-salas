package co.eci.c15.salas.application;

import co.eci.c15.salas.domain.Equipo;
import co.eci.c15.salas.domain.EquipoRepository;
import co.eci.c15.salas.domain.SalaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CrearSalaUseCaseTest {

    private SalaRepository repo;
    private EquipoRepository equipos;
    private ApplicationEventPublisher events;
    private CrearSalaUseCase useCase;

    @BeforeEach
    void setUp() {
        repo = mock(SalaRepository.class);
        equipos = mock(EquipoRepository.class);
        events = mock(ApplicationEventPublisher.class);
        useCase = new CrearSalaUseCase(repo, equipos, events);
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void creaYPublicaEvento() {
        SalaDto dto = useCase.ejecutar("Sala 1", "user-1", null);
        assertEquals("Sala 1", dto.nombre());
        assertEquals("DISPONIBLE", dto.estado());
        verify(events).publishEvent(any(SalaCreadaEvent.class));
    }

    @Test
    void creaLosEquiposPorDefectoVacios() {
        SalaDto dto = useCase.ejecutar("Sala 1", "user-1", null);

        ArgumentCaptor<Equipo> captor = ArgumentCaptor.forClass(Equipo.class);
        verify(equipos, times(dto.numEquipos())).save(captor.capture());
        List<Equipo> creados = captor.getAllValues();
        assertEquals(List.of(1, 2), creados.stream().map(Equipo::getNumero).toList());
        assertTrue(creados.stream().allMatch(e -> e.getSalaId().equals(dto.id())));
        assertTrue(creados.stream().allMatch(e -> e.getCupoMaximo() == dto.jugadoresPorEquipo()));
        assertTrue(creados.stream().allMatch(Equipo::isVacio));
    }

    @Test
    void nombreVacioLanzaExcepcion() {
        assertThrows(IllegalArgumentException.class, () -> useCase.ejecutar("", "user-1", null));
        verifyNoInteractions(events, equipos);
    }
}