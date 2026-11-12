package co.eci.c15.salas.application;

import co.eci.c15.salas.domain.Equipo;
import co.eci.c15.salas.domain.Sala;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Vista de la sala antes de la partida (HU-62.3): sus jugadores, los equipos con sus miembros
 * y quienes todavia no escogen equipo. Las listas de jugadores van ordenadas.
 */
public record SalaDetalleDto(String id, String nombre, String anfitrionId, String estado,
                             int numEquipos, int jugadoresPorEquipo, int cupoTotal, int minutos,
                             List<String> jugadores, List<String> sinEquipo, List<EquipoDto> equipos) {

    public static SalaDetalleDto from(Sala sala, List<Equipo> equipos) {
        Set<String> conEquipo = equipos.stream()
                .flatMap(e -> e.getMiembros().stream())
                .collect(Collectors.toSet());
        List<String> jugadores = sala.getJugadores().stream().sorted().toList();
        return new SalaDetalleDto(sala.getId(), sala.getNombre(), sala.getAnfitrionId(), sala.getEstado().name(),
                sala.getNumEquipos(), sala.getJugadoresPorEquipo(), sala.getCupoTotal(), sala.getDuracionMinutos(),
                jugadores,
                jugadores.stream().filter(j -> !conEquipo.contains(j)).toList(),
                equipos.stream().map(EquipoDto::from).toList());
    }
}
