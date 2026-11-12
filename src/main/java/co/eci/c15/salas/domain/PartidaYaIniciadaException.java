package co.eci.c15.salas.domain;

public class PartidaYaIniciadaException extends RuntimeException {
    public PartidaYaIniciadaException(String salaId) {
        super("La sala '" + salaId + "' ya tiene una partida en curso");
    }
}
