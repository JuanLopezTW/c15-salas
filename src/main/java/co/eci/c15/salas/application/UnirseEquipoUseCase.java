package co.eci.c15.salas.application;

import co.eci.c15.common.logging.EventoNegocio;
import co.eci.c15.salas.domain.Equipo;
import co.eci.c15.salas.domain.EquipoRepository;
import co.eci.c15.salas.domain.JugadorNoEnSalaException;
import co.eci.c15.salas.domain.PartidaYaIniciadaException;
import co.eci.c15.salas.domain.Sala;
import co.eci.c15.salas.domain.SalaRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class UnirseEquipoUseCase {

    private static final Logger log = LoggerFactory.getLogger(UnirseEquipoUseCase.class);

    private final EquipoRepository equipos;
    private final SalaRepository salas;
    private final ApplicationEventPublisher events;

    public UnirseEquipoUseCase(EquipoRepository equipos, SalaRepository salas, ApplicationEventPublisher events) {
        this.equipos = equipos;
        this.salas = salas;
        this.events = events;
    }

    /**
     * Une al jugador al equipo. El jugador debe estar en la sala del equipo.
     * Si ya estaba en otro equipo de la misma sala, se cambia de equipo.
     */
    @Transactional
    public EquipoDto ejecutar(String equipoId, String userId) {
        Equipo equipo = equipos.findById(equipoId)
                .orElseThrow(() -> new IllegalArgumentException("Equipo no encontrado: " + equipoId));
        Sala sala = salas.findById(equipo.getSalaId())
                .orElseThrow(() -> new IllegalArgumentException("Sala no encontrada: " + equipo.getSalaId()));
        if (!sala.isDisponible()) throw new PartidaYaIniciadaException(sala.getId());
        if (!sala.contieneJugador(userId)) throw new JugadorNoEnSalaException(userId, sala.getId());
        if (equipo.contieneMiembro(userId)) return EquipoDto.from(equipo);

        equipo.unirMiembro(userId);
        for (Equipo anterior : equipos.findBySalaId(sala.getId())) {
            if (!anterior.getId().equals(equipo.getId()) && anterior.contieneMiembro(userId)) {
                anterior.quitarMiembro(userId);
                equipos.save(anterior);
            }
        }
        equipos.save(equipo);
        events.publishEvent(new SalaActualizadaEvent(sala.getId()));
        EventoNegocio.de(log, "equipo.jugador_unido").con("salaId", sala.getId()).con("equipoId", equipoId)
                .con("userId", userId).info();
        return EquipoDto.from(equipo);
    }
}