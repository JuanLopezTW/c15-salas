package co.eci.c15.salas.infrastructure.persistence;

import co.eci.c15.salas.domain.Sala;
import co.eci.c15.salas.domain.SalaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class SalaRepositoryAdapter implements SalaRepository {

    private final SalaJpaRepository jpa;

    public SalaRepositoryAdapter(SalaJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Sala save(Sala sala) {
        jpa.save(new SalaJpa(sala.getId(), sala.getNombre(), sala.getAnfitrionId(), sala.getEstado(),
                sala.getNumEquipos(), sala.getJugadoresPorEquipo(), sala.getDuracionMinutos(), sala.getJugadores()));
        return sala;
    }

    @Override
    public Optional<Sala> findById(String id) {
        return jpa.findById(id).map(this::toDomain);
    }

    @Override
    public List<Sala> findAll() {
        return jpa.findAll().stream().map(this::toDomain).toList();
    }

    @Override
    public void delete(String id) {
        jpa.deleteById(id);
    }

    private Sala toDomain(SalaJpa e) {
        return Sala.reconstituir(e.getId(), e.getNombre(), e.getAnfitrionId(), e.getEstado(),
                e.getNumEquipos(), e.getJugadoresPorEquipo(),
                e.getDuracionMinutos() != null ? e.getDuracionMinutos() : Sala.DURACION_DEFECTO_MINUTOS, e.getJugadores());
    }
}