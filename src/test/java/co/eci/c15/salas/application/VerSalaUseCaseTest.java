package co.eci.c15.salas.application;

import co.eci.c15.salas.domain.Equipo;
import co.eci.c15.salas.domain.PartidaYaIniciadaException;
import co.eci.c15.salas.domain.Sala;
import co.eci.c15.salas.domain.SalaNoEncontradaException;
import co.eci.c15.salas.domain.SalaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class VerSalaUseCaseTest {

    private EquiposEnMemoria equipos;
    private SalaRepository salas;
    private VerSalaUseCase useCase;
    private Sala sala;
    private Equipo equipo1;
    private Equipo equipo2;

    @BeforeEach
    void setUp() {
        equipos = new EquiposEnMemoria();
        salas = mock(SalaRepository.class);
        useCase = new VerSalaUseCase(salas, equipos);
        sala = Sala.crear("Sala lobby", "host");
        for (String u : List.of("host", "ana", "beto", "caro")) sala.unirJugador(u);
        when(salas.findById(sala.getId())).thenReturn(Optional.of(sala));
        when(salas.findById("no-existe")).thenReturn(Optional.empty());
        equipo2 = equipos.save(Equipo.crear(sala.getId(), 2, 4));
        equipo1 = equipos.save(Equipo.crear(sala.getId(), 1, 4));
        equipo1.unirMiembro("host");
        equipo1.unirMiembro("ana");
        equipo2.unirMiembro("beto");
    }

    @Test
    void devuelveLaSalaConSusEquiposYMiembros() {
        SalaDetalleDto dto = useCase.ejecutar(sala.getId());

        assertEquals(sala.getId(), dto.id());
        assertEquals("Sala lobby", dto.nombre());
        assertEquals("host", dto.anfitrionId());
        assertEquals("DISPONIBLE", dto.estado());
        assertEquals(4, dto.cupoTotal());
        assertEquals(5, dto.minutos());
        assertEquals(List.of("ana", "beto", "caro", "host"), dto.jugadores());
        assertEquals(2, dto.equipos().size());
        assertEquals(1, dto.equipos().get(0).numero());
        assertEquals(Set.of("host", "ana"), dto.equipos().get(0).miembros());
        assertEquals(Set.of("beto"), dto.equipos().get(1).miembros());
    }

    @Test
    void listaALosJugadoresSinEquipo() {
        assertEquals(List.of("caro"), useCase.ejecutar(sala.getId()).sinEquipo());
    }

    @Test
    void salaSinJugadoresTieneListasVacias() {
        Sala vacia = Sala.crear("Vacia", "host");
        when(salas.findById(vacia.getId())).thenReturn(Optional.of(vacia));

        SalaDetalleDto dto = useCase.ejecutar(vacia.getId());

        assertTrue(dto.jugadores().isEmpty());
        assertTrue(dto.sinEquipo().isEmpty());
        assertTrue(dto.equipos().isEmpty());
    }

    @Test
    void salaInexistenteLanzaExcepcion() {
        assertThrows(SalaNoEncontradaException.class, () -> useCase.ejecutar("no-existe"));
    }

    @Test
    void salaConPartidaIniciadaEstaCerrada() {
        sala.iniciarPartida();
        assertThrows(PartidaYaIniciadaException.class, () -> useCase.ejecutar(sala.getId()));
    }

    @Test
    void buscarAbiertaDevuelveElDetalleSoloSiLaSalaEstaDisponible() {
        assertTrue(useCase.buscarAbierta(sala.getId()).isPresent());
        assertTrue(useCase.buscarAbierta("no-existe").isEmpty());

        sala.iniciarPartida();
        assertTrue(useCase.buscarAbierta(sala.getId()).isEmpty());
    }
}
