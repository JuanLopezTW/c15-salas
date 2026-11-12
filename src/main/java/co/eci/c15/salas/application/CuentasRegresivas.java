package co.eci.c15.salas.application;

import java.time.Duration;

/**
 * Cuentas regresivas en curso, vistas por todas las instancias: solo una lleva la cuenta de cada sala y
 * cualquiera la puede cancelar. Cada cuenta tiene un {@code token} de quien la empezo.
 */
public interface CuentasRegresivas {

    /** @return true si la empezo esta llamada; false si la sala ya tenia una */
    boolean empezar(String salaId, String token, Duration vida);

    boolean enCurso(String salaId);

    /** Cancela la cuenta de la sala, sea de quien sea. @return true si habia una */
    boolean cancelar(String salaId);

    /** Termina la cuenta solo si sigue siendo la de ese token. @return true si era la suya */
    boolean terminar(String salaId, String token);
}
