package co.eci.c15.salas.infrastructure.events;

import co.eci.c15.common.logging.EventoNegocio;
import co.eci.c15.common.events.MatchFinishedEvent;
import co.eci.c15.salas.application.FinalizarPartidaUseCase;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Al terminar la partida (HU-67.6) la sala sale del catalogo. */
@Component
public class PartidaTerminadaListener {

    private static final Logger log = LoggerFactory.getLogger(PartidaTerminadaListener.class);

    private final FinalizarPartidaUseCase finalizar;

    public PartidaTerminadaListener(FinalizarPartidaUseCase finalizar) {
        this.finalizar = finalizar;
    }

    @EventListener
    public void onPartidaTerminada(MatchFinishedEvent event) {
        if (finalizar.ejecutar(event.matchId())) {
            EventoNegocio.de(log, "sala.cerrada").con("salaId", event.matchId()).con("motivo", "partida_terminada").info();
        }
    }
}
