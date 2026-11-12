package co.eci.c15.salas.infrastructure.events;

import co.eci.c15.salas.application.CuentaRegresivaPartida;
import co.eci.c15.salas.application.SalaActualizadaEvent;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Cada cambio en la sala revisa si ya puede arrancar la partida (HU-64.4).
 * Se evalua despues del commit y en el hilo de salas, no en el del request: si varios marcan
 * "listo" a la vez, cada evaluacion lee lo que ya guardaron los demas con una sesion de JPA
 * limpia (la del request todavia tiene los equipos que leyo antes) y sin pedir una segunda
 * conexion mientras el request tiene la suya. Antes, con "listos" simultaneos, ninguna
 * evaluacion veia a todos listos y la partida no arrancaba.
 */
@Component
public class ArranqueAutomaticoListener {

    private final CuentaRegresivaPartida cuentaRegresiva;
    private final TaskScheduler scheduler;

    public ArranqueAutomaticoListener(CuentaRegresivaPartida cuentaRegresiva,
                                      @Qualifier("salasScheduler") TaskScheduler scheduler) {
        this.cuentaRegresiva = cuentaRegresiva;
        this.scheduler = scheduler;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onSalaActualizada(SalaActualizadaEvent event) {
        scheduler.schedule(() -> cuentaRegresiva.evaluar(event.salaId()), scheduler.getClock().instant());
    }
}
