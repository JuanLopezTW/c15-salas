package co.eci.c15.salas.infrastructure.persistence;

import co.eci.c15.salas.domain.Equipo;
import co.eci.c15.salas.domain.EquipoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class EquipoRepositoryAdapter implements EquipoRepository {

    private final EquipoJpaRepository jpa;

    public EquipoRepositoryAdapter(EquipoJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Equipo save(Equipo equipo) {
        jpa.save(new EquipoJpa(equipo.getId(), equipo.getSalaId(), equipo.getNumero(),
                equipo.getCupoMaximo(), equipo.getMiembros(), equipo.getListos()));
        return equipo;
    }

    @Override
    public Optional<Equipo> findById(String id) {
        return jpa.findById(id).map(this::toDomain);
    }

    @Override
    public List<Equipo> findBySalaId(String salaId) {
        return jpa.findBySalaIdOrderByNumero(salaId).stream().map(this::toDomain).toList();
    }

    @Override
    public void delete(String id) {
        jpa.deleteById(id);
    }

    private Equipo toDomain(EquipoJpa e) {
        return Equipo.reconstituir(e.getId(), e.getSalaId(), e.getNumero(), e.getCupoMaximo(), e.getMiembros(), e.getListos());
    }
}