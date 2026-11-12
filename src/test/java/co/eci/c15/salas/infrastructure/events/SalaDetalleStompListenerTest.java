package co.eci.c15.salas.infrastructure.events;

import co.eci.c15.salas.application.SalaActualizadaEvent;
import co.eci.c15.salas.application.SalaDetalleDto;
import co.eci.c15.salas.application.VerSalaUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class SalaDetalleStompListenerTest {

    private SimpMessagingTemplate messaging;
    private VerSalaUseCase verSala;
    private SalaDetalleStompListener listener;

    @BeforeEach
    void setUp() {
        messaging = mock(SimpMessagingTemplate.class);
        verSala = mock(VerSalaUseCase.class);
        listener = new SalaDetalleStompListener(messaging, verSala);
    }

    @Test
    void publicaElDetalleEnElTopicDeLaSala() {
        SalaDetalleDto detalle = new SalaDetalleDto("s1", "Sala", "host", "DISPONIBLE", 2, 2, 4, 5,
                List.of("host"), List.of("host"), List.of());
        when(verSala.buscarAbierta("s1")).thenReturn(Optional.of(detalle));

        listener.onSalaActualizada(new SalaActualizadaEvent("s1"));

        verify(messaging).convertAndSend("/topic/salas/s1", detalle);
    }

    @Test
    void noPublicaSiLaSalaEstaCerradaOEsInexistente() {
        when(verSala.buscarAbierta("s1")).thenReturn(Optional.empty());

        listener.onSalaActualizada(new SalaActualizadaEvent("s1"));

        verify(messaging, never()).convertAndSend(anyString(), any(Object.class));
    }
}
