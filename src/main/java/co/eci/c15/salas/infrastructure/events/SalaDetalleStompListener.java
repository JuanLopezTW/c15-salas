package co.eci.c15.salas.infrastructure.events;

import co.eci.c15.salas.application.SalaActualizadaEvent;
import co.eci.c15.salas.application.VerSalaUseCase;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

/** Publica el detalle de la sala en /topic/salas/{salaId} cada vez que cambia (HU-62.3). */
@Component
public class SalaDetalleStompListener {

    private final SimpMessagingTemplate messaging;
    private final VerSalaUseCase verSala;

    public SalaDetalleStompListener(SimpMessagingTemplate messaging, VerSalaUseCase verSala) {
        this.messaging = messaging;
        this.verSala = verSala;
    }

    @EventListener
    public void onSalaActualizada(SalaActualizadaEvent event) {
        verSala.buscarAbierta(event.salaId())
                .ifPresent(detalle -> messaging.convertAndSend("/topic/salas/" + event.salaId(), detalle));
    }
}
