package co.eci.c15.salas.domain;

public class JugadorSinEquipoException extends RuntimeException {
    public JugadorSinEquipoException(String userId, String salaId) {
        super("El jugador " + userId + " no tiene equipo en la sala " + salaId + "; primero debe unirse a un equipo");
    }
}
