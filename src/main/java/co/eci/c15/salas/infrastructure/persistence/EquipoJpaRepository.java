package co.eci.c15.salas.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

interface EquipoJpaRepository extends JpaRepository<EquipoJpa, String> {
    List<EquipoJpa> findBySalaIdOrderByNumero(String salaId);
}