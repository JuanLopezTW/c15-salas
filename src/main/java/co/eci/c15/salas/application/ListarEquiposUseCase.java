package co.eci.c15.salas.application;

import co.eci.c15.salas.domain.EquipoRepository;
import co.eci.c15.salas.domain.SalaRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ListarEquiposUseCase {

    private final SalaRepository salas;
    private final EquipoRepository equipos;

    public ListarEquiposUseCase(SalaRepository salas, EquipoRepository equipos) {
        this.salas = salas;
        this.equipos = equipos;
    }

    public List<EquipoDto> ejecutar(String salaId) {
        if (salas.findById(salaId).isEmpty()) throw new IllegalArgumentException("Sala no encontrada: " + salaId);
        return equipos.findBySalaId(salaId).stream().map(EquipoDto::from).toList();
    }
}