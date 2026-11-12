package co.eci.c15.salas.domain;

public class JugadorNoEnSalaException extends RuntimeException {
    public JugadorNoEnSalaException(String userId, String salaId) {
        super("El jugador " + userId + " no está en la sala " + salaId + "; primero debe unirse a la sala");
    }
}