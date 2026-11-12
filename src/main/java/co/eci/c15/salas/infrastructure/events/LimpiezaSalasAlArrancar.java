package co.eci.c15.salas.infrastructure.events;

import co.eci.c15.common.logging.EventoNegocio;
import co.eci.c15.salas.application.FinalizarPartidaUseCase;
import co.eci.c15.salas.domain.Sala;
import co.eci.c15.salas.domain.SalaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Quien esta conectado y las partidas viven en memoria, asi que tras un reinicio las salas que
 * quedaron en la base ya no las puede cerrar nadie: serian fantasmas en el catalogo. Se borran al
 * arrancar. Solo con el estado en memoria (una instancia): con Redis encendido lo hace
 * {@code BarrenderoSalas}, que no borra las salas que siguen vivas en otras instancias.
 */
@Component
@ConditionalOnProperty(name = "c15.estado.redis", havingValue = "false", matchIfMissing = true)
public class LimpiezaSalasAlArrancar {

    private static final Logger log = LoggerFactory.getLogger(LimpiezaSalasAlArrancar.class);

    private final SalaRepository salas;
    private final FinalizarPartidaUseCase finalizar;

    public LimpiezaSalasAlArrancar(SalaRepository salas, FinalizarPartidaUseCase finalizar) {
        this.salas = salas;
        this.finalizar = finalizar;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void limpiar() {
        try {
            List<Sala> restantes = salas.findAll();
            restantes.forEach(sala -> {
                finalizar.ejecutar(sala.getId());
                EventoNegocio.de(log, "sala.cerrada").con("salaId", sala.getId()).con("motivo", "reinicio_servidor").info();
            });
            if (!restantes.isEmpty()) log.info("Se borraron {} salas que quedaron del arranque anterior", restantes.size());
        } catch (RuntimeException e) {
            // Unas salas de mas no justifican tumbar el arranque de la aplicacion
            log.error("No se pudieron limpiar las salas al arrancar", e);
        }
    }
}
