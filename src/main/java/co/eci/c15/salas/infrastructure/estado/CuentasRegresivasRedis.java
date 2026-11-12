package co.eci.c15.salas.infrastructure.estado;

import co.eci.c15.salas.application.CuentasRegresivas;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.Objects;

/**
 * La cuenta de cada sala es una clave {@code c15:salas:{id}:cuenta} con el token de quien la empezo y una
 * vida corta (la cuenta + un margen): si esa instancia muere a mitad de cuenta, la clave vence sola y la
 * proxima evaluacion de la sala empieza otra.
 */
@Component
// Nunca convive con el adaptador en memoria (lo decide c15.estado.redis); @Primary solo le aclara
// al analisis estatico cual usar, porque no evalua el interruptor.
@ConditionalOnProperty(name = "c15.estado.redis", havingValue = "true")
@Primary
public class CuentasRegresivasRedis implements CuentasRegresivas {

    private static final RedisScript<Long> TERMINAR_SI_ES_MIA = RedisScript.of("""
            if redis.call('GET', KEYS[1]) == ARGV[1] then return redis.call('DEL', KEYS[1]) end
            return 0
            """, Long.class);

    private final StringRedisTemplate redis;

    public CuentasRegresivasRedis(StringRedisTemplate redis) {
        this.redis = redis;
    }

    static String clave(String salaId) {
        return "c15:salas:" + salaId + ":cuenta";
    }

    @Override
    public boolean empezar(String salaId, String token, Duration vida) {
        return Boolean.TRUE.equals(redis.opsForValue().setIfAbsent(clave(salaId), token, vida));
    }

    @Override
    public boolean enCurso(String salaId) {
        return Boolean.TRUE.equals(redis.hasKey(clave(salaId)));
    }

    @Override
    public boolean cancelar(String salaId) {
        return Boolean.TRUE.equals(redis.delete(clave(salaId)));
    }

    @Override
    public boolean terminar(String salaId, String token) {
        return Objects.equals(redis.execute(TERMINAR_SI_ES_MIA, List.of(clave(salaId)), token), 1L);
    }
}
