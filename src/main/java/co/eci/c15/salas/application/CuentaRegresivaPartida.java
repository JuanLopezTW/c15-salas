package co.eci.c15.salas.application;

import co.eci.c15.common.logging.EventoNegocio;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ScheduledFuture;

/**
 * Cuenta regresiva antes de arrancar la partida (HU-64.4). Cuando la sala queda lista se
 * programa el inicio; si deja de estarlo antes de que termine (alguien se desmarca, entra un
 * jugador nuevo, etc.) se cancela.
 *
 * <p>Con varias instancias la cuenta de cada sala se registra en {@link CuentasRegresivas}: solo una
 * instancia la lleva (y es la unica que arranca la partida), y cualquiera la cancela al momento.
 */
@Service
public class CuentaRegresivaPartida {

    private static final Logger log = LoggerFactory.getLogger(CuentaRegresivaPartida.class);
    /** Si la instancia que lleva la cuenta muere, su registro vence poco despues de cuando debia arrancar. */
    static final Duration MARGEN = Duration.ofSeconds(10);

    private final IniciarPartidaUseCase iniciarPartida;
    private final TaskScheduler scheduler;
    private final ArranquePartidaNotifier notifier;
    private final CuentasRegresivas cuentas;
    private final Duration espera;
    /** Tareas que lleva esta instancia, para cancelarlas. Protegido por {@code this}. */
    private final Map<String, ScheduledFuture<?>> propias = new HashMap<>();

    public CuentaRegresivaPartida(IniciarPartidaUseCase iniciarPartida,
                                  @Qualifier("salasScheduler") TaskScheduler scheduler,
                                  ArranquePartidaNotifier notifier,
                                  CuentasRegresivas cuentas,
                                  @Value("${c15.partida.cuenta-regresiva:5s}") Duration espera) {
        this.iniciarPartida = iniciarPartida;
        this.scheduler = scheduler;
        this.notifier = notifier;
        this.cuentas = cuentas;
        this.espera = espera;
    }

    public synchronized void evaluar(String salaId) {
        boolean lista = iniciarPartida.listaParaIniciar(salaId);
        if (lista && !cuentas.enCurso(salaId)) {
            String token = UUID.randomUUID().toString();
            // Si otra instancia la empezo un instante antes, la lleva ella
            if (!cuentas.empezar(salaId, token, espera.plus(MARGEN))) return;
            ScheduledFuture<?> tarea = scheduler.schedule(() -> alTerminar(salaId, token),
                    scheduler.getClock().instant().plus(espera));
            propias.put(salaId, tarea);
            notifier.cuentaRegresiva(salaId, espera.toSeconds());
            EventoNegocio.de(log, "partida.cuenta_regresiva").con("salaId", salaId).con("segundos", espera.toSeconds()).info();
        } else if (!lista && cuentas.cancelar(salaId)) {
            ScheduledFuture<?> tarea = propias.remove(salaId);
            if (tarea != null) tarea.cancel(false);
            notifier.cancelada(salaId);
            EventoNegocio.de(log, "partida.cuenta_cancelada").con("salaId", salaId).info();
        }
    }

    public boolean enCurso(String salaId) {
        return cuentas.enCurso(salaId);
    }

    void alTerminar(String salaId, String token) {
        synchronized (this) {
            propias.remove(salaId);
        }
        // Si la cancelaron (aqui o en otra instancia) ya no es suya: no arranca nada
        if (!cuentas.terminar(salaId, token)) return;
        try {
            iniciarPartida.iniciar(salaId).ifPresentOrElse(
                    matchId -> notifier.iniciada(salaId, matchId),
                    () -> notifier.cancelada(salaId));
        } catch (RuntimeException e) {
            log.error("No se pudo iniciar la partida de la sala {}", salaId, e);
            notifier.cancelada(salaId);
        }
    }
}
