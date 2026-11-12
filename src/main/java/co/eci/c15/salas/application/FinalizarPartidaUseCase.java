package co.eci.c15.salas.application;

import co.eci.c15.common.events.EventosEntreServicios;
import co.eci.c15.common.events.SalaCerradaEvent;
import co.eci.c15.salas.domain.Equipo;
import co.eci.c15.salas.domain.EquipoRepository;
import co.eci.c15.salas.domain.SalaRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cierre de la sala cuando termina su partida (matchId = salaId): se borran la sala y sus
 * equipos para que salga del catalogo. El resultado sigue en gameplay
 * ({@code GET /api/partidas/{matchId}/resultado}).
 */
@Service
public class FinalizarPartidaUseCase {

    private final SalaRepository salas;
    private final EquipoRepository equipos;
    private final ApplicationEventPublisher events;
    private final EventosEntreServicios entreServicios;

    public FinalizarPartidaUseCase(SalaRepository salas, EquipoRepository equipos, ApplicationEventPublisher events,
                                   EventosEntreServicios entreServicios) {
        this.salas = salas;
        this.equipos = equipos;
        this.events = events;
        this.entreServicios = entreServicios;
    }

    /** @return true si la sala existia y se elimino */
    @Transactional
    public boolean ejecutar(String salaId) {
        if (salas.findById(salaId).isEmpty()) return false;
        equipos.findBySalaId(salaId).stream().map(Equipo::getId).forEach(equipos::delete);
        salas.delete(salaId);
        events.publishEvent(new SalaActualizadaEvent(salaId));
        entreServicios.publicar(new SalaCerradaEvent(salaId));
        return true;
    }
}
