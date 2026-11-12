package co.eci.c15.salas.infrastructure.events;

import co.eci.c15.common.events.MatchFinishedEvent;
import co.eci.c15.salas.application.FinalizarPartidaUseCase;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.mockito.Mockito.*;

class PartidaTerminadaListenerTest {

    @Test
    void alTerminarLaPartidaFinalizaSuSala() {
        FinalizarPartidaUseCase finalizar = mock(FinalizarPartidaUseCase.class);
        PartidaTerminadaListener listener = new PartidaTerminadaListener(finalizar);

        listener.onPartidaTerminada(new MatchFinishedEvent("sala-1", "TIME_UP", null, Map.of(), Map.of()));

        verify(finalizar).ejecutar("sala-1");
    }
}
