package co.eci.c15.salas.application;

import co.eci.c15.salas.domain.Sala;

public record SalaDto(String id, String nombre, String anfitrionId, String estado,
                      int numEquipos, int jugadoresPorEquipo, int jugadoresActuales, int cupoTotal, int minutos) {
    public static SalaDto from(Sala sala) {
        return new SalaDto(sala.getId(), sala.getNombre(), sala.getAnfitrionId(),
                sala.getEstado().name(), sala.getNumEquipos(), sala.getJugadoresPorEquipo(),
                sala.getJugadores().size(), sala.getCupoTotal(), sala.getDuracionMinutos());
    }
}
