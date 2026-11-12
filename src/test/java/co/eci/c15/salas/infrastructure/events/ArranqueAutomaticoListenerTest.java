package co.eci.c15.salas.infrastructure.events;

import co.eci.c15.salas.application.CuentaRegresivaPartida;
import co.eci.c15.salas.application.SalaActualizadaEvent;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.scheduling.TaskScheduler;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class ArranqueAutomaticoListenerTest {

    @Test
    void evaluaElArranqueEnElHiloDeSalasNoEnElDelRequest() {
        CuentaRegresivaPartida cuentaRegresiva = mock(CuentaRegresivaPartida.class);
        TaskScheduler scheduler = mock(TaskScheduler.class);
        Instant ahora = Instant.parse("2026-10-12T00:00:00Z");
        when(scheduler.getClock()).thenReturn(Clock.fixed(ahora, ZoneOffset.UTC));
        ArranqueAutomaticoListener listener = new ArranqueAutomaticoListener(cuentaRegresiva, scheduler);

        listener.onSalaActualizada(new SalaActualizadaEvent("s1"));

        ArgumentCaptor<Runnable> tarea = ArgumentCaptor.forClass(Runnable.class);
        verify(scheduler).schedule(tarea.capture(), eq(ahora));
        verifyNoInteractions(cuentaRegresiva);

        tarea.getValue().run();
        verify(cuentaRegresiva).evaluar("s1");
    }
}
