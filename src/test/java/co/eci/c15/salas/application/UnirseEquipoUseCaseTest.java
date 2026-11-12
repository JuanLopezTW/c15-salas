package co.eci.c15.salas.application;

import co.eci.c15.salas.domain.Equipo;
import co.eci.c15.salas.domain.EquipoLlenoException;
import co.eci.c15.salas.domain.JugadorNoEnSalaException;
import co.eci.c15.salas.domain.PartidaYaIniciadaException;
import co.eci.c15.salas.domain.Sala;
import co.eci.c15.salas.domain.SalaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UnirseEquipoUseCaseTest {

    private EquiposEnMemoria equipos;
    private SalaRepository salas;
    private ApplicationEventPublisher events;
    private UnirseEquipoUseCase useCase;
    private Sala sala;
    private Equipo equipo1;
    private Equipo equipo2;

    @BeforeEach
    void setUp() {
        equipos = new EquiposEnMemoria();
        salas = mock(SalaRepository.class);
        events = mock(ApplicationEventPublisher.class);
        useCase = new UnirseEquipoUseCase(equipos, salas, events);
        sala = Sala.crear("Sala 1", "anfitrion-1");
        for (int i = 1; i <= 4; i++) sala.unirJugador("user-" + i);
        when(salas.findById(sala.getId())).thenReturn(Optional.of(sala));
        equipo1 = equipos.save(Equipo.crear(sala.getId(), 1, 3));
        equipo2 = equipos.save(Equipo.crear(sala.getId(), 2, 3));
    }

    @Test
    void unionExitosa() {
        EquipoDto dto = useCase.ejecutar(equipo1.getId(), "user-1");
        assertTrue(dto.miembros().contains("user-1"));
        verify(events).publishEvent(new SalaActualizadaEvent(sala.getId()));
    }

    @Test
    void siYaEstaEnElEquipoNoPublicaEvento() {
        useCase.ejecutar(equipo1.getId(), "user-1");
        clearInvocations(events);
        useCase.ejecutar(equipo1.getId(), "user-1");
        verifyNoInteractions(events);
    }

    @Test
    void equipoLlenoLanzaExcepcion() {
        useCase.ejecutar(equipo1.getId(), "user-1");
        useCase.ejecutar(equipo1.getId(), "user-2");
        useCase.ejecutar(equipo1.getId(), "user-3");
        assertThrows(EquipoLlenoException.class, () -> useCase.ejecutar(equipo1.getId(), "user-4"));
    }

    @Test
    void equipoInexistenteLanzaExcepcion() {
        assertThrows(IllegalArgumentException.class, () -> useCase.ejecutar("no-existe", "user-1"));
    }

    @Test
    void jugadorFueraDeLaSalaNoPuedeUnirse() {
        assertThrows(JugadorNoEnSalaException.class, () -> useCase.ejecutar(equipo1.getId(), "intruso"));
        assertTrue(equipo1.isVacio());
        verifyNoInteractions(events);
    }

    @Test
    void conPartidaIniciadaNoSePuedeUnir() {
        sala.iniciarPartida();
        assertThrows(PartidaYaIniciadaException.class, () -> useCase.ejecutar(equipo1.getId(), "user-1"));
    }

    @Test
    void unirseAOtroEquipoLoCambiaDeEquipo() {
        useCase.ejecutar(equipo1.getId(), "user-1");
        useCase.ejecutar(equipo2.getId(), "user-1");
        assertFalse(equipo1.contieneMiembro("user-1"));
        assertTrue(equipo2.contieneMiembro("user-1"));
    }

    @Test
    void cambiarDeEquipoQuitaElListo() {
        useCase.ejecutar(equipo1.getId(), "user-1");
        equipo1.marcarListo("user-1");

        useCase.ejecutar(equipo2.getId(), "user-1");

        assertFalse(equipo1.isMiembroListo("user-1"));
        assertFalse(equipo2.isMiembroListo("user-1"));
    }

    @Test
    void siElNuevoEquipoEstaLlenoSeQuedaEnElAnterior() {
        useCase.ejecutar(equipo1.getId(), "user-1");
        useCase.ejecutar(equipo2.getId(), "user-2");
        useCase.ejecutar(equipo2.getId(), "user-3");
        useCase.ejecutar(equipo2.getId(), "user-4");

        assertThrows(EquipoLlenoException.class, () -> useCase.ejecutar(equipo2.getId(), "user-1"));
        assertTrue(equipo1.contieneMiembro("user-1"));
    }
}