package co.eci.c15;

import co.eci.c15.common.events.PartidaIniciadaEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * En las pruebas, lo que gameplay recibiria: las partidas que salas inicio (en memoria o de vuelta por Kafka).
 * Gameplay es otro servicio; aqui solo se comprueba que salas publica el evento con lo que gameplay necesita.
 */
@Component
class IniciosDePartida {

    private final Map<String, PartidaIniciadaEvent> recibidos = new ConcurrentHashMap<>();

    @EventListener
    void onPartidaIniciada(PartidaIniciadaEvent evento) {
        recibidos.put(evento.matchId(), evento);
    }

    Optional<PartidaIniciadaEvent> de(String matchId) {
        return Optional.ofNullable(recibidos.get(matchId));
    }
}
