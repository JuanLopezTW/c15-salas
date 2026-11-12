package co.eci.c15.salas.infrastructure.events;

import co.eci.c15.salas.application.ListarSalasUseCase;
import co.eci.c15.salas.application.SalaActualizadaEvent;
import co.eci.c15.salas.application.SalaCreadaEvent;
import co.eci.c15.salas.application.SalaDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.List;

import static org.mockito.Mockito.*;

class SalaCreadaStompListenerTest {

    private SimpMessagingTemplate messaging;
    private ListarSalasUseCase listar;
    private SalaCreadaStompListener listener;
    private List<SalaDto> catalogo;

    @BeforeEach
    void setUp() {
        messaging = mock(SimpMessagingTemplate.class);
        listar = mock(ListarSalasUseCase.class);
        listener = new SalaCreadaStompListener(messaging, listar);
        catalogo = List.of(new SalaDto("s1", "Sala", "host", "DISPONIBLE", 2, 2, 1, 4, 5));
        when(listar.ejecutar()).thenReturn(catalogo);
    }

    @Test
    void alCrearUnaSalaPublicaElCatalogoCompleto() {
        listener.onSalaCreada(new SalaCreadaEvent("s1", "Sala"));
        verify(messaging).convertAndSend("/topic/salas", catalogo);
    }

    @Test
    void alActualizarUnaSalaPublicaElCatalogoCompleto() {
        listener.onSalaActualizada(new SalaActualizadaEvent("s1"));
        verify(messaging).convertAndSend("/topic/salas", catalogo);
    }
}
