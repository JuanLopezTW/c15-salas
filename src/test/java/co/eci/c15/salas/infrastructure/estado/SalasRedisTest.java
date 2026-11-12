package co.eci.c15.salas.infrastructure.estado;

import co.eci.c15.common.events.PartidaIniciadaEvent;
import co.eci.c15.salas.application.FinalizarPartidaUseCase;
import co.eci.c15.salas.domain.Sala;
import co.eci.c15.salas.domain.SalaRepository;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.TaskScheduler;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** Cuenta regresiva, presencia por sala y barrendero contra un Redis real; sin Docker se salta. */
@Testcontainers(disabledWithoutDocker = true)
class SalasRedisTest {

    @SuppressWarnings("resource")
    static final GenericContainer<?> REDIS = new GenericContainer<>("redis:8.8.2-alpine").withExposedPorts(6379);
    static LettuceConnectionFactory conexion;
    static StringRedisTemplate redis;

    private static final Instant AHORA = Instant.parse("2026-11-03T15:00:00Z");

    private TaskScheduler reloj;
    private SalaRepository salas;
    private FinalizarPartidaUseCase finalizar;
    private List<String> cerradas;

    @BeforeAll
    static void arrancarRedis() {
        if (!REDIS.isRunning()) REDIS.start();
        conexion = new LettuceConnectionFactory(REDIS.getHost(), REDIS.getMappedPort(6379));
        conexion.afterPropertiesSet();
        conexion.start();
        redis = new StringRedisTemplate(conexion);
    }

    @AfterAll
    static void cerrar() {
        conexion.destroy();
    }

    @BeforeEach
    void setUp() {
        redis.getConnectionFactory().getConnection().serverCommands().flushAll();
        reloj = mock(TaskScheduler.class);
        when(reloj.getClock()).thenReturn(Clock.fixed(AHORA, ZoneOffset.UTC));
        salas = mock(SalaRepository.class);
        cerradas = new ArrayList<>();
        finalizar = mock(FinalizarPartidaUseCase.class);
        when(finalizar.ejecutar(any())).thenAnswer(inv -> cerradas.add(inv.getArgument(0)));
    }

    private BarrenderoSalas barrendero() {
        return new BarrenderoSalas(salas, finalizar, new PresenciaSalaRedis(redis, reloj), redis, reloj,
                Duration.ofSeconds(60), Duration.ofMinutes(10));
    }

    private static Sala sala(String id, Sala.Estado estado) {
        return Sala.reconstituir(id, "Sala " + id, "host", estado, 2, 2, 5, List.of("host"));
    }

    // ---------- cuenta regresiva ----------

    @Test
    void laCuentaLaEmpiezaUnaInstanciaYCualquieraLaCancela() {
        CuentasRegresivasRedis instanciaA = new CuentasRegresivasRedis(redis);
        CuentasRegresivasRedis instanciaB = new CuentasRegresivasRedis(redis);

        assertTrue(instanciaA.empezar("s1", "token-a", Duration.ofSeconds(15)));
        assertFalse(instanciaB.empezar("s1", "token-b", Duration.ofSeconds(15)));
        assertTrue(instanciaB.enCurso("s1"));
        long vida = redis.getExpire(CuentasRegresivasRedis.clave("s1"), TimeUnit.SECONDS);
        assertTrue(vida > 0 && vida <= 15, "caduca en " + vida);

        assertFalse(instanciaB.terminar("s1", "token-b"), "solo la termina quien la empezo");
        assertTrue(instanciaB.cancelar("s1"));
        assertFalse(instanciaA.terminar("s1", "token-a"), "cancelada: ya no arranca");
        assertFalse(instanciaA.cancelar("s1"));
        assertFalse(instanciaA.enCurso("s1"));
    }

