package co.eci.c15.salas.application;

import co.eci.c15.common.events.EventosEntreServicios;
import co.eci.c15.common.logging.EventoNegocio;
import co.eci.c15.common.events.SalaCerradaEvent;
import co.eci.c15.salas.domain.Equipo;
import co.eci.c15.salas.domain.EquipoRepository;
import co.eci.c15.salas.domain.Sala;
import co.eci.c15.salas.domain.SalaRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

/**
 * Un jugador deja la sala mientras sigue en el lobby (hoy: al cerrar la pagina). Si es el
 * anfitrion, la sala se cierra: se borran la sala y sus equipos y se avisa a los demas, para que
 * no queden lobbys fantasma. Con la partida ya iniciada no hace nada (no hay reconexion).
 */
@Service
public class SalirDeSalaUseCase {

    private static final Logger log = LoggerFactory.getLogger(SalirDeSalaUseCase.class);

    public enum Resultado { SALIO, SALA_CERRADA, NADA }

    private final SalaRepository salas;
    private final EquipoRepository equipos;
    private final ArranquePartidaNotifier notifier;
    private final ApplicationEventPublisher events;
    private final EventosEntreServicios entreServicios;

    public SalirDeSalaUseCase(SalaRepository salas, EquipoRepository equipos, ArranquePartidaNotifier notifier,
                              ApplicationEventPublisher events, EventosEntreServicios entreServicios) {
        this.salas = salas;
        this.equipos = equipos;
        this.notifier = notifier;
        this.events = events;
        this.entreServicios = entreServicios;
    }

    public boolean esAnfitrion(String salaId, String userId) {
        return salas.findById(salaId).map(sala -> sala.esAnfitrion(userId)).orElse(false);
    }

    @Transactional
    public Resultado ejecutar(String salaId, String userId) {
        Optional<Sala> encontrada = salas.findById(salaId);
        if (encontrada.isEmpty()) return Resultado.NADA;
        Sala sala = encontrada.get();
        if (!sala.isDisponible()) return Resultado.NADA;

        if (sala.esAnfitrion(userId)) {
            equipos.findBySalaId(salaId).stream().map(Equipo::getId).forEach(equipos::delete);
            salas.delete(salaId);
            notifier.cerrada(salaId);
            events.publishEvent(new SalaActualizadaEvent(salaId));
            entreServicios.publicar(new SalaCerradaEvent(salaId));
            EventoNegocio.de(log, "sala.cerrada").con("salaId", salaId).con("motivo", "anfitrion_salio").info();
            return Resultado.SALA_CERRADA;
        }

        if (!sala.contieneJugador(userId)) return Resultado.NADA;
        equipos.findBySalaId(salaId).stream()
                .filter(e -> e.contieneMiembro(userId))
                .forEach(e -> {
                    e.quitarMiembro(userId);
                    equipos.save(e);
                });
        sala.retirarJugador(userId);
        salas.save(sala);
        events.publishEvent(new SalaActualizadaEvent(salaId));
        EventoNegocio.de(log, "sala.jugador_salio").con("salaId", salaId).con("userId", userId).info();
        return Resultado.SALIO;
    }
}
