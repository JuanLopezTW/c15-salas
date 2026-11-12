package co.eci.c15.salas.domain;

import java.time.Duration;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public final class Sala {

    public enum Estado { DISPONIBLE, EN_PARTIDA }

    public static final int MIN_EQUIPOS = 2;
    public static final int MIN_JUGADORES_POR_EQUIPO = 2;
    /** Por estabilidad (Juan, 2026-11-04) las partidas son de 2 equipos de 2: se configura solo dentro de estos topes. */
    public static final int MAX_EQUIPOS = 2;
    public static final int MAX_JUGADORES_POR_EQUIPO = 2;
    /** Minutos que el anfitrion puede elegir para la partida. */
    public static final List<Integer> DURACIONES_MINUTOS = List.of(5, 10, 15);
    public static final int DURACION_DEFECTO_MINUTOS = 5;

    private final String id;
    private final String nombre;
    private final String anfitrionId;
    private Estado estado;
    private int numEquipos;
    private int jugadoresPorEquipo;
    private final int duracionMinutos;
    private final Set<String> jugadores = new HashSet<>();

    private Sala(String id, String nombre, String anfitrionId, Estado estado,
                 int numEquipos, int jugadoresPorEquipo, int duracionMinutos) {
        this.id = id;
        this.nombre = nombre;
        this.anfitrionId = anfitrionId;
        this.estado = estado;
        this.numEquipos = numEquipos;
        this.jugadoresPorEquipo = jugadoresPorEquipo;
        this.duracionMinutos = duracionMinutos;
    }

    public static Sala crear(String nombre, String anfitrionId) {
        return crear(nombre, anfitrionId, null);
    }

    /** @param minutos lo que dura la partida (uno de {@link #DURACIONES_MINUTOS}); null = {@link #DURACION_DEFECTO_MINUTOS} */
    public static Sala crear(String nombre, String anfitrionId, Integer minutos) {
        if (nombre == null || nombre.isBlank()) throw new IllegalArgumentException("El nombre de la sala no puede estar vacío");
        if (anfitrionId == null || anfitrionId.isBlank()) throw new IllegalArgumentException("El anfitrionId es obligatorio");
        int duracion = minutos == null ? DURACION_DEFECTO_MINUTOS : minutos;
        if (!DURACIONES_MINUTOS.contains(duracion)) {
            throw new ConfiguracionInvalidaException("La partida puede durar " + DURACIONES_MINUTOS + " minutos");
        }
        return new Sala(UUID.randomUUID().toString(), nombre.trim(), anfitrionId, Estado.DISPONIBLE,
                MAX_EQUIPOS, MAX_JUGADORES_POR_EQUIPO, duracion);
    }

    /** Reconstruye una sala ya existente (desde persistencia) conservando su id y su estado. */
    public static Sala reconstituir(String id, String nombre, String anfitrionId, Estado estado, int numEquipos,
                                    int jugadoresPorEquipo, int duracionMinutos, Collection<String> jugadores) {
        Sala sala = new Sala(Objects.requireNonNull(id), nombre, anfitrionId, estado, numEquipos, jugadoresPorEquipo,
                duracionMinutos);
        sala.jugadores.addAll(jugadores);
        return sala;
    }

    public void configurar(String solicitanteId, int numEquipos, int jugadoresPorEquipo) {
        if (!anfitrionId.equals(solicitanteId)) throw new NoEsAnfitrionException(solicitanteId);
        if (estado == Estado.EN_PARTIDA) throw new PartidaYaIniciadaException(id);
        if (numEquipos < MIN_EQUIPOS || numEquipos > MAX_EQUIPOS) {
            throw new ConfiguracionInvalidaException("El número de equipos debe estar entre " + MIN_EQUIPOS + " y " + MAX_EQUIPOS);
        }
        if (jugadoresPorEquipo < MIN_JUGADORES_POR_EQUIPO || jugadoresPorEquipo > MAX_JUGADORES_POR_EQUIPO) {
            throw new ConfiguracionInvalidaException("Los jugadores por equipo deben estar entre "
                    + MIN_JUGADORES_POR_EQUIPO + " y " + MAX_JUGADORES_POR_EQUIPO);
        }
        if (numEquipos * jugadoresPorEquipo < jugadores.size()) {
            throw new ConfiguracionInvalidaException("El nuevo cupo (" + numEquipos * jugadoresPorEquipo
                    + ") es menor que los jugadores que ya están en la sala (" + jugadores.size() + ")");
        }
        this.numEquipos = numEquipos;
        this.jugadoresPorEquipo = jugadoresPorEquipo;
    }

    public void unirJugador(String userId) {
        if (userId == null || userId.isBlank()) throw new IllegalArgumentException("El userId es obligatorio");
        if (estado == Estado.EN_PARTIDA) throw new PartidaYaIniciadaException(id);
        if (jugadores.contains(userId)) return;
        if (jugadores.size() >= numEquipos * jugadoresPorEquipo) throw new SalaLlenaException(id);
        jugadores.add(userId);
    }

    public boolean contieneJugador(String userId) { return jugadores.contains(userId); }

    /** El jugador deja la sala (solo en el lobby). */
    public void retirarJugador(String userId) {
        if (estado == Estado.EN_PARTIDA) throw new PartidaYaIniciadaException(id);
        jugadores.remove(userId);
    }

    public boolean esAnfitrion(String userId) { return anfitrionId.equals(userId); }

    /**
     * Lista para iniciar (HU-64.4): todos los jugadores de la sala tienen equipo, hay al menos
     * {@link #MIN_EQUIPOS} equipos con jugadores y todos esos estan listos. Los equipos vacios no cuentan.
     */
    public boolean listaParaIniciar(Collection<Equipo> equipos) {
        if (!isDisponible()) return false;
        List<Equipo> conJugadores = equipos.stream().filter(e -> !e.isVacio()).toList();
        if (conJugadores.size() < MIN_EQUIPOS) return false;
        Set<String> conEquipo = conJugadores.stream()
                .flatMap(e -> e.getMiembros().stream())
                .collect(Collectors.toSet());
        return conEquipo.containsAll(jugadores) && conJugadores.stream().allMatch(Equipo::isListo);
    }

    public void iniciarPartida() { this.estado = Estado.EN_PARTIDA; }

    public String getId() { return id; }
    public String getNombre() { return nombre; }
    public String getAnfitrionId() { return anfitrionId; }
    public Estado getEstado() { return estado; }
    public boolean isDisponible() { return estado == Estado.DISPONIBLE; }
    public int getNumEquipos() { return numEquipos; }
    public int getJugadoresPorEquipo() { return jugadoresPorEquipo; }
    public Set<String> getJugadores() { return Collections.unmodifiableSet(jugadores); }
    public int getCupoTotal() { return numEquipos * jugadoresPorEquipo; }
    public int getDuracionMinutos() { return duracionMinutos; }
    public Duration getDuracion() { return Duration.ofMinutes(duracionMinutos); }
}