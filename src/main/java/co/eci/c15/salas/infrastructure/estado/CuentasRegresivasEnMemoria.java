package co.eci.c15.salas.infrastructure.estado;

import co.eci.c15.salas.application.CuentasRegresivas;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@ConditionalOnProperty(name = "c15.estado.redis", havingValue = "false", matchIfMissing = true)
public class CuentasRegresivasEnMemoria implements CuentasRegresivas {

    private final Map<String, String> cuentas = new ConcurrentHashMap<>();

    @Override
    public boolean empezar(String salaId, String token, Duration vida) {
        return cuentas.putIfAbsent(salaId, token) == null;
    }

    @Override
    public boolean enCurso(String salaId) {
        return cuentas.containsKey(salaId);
    }

    @Override
    public boolean cancelar(String salaId) {
        return cuentas.remove(salaId) != null;
    }

    @Override
    public boolean terminar(String salaId, String token) {
        return cuentas.remove(salaId, token);
    }
}
