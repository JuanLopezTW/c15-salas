package co.eci.c15.salas.infrastructure.persistence;

import co.eci.c15.salas.domain.Sala;
import jakarta.persistence.*;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "salas")
class SalaJpa {

    @Id
    private String id;
    @Column(nullable = false)
    private String nombre;
    @Column(nullable = false)
    private String anfitrionId;
    @Enumerated(EnumType.STRING)
    private Sala.Estado estado;
    @Column(nullable = false)
    private int numEquipos;
    @Column(nullable = false)
    private int jugadoresPorEquipo;
    /** Nullable: las salas guardadas antes de que existiera la columna usan la duracion por defecto. */
    private Integer duracionMinutos;
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "sala_jugadores", joinColumns = @JoinColumn(name = "sala_id"))
    @Column(name = "user_id", nullable = false)
    private Set<String> jugadores = new HashSet<>();

    protected SalaJpa() {}

    SalaJpa(String id, String nombre, String anfitrionId, Sala.Estado estado,
            int numEquipos, int jugadoresPorEquipo, int duracionMinutos, Set<String> jugadores) {
        this.id = id;
        this.nombre = nombre;
        this.anfitrionId = anfitrionId;
        this.estado = estado;
        this.numEquipos = numEquipos;
        this.jugadoresPorEquipo = jugadoresPorEquipo;
        this.duracionMinutos = duracionMinutos;
        this.jugadores = new HashSet<>(jugadores);
    }

    String getId() { return id; }
    String getNombre() { return nombre; }
    String getAnfitrionId() { return anfitrionId; }
    Sala.Estado getEstado() { return estado; }
    int getNumEquipos() { return numEquipos; }
    int getJugadoresPorEquipo() { return jugadoresPorEquipo; }
    Integer getDuracionMinutos() { return duracionMinutos; }
    Set<String> getJugadores() { return jugadores; }
}