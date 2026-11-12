package co.eci.c15.salas.application;

import co.eci.c15.common.events.EventosEntreServicios;
import co.eci.c15.common.events.SalaCerradaEvent;
import co.eci.c15.salas.domain.Equipo;
import co.eci.c15.salas.domain.Sala;
import co.eci.c15.salas.domain.SalaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class SalirDeSalaUseCaseTest {

    private EquiposEnMemoria equipos;
    private SalaRepository salas;
    private ArranquePartidaNotifier notifier;
    private ApplicationEventPublisher events;
    private EventosEntreServicios entreServicios;
    private SalirDeSalaUseCase useCase;
    private Sala sala;
    private Equipo equipo1;
    private Equipo equipo2;

    @BeforeEach
    void setUp() {
        equipos = new EquiposEnMemoria();
        salas = mock(SalaRepository.class);
        notifier = mock(ArranquePartidaNotifier.class);
        events = mock(ApplicationEventPublisher.class);
        entreServicios = mock(EventosEntreServicios.class);
        useCase = new SalirDeSalaUseCase(salas, equipos, notifier, events, entreServicios);

        sala = Sala.crear("Sala", "host");
        for (String u : new String[]{"host", "ana", "beto"}) sala.unirJugador(u);
        when(salas.findById(sala.getId())).thenReturn(Optional.of(sala));
        when(salas.findById("no-existe")).thenReturn(Optional.empty());
        equipo1 = equipos.save(Equipo.crear(sala.getId(), 1, 4));
        equipo2 = equipos.save(Equipo.crear(sala.getId(), 2, 4));
        equipo1.unirMiembro("host");
        equipo1.unirMiembro("ana");
        equipo1.marcarListo("ana");
        equipo2.unirMiembro("beto");
    }

    @Test
    void unJugadorSaleDeLaSalaYDeSuEquipo() {
        assertEquals(SalirDeSalaUseCase.Resultado.SALIO, useCase.ejecutar(sala.getId(), "ana"));

        assertFalse(sala.contieneJugador("ana"));
        assertEquals(Set.of("host"), equipos.findById(equipo1.getId()).orElseThrow().getMiembros());
        assertFalse(equipos.findById(equipo1.getId()).orElseThrow().isMiembroListo("ana"));
        verify(salas).save(sala);
        verify(events).publishEvent(new SalaActualizadaEvent(sala.getId()));
        verify(salas, never()).delete(any());
        verifyNoInteractions(entreServicios);
        verifyNoInteractions(notifier);
    }

    @Test
    void siSeVaElAnfitrionSeCierraLaSalaYSeAvisa() {
        assertEquals(SalirDeSalaUseCase.Resultado.SALA_CERRADA, useCase.ejecutar(sala.getId(), "host"));

        verify(salas).delete(sala.getId());
        assertTrue(equipos.findBySalaId(sala.getId()).isEmpty());
        verify(notifier).cerrada(sala.getId());
        verify(events).publishEvent(new SalaActualizadaEvent(sala.getId()));
        verify(entreServicios).publicar(new SalaCerradaEvent(sala.getId()));
    }

    @Test
    void sabeQuienEsElAnfitrion() {
        assertTrue(useCase.esAnfitrion(sala.getId(), "host"));
        assertFalse(useCase.esAnfitrion(sala.getId(), "ana"));
        assertFalse(useCase.esAnfitrion("no-existe", "host"));
    }

    @Test
    void conLaPartidaIniciadaNoHaceNada() {
        sala.iniciarPartida();

        assertEquals(SalirDeSalaUseCase.Resultado.NADA, useCase.ejecutar(sala.getId(), "host"));
        assertEquals(SalirDeSalaUseCase.Resultado.NADA, useCase.ejecutar(sala.getId(), "ana"));

        assertTrue(sala.contieneJugador("ana"));
        verify(salas, never()).delete(any());
        verify(salas, never()).save(any());
        verifyNoInteractions(notifier, events, entreServicios);
    }

    @Test
    void salaInexistenteOJugadorAjenoNoHacenNada() {
        assertEquals(SalirDeSalaUseCase.Resultado.NADA, useCase.ejecutar("no-existe", "ana"));
        assertEquals(SalirDeSalaUseCase.Resultado.NADA, useCase.ejecutar(sala.getId(), "extraño"));

        verify(salas, never()).save(any());
        verifyNoInteractions(notifier, events, entreServicios);
    }

    @Test
    void unJugadorSinEquipoTambienSale() {
        sala.unirJugador("caro");

        assertEquals(SalirDeSalaUseCase.Resultado.SALIO, useCase.ejecutar(sala.getId(), "caro"));

        assertFalse(sala.contieneJugador("caro"));
    }
}
