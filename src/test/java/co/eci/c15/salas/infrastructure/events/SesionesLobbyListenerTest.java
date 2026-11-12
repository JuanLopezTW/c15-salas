package co.eci.c15.salas.infrastructure.events;

import co.eci.c15.common.events.SesionEvent;
import co.eci.c15.salas.application.SalirDeSalaUseCase;
import co.eci.c15.salas.infrastructure.estado.PresenciaSalaEnMemoria;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.scheduling.TaskScheduler;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class SesionesLobbyListenerTest {

    private static final Instant AHORA = Instant.parse("2026-11-02T15:00:00Z");
    private static final Duration GRACIA = Duration.ofSeconds(20);

    private SalirDeSalaUseCase salir;
    private TaskScheduler scheduler;
    private SesionesLobbyListener listener;

    @BeforeEach
    void setUp() {
        salir = mock(SalirDeSalaUseCase.class);
        scheduler = mock(TaskScheduler.class);
        when(scheduler.getClock()).thenReturn(Clock.fixed(AHORA, ZoneOffset.UTC));
        when(salir.esAnfitrion("s1", "host")).thenReturn(true);
        listener = new SesionesLobbyListener(new PresenciaSalaEnMemoria(), salir, scheduler, GRACIA);
    }

    private void abrir(String userId, String sessionId) {
        listener.onSesion(new SesionEvent(SesionEvent.ABIERTA, SesionEvent.SALA, "s1", userId, sessionId));
    }

    private void cerrar(String userId, String sessionId) {
        listener.onSesion(new SesionEvent(SesionEvent.CERRADA, SesionEvent.SALA, "s1", userId, sessionId));
    }

    /** Se cumple el tiempo de gracia de la ultima desconexion. */
    private void pasaLaGracia(int programadas) {
        ArgumentCaptor<Runnable> tareas = ArgumentCaptor.forClass(Runnable.class);
        verify(scheduler, times(programadas)).schedule(tareas.capture(), eq(AHORA.plus(GRACIA)));
        tareas.getValue().run();
    }

    @Test
    void elInvitadoSaleDeUnaSinTiempoDeGracia() {
        abrir("ana", "sesion-1");

        cerrar("ana", "sesion-1");

        verify(salir).ejecutar("s1", "ana");
        verify(scheduler, never()).schedule(any(Runnable.class), any(Instant.class));
    }

    @Test
    void elAnfitrionNoSaleDeUnaSinoCuandoPasaLaGracia() {
        abrir("host", "sesion-1");

        cerrar("host", "sesion-1");
        verify(salir, never()).ejecutar(anyString(), anyString());

        pasaLaGracia(1);
        verify(salir).ejecutar("s1", "host");
    }

    @Test
    void siElAnfitrionVuelveDentroDeLaGraciaNoSale() {
        abrir("host", "sesion-1");
        cerrar("host", "sesion-1");

        abrir("host", "sesion-nueva");
        pasaLaGracia(1);

        verify(salir, never()).ejecutar(anyString(), anyString());
    }

    @Test
    void elCierreDeUnaSesionQueNoEstabaSeIgnora() {
        cerrar("ana", "desconocida");

        verify(scheduler, never()).schedule(any(Runnable.class), any(Instant.class));
        verifyNoInteractions(salir);
    }

    @Test
    void unCierreRepetidoPorKafkaSoloSeProcesaUnaVez() {
        abrir("ana", "sesion-1");

        cerrar("ana", "sesion-1");
        cerrar("ana", "sesion-1");

        verify(salir, times(1)).ejecutar(anyString(), anyString());
    }

    @Test
    void conOtraPestanaAbiertaEnLaSalaNoSale() {
        abrir("ana", "sesion-1");
        abrir("ana", "sesion-2");

        cerrar("ana", "sesion-1");
        verify(salir, never()).ejecutar(anyString(), anyString());

        cerrar("ana", "sesion-2");
        verify(salir).ejecutar("s1", "ana");
    }

    @Test
    void lasSesionesDelMapaDeUnaPartidaNoSonDeSalas() {
        listener.onSesion(new SesionEvent(SesionEvent.ABIERTA, SesionEvent.PARTIDA, "s1", "ana", "sesion-1"));
        listener.onSesion(new SesionEvent(SesionEvent.CERRADA, SesionEvent.PARTIDA, "s1", "ana", "sesion-1"));

        verifyNoInteractions(salir, scheduler);
    }
}
