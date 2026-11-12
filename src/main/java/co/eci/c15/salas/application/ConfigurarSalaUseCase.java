package co.eci.c15.salas.application;

import co.eci.c15.common.logging.EventoNegocio;
import co.eci.c15.salas.domain.ConfiguracionInvalidaException;
import co.eci.c15.salas.domain.Equipo;
import co.eci.c15.salas.domain.EquipoRepository;
import co.eci.c15.salas.domain.Sala;
import co.eci.c15.salas.domain.SalaRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

@Service
public class ConfigurarSalaUseCase {

    private static final Logger log = LoggerFactory.getLogger(ConfigurarSalaUseCase.class);

    private final SalaRepository salas;
    private final EquipoRepository equipos;
    private final ApplicationEventPublisher events;

    public ConfigurarSalaUseCase(SalaRepository salas, EquipoRepository equipos, ApplicationEventPublisher events) {
        this.salas = salas;
        this.equipos = equipos;
        this.events = events;
    }

    /**
     * Cambia la configuración y ajusta los equipos: crea los que falten, elimina los sobrantes
     * (solo si están vacíos) y actualiza el cupo de cada uno. Si algo no cuadra no se guarda nada.
     */
    @Transactional
    public SalaDto ejecutar(String salaId, String solicitanteId, int numEquipos, int jugadoresPorEquipo) {
        Sala sala = salas.findById(salaId)
                .orElseThrow(() -> new IllegalArgumentException("Sala no encontrada: " + salaId));
        sala.configurar(solicitanteId, numEquipos, jugadoresPorEquipo);

        List<Equipo> actuales = equipos.findBySalaId(salaId);
        List<Equipo> sobrantes = actuales.stream().filter(e -> e.getNumero() > numEquipos).toList();
        List<Equipo> conservados = actuales.stream().filter(e -> e.getNumero() <= numEquipos).toList();

        for (Equipo equipo : sobrantes) {
            if (!equipo.isVacio()) {
                throw new ConfiguracionInvalidaException("No se puede quitar el equipo " + equipo.getNumero()
                        + " porque ya tiene jugadores");
            }
        }
        conservados.forEach(e -> e.cambiarCupo(jugadoresPorEquipo));

        salas.save(sala);
        sobrantes.forEach(e -> equipos.delete(e.getId()));
        conservados.forEach(equipos::save);
        for (int numero = conservados.size() + 1; numero <= numEquipos; numero++) {
            equipos.save(Equipo.crear(salaId, numero, jugadoresPorEquipo));
        }

        events.publishEvent(new SalaActualizadaEvent(sala.getId()));
        EventoNegocio.de(log, "sala.configurada").con("salaId", salaId).con("numEquipos", numEquipos)
                .con("jugadoresPorEquipo", jugadoresPorEquipo).info();
        return SalaDto.from(sala);
    }
}