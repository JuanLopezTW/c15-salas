package co.eci.c15.salas.infrastructure.events;

import co.eci.c15.common.events.SesionEvent;
import co.eci.c15.salas.application.SalirDeSalaUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Las conexiones del lobby, segun las avisa el broker (topic c15.sesion). Al abrir (o renovar) se marca la
 * presencia. Al cerrar, un invitado sale de una; el anfitrion tiene un tiempo de gracia para volver, porque si
 * sale la sala se cierra para todos: un corte de red o recargar no debe causar eso. Si el jugador sigue con
 * otra pestaña en la sala, no sale. Un cierre repetido o de una sesion que no estaba se ignora.
 */
@Component
public class SesionesLobbyListener {

    private static final Logger log = LoggerFactory.getLogger(SesionesLobbyListener.class);

    private final PresenciaSala presencia;
    private final SalirDeSalaUseCase salir;
    private final TaskScheduler scheduler;
    private final Duration gracia;

    public SesionesLobbyListener(PresenciaSala presencia, SalirDeSalaUseCase salir,
                                 @Qualifier("salasScheduler") TaskScheduler scheduler,
                                 @Value("${c15.salas.gracia-desconexion:20s}") Duration gracia) {
        this.presencia = presencia;
        this.salir = salir;
        this.scheduler = scheduler;
        this.gracia = gracia;
    }

    @EventListener
    public void onSesion(SesionEvent sesion) {
        if (!sesion.deSala()) return;
        if (sesion.estaAbierta()) {
            presencia.entrar(sesion.sessionId(), sesion.id(), sesion.userId());
            return;
        }
        if (!presencia.salir(sesion.sessionId(), sesion.id(), sesion.userId())) return;
        if (salir.esAnfitrion(sesion.id(), sesion.userId())) {
            log.info("Anfitrion {} desconectado de la sala {}; tiene {} para volver", sesion.userId(), sesion.id(), gracia);
            scheduler.schedule(() -> sacarSiNoVolvio(sesion), scheduler.getClock().instant().plus(gracia));
        } else {
            log.info("Jugador {} desconectado de la sala {}", sesion.userId(), sesion.id());
            sacarSiNoVolvio(sesion);
        }
    }

    private void sacarSiNoVolvio(SesionEvent sesion) {
        if (!presencia.conectado(sesion.id(), sesion.userId())) salir.ejecutar(sesion.id(), sesion.userId());
    }
}
