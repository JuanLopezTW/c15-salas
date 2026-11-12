package co.eci.c15.salas.infrastructure.events;

import co.eci.c15.salas.application.ListarSalasUseCase;
import co.eci.c15.salas.application.SalaActualizadaEvent;
import co.eci.c15.salas.application.SalaCreadaEvent;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
public class SalaCreadaStompListener {

    private final SimpMessagingTemplate messaging;
    private final ListarSalasUseCase listar;

    public SalaCreadaStompListener(SimpMessagingTemplate messaging, ListarSalasUseCase listar) {
        this.messaging = messaging;
        this.listar = listar;
    }

    @EventListener
    public void onSalaCreada(SalaCreadaEvent event) {
        messaging.convertAndSend("/topic/salas", listar.ejecutar());
    }

    @EventListener
    public void onSalaActualizada(SalaActualizadaEvent event) {
        messaging.convertAndSend("/topic/salas", listar.ejecutar());
    }
}
