package co.eci.c15.salas.infrastructure.persistence;

import jakarta.persistence.*;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "equipos", uniqueConstraints = @UniqueConstraint(columnNames = {"sala_id", "numero"}))
class EquipoJpa {

    @Id
    private String id;
    @Column(name = "sala_id", nullable = false)
    private String salaId;
    @Column(nullable = false)
    private int numero;
    @Column(nullable = false)
    private int cupoMaximo;
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "equipo_miembros", joinColumns = @JoinColumn(name = "equipo_id"))
    @Column(name = "user_id", nullable = false)
    private Set<String> miembros = new HashSet<>();
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "equipo_listos", joinColumns = @JoinColumn(name = "equipo_id"))
    @Column(name = "user_id", nullable = false)
    private Set<String> listos = new HashSet<>();

    protected EquipoJpa() {}

    EquipoJpa(String id, String salaId, int numero, int cupoMaximo, Set<String> miembros, Set<String> listos) {
        this.id = id;
        this.salaId = salaId;
        this.numero = numero;
        this.cupoMaximo = cupoMaximo;
        this.miembros = new HashSet<>(miembros);
        this.listos = new HashSet<>(listos);
    }

    String getId() { return id; }
    String getSalaId() { return salaId; }
    int getNumero() { return numero; }
    int getCupoMaximo() { return cupoMaximo; }
    Set<String> getMiembros() { return miembros; }
    Set<String> getListos() { return listos; }
}