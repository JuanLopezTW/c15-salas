package co.eci.c15.salas.application;

import co.eci.c15.common.logging.EventoNegocio;
import co.eci.c15.salas.domain.Equipo;
import co.eci.c15.salas.domain.EquipoRepository;
import co.eci.c15.salas.domain.Sala;
import co.eci.c15.salas.domain.SalaRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class CrearSalaUseCase {

    private static final Logger log = LoggerFactory.getLogger(CrearSalaUseCase.class);

    private final SalaRepository salas;
    private final EquipoRepository equipos;
    private final ApplicationEventPublisher events;

    public CrearSalaUseCase(SalaRepository salas, EquipoRepository equipos, ApplicationEventPublisher events) {
        this.salas = salas;
        this.equipos = equipos;
        this.events = events;
    }

    /** Crea la sala (2 equipos de 2) con sus equipos vacíos; {@code minutos} null = duración por defecto. */
    @Transactional
    public SalaDto ejecutar(String nombre, String anfitrionId, Integer minutos) {
        Sala sala = salas.save(Sala.crear(nombre, anfitrionId, minutos));
        for (int numero = 1; numero <= sala.getNumEquipos(); numero++) {
            equipos.save(Equipo.crear(sala.getId(), numero, sala.getJugadoresPorEquipo()));
        }
        events.publishEvent(new SalaCreadaEvent(sala.getId(), sala.getNombre()));
        EventoNegocio.de(log, "sala.creada").con("salaId", sala.getId()).con("userId", anfitrionId)
                .con("minutos", sala.getDuracionMinutos()).info();
        return SalaDto.from(sala);
    }
}