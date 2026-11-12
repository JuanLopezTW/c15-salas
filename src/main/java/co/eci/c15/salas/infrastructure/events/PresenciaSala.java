package co.eci.c15.salas.infrastructure.events;

/**
 * Que jugadores tienen el lobby abierto en cada sala, segun las sesiones que avisa el broker (puede tener
 * varias replicas): un anfitrion que recarga puede volver por otra conexion antes de que llegue el cierre.
 */
public interface PresenciaSala {

    void entrar(String sessionId, String salaId, String userId);

    /** Quita la conexion; false si no estaba (cierre repetido o de una sesion desconocida). */
    boolean salir(String sessionId, String salaId, String userId);

    /** Si el jugador tiene al menos una conexion abierta en esa sala, en cualquier replica del broker. */
    boolean conectado(String salaId, String userId);

    /** Si alguien (quien sea) tiene el lobby de la sala abierto, en cualquier replica del broker. */
    boolean hayAlguien(String salaId);
}
