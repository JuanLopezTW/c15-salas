package co.eci.c15.salas.domain;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public final class Equipo {

    private final String id;
    private final String salaId;
    private final int numero;
    private int cupoMaximo;
    private final Set<String> miembros = new HashSet<>();
    private final Set<String> listos = new HashSet<>();

    public Equipo(String id, String salaId, int numero, int cupoMaximo) {
        this.id = Objects.requireNonNull(id);
        this.salaId = Objects.requireNonNull(salaId);
        this.numero = numero;
        this.cupoMaximo = cupoMaximo;
    }

    public static Equipo crear(String salaId, int numero, int cupoMaximo) {
        return new Equipo(UUID.randomUUID().toString(), salaId, numero, cupoMaximo);
    }

    /** Reconstruye un equipo ya existente (desde persistencia) con sus miembros y quienes estan listos. */
    public static Equipo reconstituir(String id, String salaId, int numero, int cupoMaximo,
                                      Collection<String> miembros, Collection<String> listos) {
        Equipo equipo = new Equipo(id, salaId, numero, cupoMaximo);
        equipo.miembros.addAll(miembros);
        listos.stream().filter(equipo.miembros::contains).forEach(equipo.listos::add);
        return equipo;
    }

    public void unirMiembro(String userId) {
        if (miembros.contains(userId)) return;
        if (miembros.size() >= cupoMaximo) throw new EquipoLlenoException(id);
        miembros.add(userId);
    }

    /** Al salir del equipo el jugador pierde su estado listo. */
    public void quitarMiembro(String userId) {
        miembros.remove(userId);
        listos.remove(userId);
    }

    public void marcarListo(String userId) {
        if (!miembros.contains(userId)) throw new JugadorSinEquipoException(userId, salaId);
        listos.add(userId);
    }

    public void desmarcarListo(String userId) {
        if (!miembros.contains(userId)) throw new JugadorSinEquipoException(userId, salaId);
        listos.remove(userId);
    }

    public boolean isMiembroListo(String userId) { return listos.contains(userId); }

    /** Listo cuando tiene al menos el minimo de jugadores y todos marcaron listo (HU-63.4). */
    public boolean isListo() {
        return miembros.size() >= Sala.MIN_JUGADORES_POR_EQUIPO && listos.containsAll(miembros);
    }

    public void cambiarCupo(int nuevoCupo) {
        if (nuevoCupo < miembros.size()) {
            throw new ConfiguracionInvalidaException("El equipo " + numero + " ya tiene " + miembros.size()
                    + " jugadores; el cupo no puede bajar a " + nuevoCupo);
        }
        this.cupoMaximo = nuevoCupo;
    }

    public boolean contieneMiembro(String userId) { return miembros.contains(userId); }
    public boolean isVacio() { return miembros.isEmpty(); }

    public String getId() { return id; }
    public String getSalaId() { return salaId; }
    public int getNumero() { return numero; }
    public int getCupoMaximo() { return cupoMaximo; }
    public Set<String> getMiembros() { return Collections.unmodifiableSet(miembros); }
    public Set<String> getListos() { return Collections.unmodifiableSet(listos); }
    public boolean isFull() { return miembros.size() >= cupoMaximo; }
}