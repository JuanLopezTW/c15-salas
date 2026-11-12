package co.eci.c15.salas.application;

import co.eci.c15.common.events.EventosEntreServicios;
import co.eci.c15.common.logging.EventoNegocio;
import co.eci.c15.common.events.PartidaIniciadaEvent;
import co.eci.c15.salas.domain.Equipo;
import co.eci.c15.salas.domain.EquipoRepository;
import co.eci.c15.salas.domain.Sala;
import co.eci.c15.salas.domain.SalaRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Arranque de la partida (HU-64.4). La partida usa el id de la sala como matchId: una sala
 * solo tiene una partida. Solo juegan los equipos con jugadores.
 */
@Service
public class IniciarPartidaUseCase {

    private static final Logger log = LoggerFactory.getLogger(IniciarPartidaUseCase.class);

    private final SalaRepository salas;
    private final EquipoRepository equipos;
    private final ApplicationEventPublisher events;
    private final EventosEntreServicios entreServicios;

    public IniciarPartidaUseCase(SalaRepository salas, EquipoRepository equipos, ApplicationEventPublisher events,
                                 EventosEntreServicios entreServicios) {
        this.salas = salas;
        this.equipos = equipos;
        this.events = events;
        this.entreServicios = entreServicios;
    }

    public boolean listaParaIniciar(String salaId) {
        return salas.findById(salaId)
                .map(sala -> sala.listaParaIniciar(equipos.findBySalaId(salaId)))
                .orElse(false);
    }

    /**
     * Vuelve a validar y, si la sala sigue lista, la cierra y publica {@link PartidaIniciadaEvent}.
     * @return el matchId, o vacio si la sala ya no esta lista
     */
    @Transactional
    public Optional<String> iniciar(String salaId) {
        Optional<Sala> encontrada = salas.findById(salaId);
        if (encontrada.isEmpty()) return Optional.empty();
        Sala sala = encontrada.get();
        List<Equipo> equiposSala = equipos.findBySalaId(salaId);
        if (!sala.listaParaIniciar(equiposSala)) return Optional.empty();

        sala.iniciarPartida();
        salas.save(sala);

        Map<String, List<String>> jugadoresPorEquipo = new LinkedHashMap<>();
        equiposSala.stream()
                .filter(e -> !e.isVacio())
                .forEach(e -> jugadoresPorEquipo.put(e.getId(), e.getMiembros().stream().sorted().toList()));
        String matchId = sala.getId();
        // Gameplay, chat y estadisticas lo reciben despues del commit: si alguno falla, la sala ya quedo EN_PARTIDA
        entreServicios.publicar(new PartidaIniciadaEvent(matchId, Collections.unmodifiableMap(jugadoresPorEquipo),
                sala.getDuracion()));
        events.publishEvent(new SalaActualizadaEvent(salaId));
        EventoNegocio.de(log, "partida.iniciada").con("matchId", matchId).con("equipos", jugadoresPorEquipo.size())
                .con("jugadores", jugadoresPorEquipo.values().stream().mapToInt(List::size).sum())
                .con("minutos", sala.getDuracionMinutos()).info();
        return Optional.of(matchId);
    }
}
