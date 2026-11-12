package co.eci.c15.salas.infrastructure.estado;

import co.eci.c15.common.events.PartidaIniciadaEvent;
import co.eci.c15.common.logging.EventoNegocio;
import co.eci.c15.salas.application.FinalizarPartidaUseCase;
import co.eci.c15.salas.domain.Sala;
import co.eci.c15.salas.domain.SalaRepository;
import co.eci.c15.salas.infrastructure.events.PresenciaSala;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.UUID;

/**
 * Con varias instancias no se pueden borrar las salas al arrancar (serian las de las demas). En su lugar,
 * cada {@code c15.salas.barrido} una sola instancia (la que toma el turno en Redis) cierra las que quedaron
 * huerfanas, por ejemplo porque la instancia de sus jugadores murio sin avisar:
 * <ul>
 *   <li>en espera y sin nadie con el lobby abierto en dos revisiones seguidas;</li>
 *   <li>en partida sin partida viva (la marca se pone al iniciar y caduca con la partida).</li>
 * </ul>
 */
@Component
@ConditionalOnProperty(name = "c15.estado.redis", havingValue = "true")
public class BarrenderoSalas {

    private static final Logger log = LoggerFactory.getLogger(BarrenderoSalas.class);
    static final String TURNO = "c15:salas:barrendero";

    private final SalaRepository salas;
    private final FinalizarPartidaUseCase finalizar;
    private final PresenciaSala presencia;
    private final StringRedisTemplate redis;
    private final TaskScheduler scheduler;
    private final Duration intervalo;
    private final Duration margen;
    private final String instancia = UUID.randomUUID().toString();

    @SuppressWarnings("java:S107") // salas, presencia, Redis, reloj y los tiempos del barrido
    public BarrenderoSalas(SalaRepository salas, FinalizarPartidaUseCase finalizar, PresenciaSala presencia,
                           StringRedisTemplate redis, @Qualifier("salasScheduler") TaskScheduler scheduler,
                           @Value("${c15.salas.barrido:60s}") Duration intervalo,
                           @Value("${c15.estado.margen:10m}") Duration margen) {
        this.salas = salas;
        this.finalizar = finalizar;
        this.presencia = presencia;
        this.redis = redis;
        this.scheduler = scheduler;
        this.intervalo = intervalo;
        this.margen = margen;
    }

    static String enPartida(String salaId) {
        return "c15:salas:" + salaId + ":en-partida";
    }

    static String sinNadie(String salaId) {
        return "c15:salas:" + salaId + ":sin-nadie";
    }

    @EventListener(ApplicationReadyEvent.class)
    public void programar() {
        scheduler.scheduleAtFixedRate(this::barrer, intervalo);
    }

    /** La sala tiene partida viva mientras dure la partida (mas el margen). */
    @EventListener
    public void onPartidaIniciada(PartidaIniciadaEvent evento) {
        redis.opsForValue().set(enPartida(evento.matchId()), instancia, evento.duracion().plus(margen));
    }

    void barrer() {
        try {
            // Una sola instancia por vuelta; el turno vence antes de la siguiente
            if (!Boolean.TRUE.equals(redis.opsForValue().setIfAbsent(TURNO, instancia, intervalo.dividedBy(2)))) return;
            for (Sala sala : salas.findAll()) {
                if (sala.isDisponible()) revisarEspera(sala);
                else revisarPartida(sala);
            }
        } catch (RuntimeException e) {
            // Si se escapa, el scheduler no vuelve a llamar el barrido nunca
            log.error("No se pudieron revisar las salas huerfanas", e);
        }
    }

    private void revisarEspera(Sala sala) {
        String marca = sinNadie(sala.getId());
        if (presencia.hayAlguien(sala.getId())) {
            redis.delete(marca);
            return;
        }
        // La primera vez solo se anota: puede ser una sala recien creada cuyo lobby aun no se conecta
        boolean primeraVez = Boolean.TRUE.equals(redis.opsForValue().setIfAbsent(marca, instancia, intervalo.multipliedBy(3)));
        if (!primeraVez) cerrar(sala, "sin_jugadores");
    }

    private void revisarPartida(Sala sala) {
        if (!Boolean.TRUE.equals(redis.hasKey(enPartida(sala.getId())))) cerrar(sala, "partida_perdida");
    }

    private void cerrar(Sala sala, String motivo) {
        if (finalizar.ejecutar(sala.getId())) {
            EventoNegocio.de(log, "sala.cerrada").con("salaId", sala.getId()).con("motivo", motivo).info();
        }
        redis.delete(sinNadie(sala.getId()));
    }
}
