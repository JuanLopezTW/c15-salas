package co.eci.c15.salas.application;

import co.eci.c15.salas.domain.EquipoRepository;
import co.eci.c15.salas.domain.PartidaYaIniciadaException;
import co.eci.c15.salas.domain.Sala;
import co.eci.c15.salas.domain.SalaNoEncontradaException;
import co.eci.c15.salas.domain.SalaRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Detalle de una sala para el lobby (HU-62.3). Cualquiera puede consultarlo; una sala con la
 * partida ya iniciada se considera cerrada.
 */
@Service
public class VerSalaUseCase {

    private final SalaRepository salas;
    private final EquipoRepository equipos;

    public VerSalaUseCase(SalaRepository salas, EquipoRepository equipos) {
        this.salas = salas;
        this.equipos = equipos;
    }

    public SalaDetalleDto ejecutar(String salaId) {
        Sala sala = salas.findById(salaId).orElseThrow(() -> new SalaNoEncontradaException(salaId));
        if (!sala.isDisponible()) throw new PartidaYaIniciadaException(salaId);
        return detalle(sala);
    }

    /** Detalle para notificar en tiempo real; vacio si la sala no existe o ya esta cerrada. */
    public Optional<SalaDetalleDto> buscarAbierta(String salaId) {
        return salas.findById(salaId).filter(Sala::isDisponible).map(this::detalle);
    }

    private SalaDetalleDto detalle(Sala sala) {
        return SalaDetalleDto.from(sala, equipos.findBySalaId(sala.getId()));
    }
}
