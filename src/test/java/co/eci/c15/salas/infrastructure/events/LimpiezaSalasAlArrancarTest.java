package co.eci.c15.salas.infrastructure.events;

import co.eci.c15.salas.application.FinalizarPartidaUseCase;
import co.eci.c15.salas.domain.Sala;
import co.eci.c15.salas.domain.SalaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class LimpiezaSalasAlArrancarTest {

    private SalaRepository salas;
    private FinalizarPartidaUseCase finalizar;
    private LimpiezaSalasAlArrancar limpieza;

    @BeforeEach
    void setUp() {
        salas = mock(SalaRepository.class);
        finalizar = mock(FinalizarPartidaUseCase.class);
        limpieza = new LimpiezaSalasAlArrancar(salas, finalizar);
    }

    @Test
    void borraLasSalasQueQuedaronEnEsperaYEnPartida() {
        Sala enEspera = Sala.crear("En espera", "ana");
        Sala enPartida = Sala.crear("En partida", "beto");
        enPartida.iniciarPartida();
        when(salas.findAll()).thenReturn(List.of(enEspera, enPartida));

        limpieza.limpiar();

        verify(finalizar).ejecutar(enEspera.getId());
        verify(finalizar).ejecutar(enPartida.getId());
    }

    @Test
    void sinSalasNoHaceNada() {
        when(salas.findAll()).thenReturn(List.of());

        limpieza.limpiar();

        verify(finalizar, never()).ejecutar(any());
    }

    @Test
    void unFalloNoTumbaElArranque() {
        when(salas.findAll()).thenThrow(new IllegalStateException("sin base de datos"));

        assertDoesNotThrow(limpieza::limpiar);
    }
}
