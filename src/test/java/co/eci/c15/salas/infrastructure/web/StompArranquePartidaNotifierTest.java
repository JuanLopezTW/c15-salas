package co.eci.c15.salas.infrastructure.web;

import co.eci.c15.salas.infrastructure.web.StompArranquePartidaNotifier.ArranqueMensaje;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import static org.mockito.Mockito.*;

class StompArranquePartidaNotifierTest {

    @Test
    void avisaQueLaSalaSeCerroEnElTopicDeInicio() {
        SimpMessagingTemplate messaging = mock(SimpMessagingTemplate.class);

        new StompArranquePartidaNotifier(messaging).cerrada("s1");

        verify(messaging).convertAndSend("/topic/salas/s1/inicio", new ArranqueMensaje("CERRADA", null, null));
    }
}
