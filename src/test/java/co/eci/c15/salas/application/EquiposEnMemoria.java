package co.eci.c15.salas.application;

import co.eci.c15.salas.domain.Equipo;
import co.eci.c15.salas.domain.EquipoRepository;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Repositorio de equipos en memoria para los tests de casos de uso. */
class EquiposEnMemoria implements EquipoRepository {

    private final Map<String, Equipo> equipos = new LinkedHashMap<>();

    @Override
    public Equipo save(Equipo equipo) {
        equipos.put(equipo.getId(), equipo);
        return equipo;
    }

    @Override
    public Optional<Equipo> findById(String id) {
        return Optional.ofNullable(equipos.get(id));
    }

    @Override
    public List<Equipo> findBySalaId(String salaId) {
        return equipos.values().stream()
                .filter(e -> e.getSalaId().equals(salaId))
                .sorted(Comparator.comparingInt(Equipo::getNumero))
                .toList();
    }

    @Override
    public void delete(String id) {
        equipos.remove(id);
    }
}
