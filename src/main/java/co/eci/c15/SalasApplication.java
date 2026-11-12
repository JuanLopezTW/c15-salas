package co.eci.c15;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Servicio de salas del EscapeRoom C15: salas, equipos, listos, cuenta regresiva y arranque. Sale del monolito
 * (repo ScapeRoomC15) con los mismos paquetes; lo comun a todos los servicios esta en {@code plataforma}.
 */
@SpringBootApplication
public class SalasApplication {

    public static void main(String[] args) {
        SpringApplication.run(SalasApplication.class, args);
    }
}
