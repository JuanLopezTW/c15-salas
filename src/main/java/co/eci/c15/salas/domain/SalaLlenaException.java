package co.eci.c15.salas.domain;

public class SalaLlenaException extends RuntimeException {
    public SalaLlenaException(String salaId) {
        super("La sala '" + salaId + "' está llena");
    }
}
