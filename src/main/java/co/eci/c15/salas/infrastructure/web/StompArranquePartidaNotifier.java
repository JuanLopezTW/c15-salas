package co.eci.c15.salas.infrastructure.web;

import co.eci.c15.salas.application.ArranquePartidaNotifier;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

/**
 * Publica el arranque en /topic/salas/{salaId}/inicio con estado CUENTA_REGRESIVA (y los
 * segundos), CANCELADA o INICIADA (y el matchId).
 */
@Component
public class StompArranquePartidaNotifier implements ArranquePartidaNotifier {

    public record ArranqueMensaje(String estado, Long segundos, String matchId) {}

    private final SimpMessagingTemplate messaging;

    public StompArranquePartidaNotifier(SimpMessagingTemplate messaging) {
        this.messaging = messaging;
    }

    @Override
    public void cuentaRegresiva(String salaId, long segundos) {
        enviar(salaId, new ArranqueMensaje("CUENTA_REGRESIVA", segundos, null));
    }

    @Override
    public void cancelada(String salaId) {
        enviar(salaId, new ArranqueMensaje("CANCELADA", null, null));
    }

    @Override
    public void iniciada(String salaId, String matchId) {
        enviar(salaId, new ArranqueMensaje("INICIADA", null, matchId));
    }

    @Override
    public void cerrada(String salaId) {
        enviar(salaId, new ArranqueMensaje("CERRADA", null, null));
    }

    private void enviar(String salaId, ArranqueMensaje mensaje) {
        messaging.convertAndSend("/topic/salas/" + salaId + "/inicio", mensaje);
    }
}
