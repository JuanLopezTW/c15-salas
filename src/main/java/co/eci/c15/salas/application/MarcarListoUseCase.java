package co.eci.c15.salas.application;

import co.eci.c15.common.logging.EventoNegocio;
import co.eci.c15.salas.domain.Equipo;
import co.eci.c15.salas.domain.EquipoRepository;
import co.eci.c15.salas.domain.JugadorSinEquipoException;
import co.eci.c15.salas.domain.PartidaYaIniciadaException;
import co.eci.c15.salas.domain.Sala;
import co.eci.c15.salas.domain.SalaNoEncontradaException;
import co.eci.c15.salas.domain.SalaRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Estado listo de un jugador en su equipo (HU-63.4). Cada jugador marca o desmarca el suyo y
 * debe estar en un equipo de la sala. Devuelve el detalle actualizado de la sala.
 */
@Service
public class MarcarListoUseCase {

    private static final Logger log = LoggerFactory.getLogger(MarcarListoUseCase.class);

    private final SalaRepository salas;
    private final EquipoRepository equipos;
    private final ApplicationEventPublisher events;

    public MarcarListoUseCase(SalaRepository salas, EquipoRepository equipos, ApplicationEventPublisher events) {
        this.salas = salas;
        this.equipos = equipos;
        this.events = events;
    }

    @Transactional
    public SalaDetalleDto marcar(String salaId, String userId) {
        return cambiar(salaId, userId, true);
    }

    @Transactional
    public SalaDetalleDto desmarcar(String salaId, String userId) {
        return cambiar(salaId, userId, false);
    }

    private SalaDetalleDto cambiar(String salaId, String userId, boolean listo) {
        if (userId == null || userId.isBlank()) throw new IllegalArgumentException("El userId es obligatorio");
        Sala sala = salas.findById(salaId).orElseThrow(() -> new SalaNoEncontradaException(salaId));
        if (!sala.isDisponible()) throw new PartidaYaIniciadaException(salaId);

        Equipo equipo = equipos.findBySalaId(salaId).stream()
                .filter(e -> e.contieneMiembro(userId))
                .findFirst()
                .orElseThrow(() -> new JugadorSinEquipoException(userId, salaId));
        if (equipo.isMiembroListo(userId) != listo) {
            if (listo) equipo.marcarListo(userId);
            else equipo.desmarcarListo(userId);
            equipos.save(equipo);
            events.publishEvent(new SalaActualizadaEvent(salaId));
            EventoNegocio.de(log, listo ? "jugador.listo" : "jugador.no_listo").con("salaId", salaId)
                    .con("userId", userId).info();
        }
        return SalaDetalleDto.from(sala, equipos.findBySalaId(salaId));
    }
}
