package co.eci.c15.salas.domain;

public class EquipoLlenoException extends RuntimeException {
    public EquipoLlenoException(String equipoId) {
        super("El equipo '" + equipoId + "' está lleno");
    }
}
