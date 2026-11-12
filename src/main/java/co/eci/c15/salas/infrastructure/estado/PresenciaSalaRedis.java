package co.eci.c15.salas.infrastructure.estado;

import co.eci.c15.salas.infrastructure.events.PresenciaSala;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Component;
import org.springframework.context.annotation.Primary;

import java.time.Duration;

/**
 * Por cada jugador en el lobby de una sala, un set ordenado de Redis con sus conexiones y la hora hasta la
 * que cada una vale. El broker repite el aviso de cada conexion abierta cada 30 s y cada aviso la renueva:
 * si el broker muere, sus conexiones dejan de contar solas en {@link #VIDA} (nadie tiene que limpiar) y la
 * clave entera caduca.
 */
@Component
// Nunca convive con el adaptador en memoria (lo decide c15.estado.redis); @Primary solo le aclara
// al analisis estatico cual usar, porque no evalua el interruptor.
@ConditionalOnProperty(name = "c15.estado.redis", havingValue = "true")
@Primary
public class PresenciaSalaRedis implements PresenciaSala {

    static final String PREFIJO = "c15:salas:presencia:";
    static final Duration VIDA = Duration.ofSeconds(90);

    private final StringRedisTemplate redis;
    private final TaskScheduler scheduler;

    public PresenciaSalaRedis(StringRedisTemplate redis, @Qualifier("salasScheduler") TaskScheduler scheduler) {
        this.redis = redis;
        this.scheduler = scheduler;
    }

    static String clave(String salaId, String userId) {
        return PREFIJO + salaId + ":" + userId;
    }

    @Override
    public void entrar(String sessionId, String salaId, String userId) {
        marcar(sessionId, salaId, userId);
    }

    @Override
    public boolean salir(String sessionId, String salaId, String userId) {
        Long quitadas = redis.opsForZSet().remove(clave(salaId, userId), sessionId);
        return quitadas != null && quitadas > 0;
    }

    @Override
    public boolean conectado(String salaId, String userId) {
        Long vigentes = redis.opsForZSet().count(clave(salaId, userId), ahora(), Double.POSITIVE_INFINITY);
        return vigentes != null && vigentes > 0;
    }

    @Override
    public boolean hayAlguien(String salaId) {
        ScanOptions patron = ScanOptions.scanOptions().match(PREFIJO + salaId + ":*").count(100).build();
        try (Cursor<String> claves = redis.scan(patron)) {
            while (claves.hasNext()) {
                Long vigentes = redis.opsForZSet().count(claves.next(), ahora(), Double.POSITIVE_INFINITY);
                if (vigentes != null && vigentes > 0) return true;
            }
        }
        return false;
    }

    private void marcar(String sessionId, String salaId, String userId) {
        String clave = clave(salaId, userId);
        redis.opsForZSet().add(clave, sessionId, ahora() + VIDA.toMillis());
        redis.expire(clave, VIDA);
    }

    private double ahora() {
        return (double) scheduler.getClock().millis();
    }
}
