package co.eci.c15.salas.domain;

import java.util.List;
import java.util.Optional;

public interface SalaRepository {
    Sala save(Sala sala);
    Optional<Sala> findById(String id);
    List<Sala> findAll();
    void delete(String id);
}
