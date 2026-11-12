package co.eci.c15.salas.infrastructure.estado;

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
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** Dos "instancias" (dos adaptadores) sobre el mismo Redis real; sin Docker se salta. */
@Testcontainers(disabledWithoutDocker = true)
class PresenciaSalaRedisTest {

    @SuppressWarnings("resource")
    static final GenericContainer<?> REDIS = new GenericContainer<>("redis:8.8.2-alpine").withExposedPorts(6379);

    static LettuceConnectionFactory conexion;
    static StringRedisTemplate redis;

    private static final Instant AHORA = Instant.parse("2026-11-03T15:00:00Z");

    private TaskScheduler relojA;
    private TaskScheduler relojB;
    private PresenciaSalaRedis instanciaA;
    private PresenciaSalaRedis instanciaB;

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

    private static TaskScheduler reloj(Instant instante) {
        TaskScheduler scheduler = mock(TaskScheduler.class);
        when(scheduler.getClock()).thenReturn(Clock.fixed(instante, ZoneOffset.UTC));
        return scheduler;
    }

    @BeforeEach
    void setUp() {
        redis.getConnectionFactory().getConnection().serverCommands().flushAll();
        relojA = reloj(AHORA);
        relojB = reloj(AHORA);
        instanciaA = new PresenciaSalaRedis(redis, relojA);
        instanciaB = new PresenciaSalaRedis(redis, relojB);
    }

    @Test
    void unaInstanciaVeLaConexionQueAbrioOtra() {
        instanciaA.entrar("sesion-1", "s1", "auth0|host");

        assertTrue(instanciaB.conectado("s1", "auth0|host"));
        assertFalse(instanciaB.conectado("s1", "auth0|otro"));
        assertFalse(instanciaB.conectado("s2", "auth0|host"));
    }

    @Test
    void alSalirDeLaUnicaConexionYaNoEstaConectado() {
        instanciaA.entrar("sesion-1", "s1", "host");
        instanciaB.entrar("sesion-2", "s1", "host");

        instanciaA.salir("sesion-1", "s1", "host");
        assertTrue(instanciaA.conectado("s1", "host"), "sigue la pestaña de la otra instancia");

        instanciaB.salir("sesion-2", "s1", "host");
        assertFalse(instanciaA.conectado("s1", "host"));
    }

    @Test
    void siUnaInstanciaMuereSusConexionesDejanDeContarSolas() {
        instanciaA.entrar("sesion-1", "s1", "host");

        TaskScheduler despues = reloj(AHORA.plus(PresenciaSalaRedis.VIDA).plusSeconds(1));
        assertFalse(new PresenciaSalaRedis(redis, despues).conectado("s1", "host"));
    }

    @Test
    void cadaAvisoDelBrokerRenuevaLaConexionYLaClaveCaduca() {
        instanciaA.entrar("sesion-1", "s1", "host");
        long segundos = redis.getExpire(PresenciaSalaRedis.clave("s1", "host"), TimeUnit.SECONDS);
        assertTrue(segundos > 0 && segundos <= PresenciaSalaRedis.VIDA.toSeconds(), "caduca en " + segundos);

        // Pasa el tiempo y el broker repite el aviso (cada 30 s): la conexion sigue contando
        Instant luego = AHORA.plus(PresenciaSalaRedis.VIDA).minusSeconds(5);
        when(relojA.getClock()).thenReturn(Clock.fixed(luego, ZoneOffset.UTC));
        instanciaA.entrar("sesion-1", "s1", "host");

        TaskScheduler masTarde = reloj(AHORA.plus(PresenciaSalaRedis.VIDA).plusSeconds(10));
        assertTrue(new PresenciaSalaRedis(redis, masTarde).conectado("s1", "host"));
    }

    @Test
    void salirDiceSiLaConexionEstaba() {
        instanciaA.entrar("sesion-1", "s1", "host");

        assertTrue(instanciaB.salir("sesion-1", "s1", "host"));
        assertFalse(instanciaB.salir("sesion-1", "s1", "host"), "un cierre repetido no hace nada");
    }
}
