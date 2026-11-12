package co.eci.c15.salas.application;

import co.eci.c15.salas.infrastructure.estado.CuentasRegresivasEnMemoria;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.scheduling.TaskScheduler;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.concurrent.ScheduledFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class CuentaRegresivaPartidaTest {

    private static final Instant AHORA = Instant.parse("2026-10-09T12:00:00Z");

    private IniciarPartidaUseCase iniciar;
    private TaskScheduler scheduler;
    private ArranquePartidaNotifier notifier;
    private ScheduledFuture<?> tarea;
    private CuentasRegresivas cuentas;
    private CuentaRegresivaPartida cuenta;

    @BeforeEach
    void setUp() {
        iniciar = mock(IniciarPartidaUseCase.class);
        scheduler = mock(TaskScheduler.class);
        notifier = mock(ArranquePartidaNotifier.class);
        tarea = mock(ScheduledFuture.class);
        when(scheduler.getClock()).thenReturn(Clock.fixed(AHORA, ZoneOffset.UTC));
        doReturn(tarea).when(scheduler).schedule(any(Runnable.class), any(Instant.class));
        cuentas = new CuentasRegresivasEnMemoria();
        cuenta = new CuentaRegresivaPartida(iniciar, scheduler, notifier, cuentas, Duration.ofSeconds(5));
    }

    private Runnable programada() {
        ArgumentCaptor<Runnable> runnable = ArgumentCaptor.forClass(Runnable.class);
        verify(scheduler).schedule(runnable.capture(), eq(AHORA.plusSeconds(5)));
        return runnable.getValue();
    }

    @Test
    void salaListaProgramaElInicioEnCincoSegundosYAvisa() {
        when(iniciar.listaParaIniciar("s1")).thenReturn(true);

        cuenta.evaluar("s1");

        programada();
        assertTrue(cuenta.enCurso("s1"));
        verify(notifier).cuentaRegresiva("s1", 5);
    }

    @Test
    void salaNoListaNoProgramaNada() {
        cuenta.evaluar("s1");

        verifyNoInteractions(notifier);
        verify(scheduler, never()).schedule(any(Runnable.class), any(Instant.class));
    }

    @Test
    void evaluarDeNuevoDuranteLaCuentaNoLaReinicia() {
        when(iniciar.listaParaIniciar("s1")).thenReturn(true);

        cuenta.evaluar("s1");
        cuenta.evaluar("s1");

        verify(scheduler, times(1)).schedule(any(Runnable.class), any(Instant.class));
        verify(notifier, times(1)).cuentaRegresiva("s1", 5);
    }

    @Test
    void siDejaDeEstarListaSeCancelaLaCuenta() {
        when(iniciar.listaParaIniciar("s1")).thenReturn(true, false);
        cuenta.evaluar("s1");

        cuenta.evaluar("s1");

        verify(tarea).cancel(false);
        verify(notifier).cancelada("s1");
        assertFalse(cuenta.enCurso("s1"));
    }

    @Test
    void alTerminarLaCuentaIniciaLaPartida() {
        when(iniciar.listaParaIniciar("s1")).thenReturn(true);
        when(iniciar.iniciar("s1")).thenReturn(Optional.of("s1"));
        cuenta.evaluar("s1");

        programada().run();

        verify(notifier).iniciada("s1", "s1");
        assertFalse(cuenta.enCurso("s1"));
    }

    @Test
    void siAlTerminarYaNoEstaListaAvisaCancelada() {
        when(iniciar.listaParaIniciar("s1")).thenReturn(true);
        when(iniciar.iniciar("s1")).thenReturn(Optional.empty());
        cuenta.evaluar("s1");

        programada().run();

        verify(notifier).cancelada("s1");
        verify(notifier, never()).iniciada(anyString(), anyString());
    }

    @Test
    void unaCuentaCanceladaQueAlcanzaAEjecutarseNoIniciaNada() {
        when(iniciar.listaParaIniciar("s1")).thenReturn(true, false);
        cuenta.evaluar("s1");
        Runnable vieja = programada();
        cuenta.evaluar("s1");

        vieja.run();

        verify(iniciar, never()).iniciar(anyString());
    }

    @Test
    void siOtraInstanciaYaLlevaLaCuentaEstaNoProgramaNada() {
        when(iniciar.listaParaIniciar("s1")).thenReturn(true);
        cuentas.empezar("s1", "de-otra-instancia", Duration.ofSeconds(15));

        cuenta.evaluar("s1");

        verify(scheduler, never()).schedule(any(Runnable.class), any(Instant.class));
        verifyNoInteractions(notifier);
        assertTrue(cuenta.enCurso("s1"));
    }

    @Test
    void siAlguienSeDesmarcaEnOtraInstanciaSeCancelaLaCuentaDeLaOtra() {
        when(iniciar.listaParaIniciar("s1")).thenReturn(false);
        cuentas.empezar("s1", "de-otra-instancia", Duration.ofSeconds(15));

        cuenta.evaluar("s1");

        verify(notifier).cancelada("s1");
        assertFalse(cuenta.enCurso("s1"));
    }

    @Test
    void siLaCancelaronEnOtraInstanciaSuTareaYaNoArrancaLaPartida() {
        when(iniciar.listaParaIniciar("s1")).thenReturn(true);
        cuenta.evaluar("s1");
        Runnable propia = programada();
        cuentas.cancelar("s1"); // otra instancia vio que alguien se desmarco

        propia.run();

        verify(iniciar, never()).iniciar(anyString());
    }

    @Test
    void siFallaElInicioAvisaCancelada() {
        when(iniciar.listaParaIniciar("s1")).thenReturn(true);
        when(iniciar.iniciar("s1")).thenThrow(new IllegalStateException("fallo"));
        cuenta.evaluar("s1");

        programada().run();

        verify(notifier).cancelada("s1");
    }
}
