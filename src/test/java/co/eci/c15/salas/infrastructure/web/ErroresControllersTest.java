package co.eci.c15.salas.infrastructure.web;

import co.eci.c15.salas.application.ConfigurarSalaUseCase;
import co.eci.c15.salas.application.CrearSalaUseCase;
import co.eci.c15.salas.application.ListarEquiposUseCase;
import co.eci.c15.salas.application.ListarSalasUseCase;
import co.eci.c15.salas.application.MarcarListoUseCase;
import co.eci.c15.salas.application.SalirDeSalaUseCase;
import co.eci.c15.salas.application.UnirseEquipoUseCase;
import co.eci.c15.salas.application.UnirseSalaUseCase;
import co.eci.c15.salas.application.VerSalaUseCase;
import co.eci.c15.salas.domain.EquipoLlenoException;
import co.eci.c15.salas.domain.NoEsAnfitrionException;
import co.eci.c15.salas.domain.PartidaYaIniciadaException;
import co.eci.c15.salas.domain.SalaLlenaException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.security.Principal;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Cada error de los casos de uso llega al cliente con su codigo HTTP y el mensaje en "error". */
class ErroresControllersTest {

    private static final Principal ANA = () -> "ana";
    private static final Map<String, Object> DOS_POR_DOS = Map.of("numEquipos", 2, "jugadoresPorEquipo", 2);

    private UnirseEquipoUseCase unirseEquipo;
    private ConfigurarSalaUseCase configurar;
    private UnirseSalaUseCase unirseSala;
    private MarcarListoUseCase listo;
    private EquipoController equipos;
    private SalaController salas;

    @BeforeEach
    void preparar() {
        unirseEquipo = mock(UnirseEquipoUseCase.class);
        configurar = mock(ConfigurarSalaUseCase.class);
        unirseSala = mock(UnirseSalaUseCase.class);
        listo = mock(MarcarListoUseCase.class);
        equipos = new EquipoController(unirseEquipo);
        salas = new SalaController(mock(CrearSalaUseCase.class), mock(ListarSalasUseCase.class), configurar,
                unirseSala, mock(ListarEquiposUseCase.class), mock(VerSalaUseCase.class), listo,
                mock(SalirDeSalaUseCase.class));
    }

    @Test
    void equipoLlenoEs409() {
        when(unirseEquipo.ejecutar("e1", "ana")).thenThrow(new EquipoLlenoException("e1"));
        verificar(equipos.unirse("e1", ANA), 409);
    }

    @Test
    void equipoInvalidoEs400() {
        when(unirseEquipo.ejecutar("e1", "ana")).thenThrow(new IllegalArgumentException("Equipo no encontrado"));
        verificar(equipos.unirse("e1", ANA), 400);
    }

    @Test
    void configurarSinSerAnfitrionEs403() {
        when(configurar.ejecutar(anyString(), anyString(), anyInt(), anyInt())).thenThrow(new NoEsAnfitrionException("ana"));
        verificar(salas.configurar("s1", DOS_POR_DOS, ANA), 403);
    }

    @Test
    void unirseASalaLlenaEs409() {
        when(unirseSala.ejecutar("s1", "ana")).thenThrow(new SalaLlenaException("s1"));
        verificar(salas.unirse("s1", ANA), 409);
    }

    @Test
    void unirseASalaEnPartidaEs403() {
        when(unirseSala.ejecutar("s1", "ana")).thenThrow(new PartidaYaIniciadaException("s1"));
        verificar(salas.unirse("s1", ANA), 403);
    }

    @Test
    void unirseASalaInexistenteEs400() {
        when(unirseSala.ejecutar("s1", "ana")).thenThrow(new IllegalArgumentException("Sala no encontrada"));
        verificar(salas.unirse("s1", ANA), 400);
    }

    @Test
    void listoConDatosInvalidosEs400() {
        when(listo.marcar("s1", "ana")).thenThrow(new IllegalArgumentException("Dato invalido"));
        verificar(salas.marcarListo("s1", ANA), 400);
    }

    @Test
    void desmarcarEnPartidaEs409() {
        when(listo.desmarcar("s1", "ana")).thenThrow(new PartidaYaIniciadaException("s1"));
        verificar(salas.desmarcarListo("s1", ANA), 409);
    }

    private static void verificar(ResponseEntity<?> respuesta, int codigo) {
        assertEquals(codigo, respuesta.getStatusCode().value());
        Map<?, ?> cuerpo = (Map<?, ?>) respuesta.getBody();
        assertEquals(1, cuerpo.size(), "solo trae el mensaje de error");
    }
}
