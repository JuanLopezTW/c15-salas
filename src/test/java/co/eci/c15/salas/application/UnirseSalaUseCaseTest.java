package co.eci.c15.salas.application;

import co.eci.c15.salas.domain.PartidaYaIniciadaException;
import co.eci.c15.salas.domain.Sala;
import co.eci.c15.salas.domain.SalaLlenaException;
import co.eci.c15.salas.domain.SalaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class UnirseSalaUseCaseTest {

    private SalaRepository repo;
    private ApplicationEventPublisher events;
    private UnirseSalaUseCase useCase;
    private Sala sala;

    @BeforeEach
    void setUp() {
        repo = mock(SalaRepository.class);
        events = mock(ApplicationEventPublisher.class);
        useCase = new UnirseSalaUseCase(repo, events);
        sala = Sala.crear("Sala 1", "anfitrion-1");
        when(repo.findById(sala.getId())).thenReturn(Optional.of(sala));
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void unionExitosaPublicaEvento() {
        SalaDto dto = useCase.ejecutar(sala.getId(), "user-1");
        assertEquals(1, dto.jugadoresActuales());
        verify(events).publishEvent(any(SalaActualizadaEvent.class));
    }

    @Test
    void salaLlenaLanzaExcepcion() {
        sala.configurar("anfitrion-1", 2, 2);
        for (int i = 1; i <= 4; i++) sala.unirJugador("user-" + i);
        assertThrows(SalaLlenaException.class, () -> useCase.ejecutar(sala.getId(), "user-5"));
        verify(repo, never()).save(any());
    }

    @Test
    void partidaIniciadaLanzaExcepcion() {
        sala.iniciarPartida();
        assertThrows(PartidaYaIniciadaException.class, () -> useCase.ejecutar(sala.getId(), "user-1"));
    }
}