package co.eci.c15.salas.application;

import co.eci.c15.salas.domain.Sala;
import co.eci.c15.salas.domain.SalaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ListarSalasUseCaseTest {

    private SalaRepository salas;
    private ListarSalasUseCase useCase;

    @BeforeEach
    void setUp() {
        salas = mock(SalaRepository.class);
        useCase = new ListarSalasUseCase(salas);
    }

    @Test
    void catalogoVacio() {
        when(salas.findAll()).thenReturn(List.of());
        assertTrue(useCase.ejecutar().isEmpty());
    }

    @Test
    void ordenaPorNombreSinImportarMayusculas() {
        when(salas.findAll()).thenReturn(List.of(
                Sala.crear("zeta", "h"), Sala.crear("Alfa", "h"), Sala.crear("beta", "h")));

        assertEquals(List.of("Alfa", "beta", "zeta"), useCase.ejecutar().stream().map(SalaDto::nombre).toList());
    }

    @Test
    void muestraCantidadDeJugadoresYCupo() {
        Sala sala = Sala.crear("Sala", "host");
        sala.unirJugador("ana");
        sala.unirJugador("beto");
        when(salas.findAll()).thenReturn(List.of(sala));

        SalaDto dto = useCase.ejecutar().get(0);

        assertEquals(2, dto.jugadoresActuales());
        assertEquals(4, dto.cupoTotal());
        assertEquals(5, dto.minutos());
        assertEquals("DISPONIBLE", dto.estado());
    }

    @Test
    void incluyeLasSalasEnPartidaConSuEstado() {
        Sala libre = Sala.crear("Libre", "host");
        Sala jugando = Sala.crear("Jugando", "host");
        jugando.iniciarPartida();
        when(salas.findAll()).thenReturn(List.of(libre, jugando));

        List<SalaDto> catalogo = useCase.ejecutar();

        assertEquals(List.of("EN_PARTIDA", "DISPONIBLE"), catalogo.stream().map(SalaDto::estado).toList());
    }
}
