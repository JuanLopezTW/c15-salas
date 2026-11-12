package co.eci.c15.salas.domain;

public class SalaNoEncontradaException extends RuntimeException {
    public SalaNoEncontradaException(String salaId) {
        super("Sala no encontrada: " + salaId);
    }
}