    @Test
    void quienLaEmpezoLaTerminaUnaSolaVez() {
        CuentasRegresivasRedis cuentas = new CuentasRegresivasRedis(redis);
        cuentas.empezar("s1", "token-a", Duration.ofSeconds(15));

        assertTrue(cuentas.terminar("s1", "token-a"));
        assertFalse(cuentas.terminar("s1", "token-a"));
    }

    // ---------- presencia por sala ----------

    @Test
    void hayAlguienEnLaSalaSiCualquierJugadorTieneUnaConexionVigente() {
        PresenciaSalaRedis presencia = new PresenciaSalaRedis(redis, reloj);
        assertFalse(presencia.hayAlguien("s1"));

        presencia.entrar("sesion-1", "s1", "auth0|ana");
        assertTrue(presencia.hayAlguien("s1"));
        assertFalse(presencia.hayAlguien("s2"));

        presencia.salir("sesion-1", "s1", "auth0|ana");
        assertFalse(presencia.hayAlguien("s1"));
    }

    // ---------- barrendero ----------

    @Test
    void unaSalaEnEsperaSinNadieSeCierraEnLaSegundaRevision() {
        when(salas.findAll()).thenReturn(List.of(sala("s1", Sala.Estado.DISPONIBLE)));
        BarrenderoSalas barrendero = barrendero();

        barrendero.barrer();
        assertTrue(cerradas.isEmpty(), "la primera vez solo se anota: puede ser recien creada");

        redis.delete(BarrenderoSalas.TURNO); // pasa el intervalo
        barrendero.barrer();
        assertEquals(List.of("s1"), cerradas);
        assertFalse(redis.hasKey(BarrenderoSalas.sinNadie("s1")));
    }

    @Test
    void unaSalaConAlguienConectadoNoSeCierra() {
        when(salas.findAll()).thenReturn(List.of(sala("s1", Sala.Estado.DISPONIBLE)));
        BarrenderoSalas barrendero = barrendero();
        barrendero.barrer();
        new PresenciaSalaRedis(redis, reloj).entrar("sesion-1", "s1", "host"); // el lobby se conecto

        redis.delete(BarrenderoSalas.TURNO);
        barrendero.barrer();

        assertTrue(cerradas.isEmpty());
        assertFalse(redis.hasKey(BarrenderoSalas.sinNadie("s1")), "se borra la anotacion");
    }

    @Test
    void unaSalaEnPartidaSinPartidaVivaSeCierraYConPartidaNo() {
        when(salas.findAll()).thenReturn(List.of(sala("viva", Sala.Estado.EN_PARTIDA), sala("perdida", Sala.Estado.EN_PARTIDA)));
        BarrenderoSalas barrendero = barrendero();
        barrendero.onPartidaIniciada(new PartidaIniciadaEvent("viva", Map.of("e1", List.of("host")), Duration.ofMinutes(15)));
        long vida = redis.getExpire(BarrenderoSalas.enPartida("viva"), TimeUnit.SECONDS);
        assertTrue(vida > 1450 && vida <= 1500, "la marca dura lo que eligio el anfitrion + margen: " + vida);

        barrendero.barrer();

        assertEquals(List.of("perdida"), cerradas);
    }

    @Test
    void soloUnaInstanciaBarrePorVuelta() {
        when(salas.findAll()).thenReturn(List.of(sala("perdida", Sala.Estado.EN_PARTIDA)));
        BarrenderoSalas instanciaA = barrendero();
        BarrenderoSalas instanciaB = barrendero();

        instanciaA.barrer();
        instanciaB.barrer();

        assertEquals(1, cerradas.size());
        verify(salas, times(1)).findAll();
    }

    @Test
    void unErrorNoDetieneElBarridoYAlArrancarSeProgramaSolo() {
        when(salas.findAll()).thenThrow(new IllegalStateException("sin base de datos"));
        BarrenderoSalas barrendero = barrendero();

        assertDoesNotThrow(barrendero::barrer);
        barrendero.programar();
        verify(reloj).scheduleAtFixedRate(any(Runnable.class), eq(Duration.ofSeconds(60)));
    }
}
