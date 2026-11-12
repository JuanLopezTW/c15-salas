package co.eci.c15.salas.application;

import co.eci.c15.salas.domain.Equipo;

import java.util.Set;

/** {@code listos}: miembros que marcaron listo; {@code listo}: el equipo completo esta listo (HU-63.4). */
public record EquipoDto(String id, String salaId, int numero, int cupoMaximo, Set<String> miembros,
                        Set<String> listos, boolean listo) {
    public static EquipoDto from(Equipo equipo) {
        return new EquipoDto(equipo.getId(), equipo.getSalaId(), equipo.getNumero(),
                equipo.getCupoMaximo(), equipo.getMiembros(), equipo.getListos(), equipo.isListo());
    }
}
