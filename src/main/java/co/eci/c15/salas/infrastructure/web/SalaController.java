package co.eci.c15.salas.infrastructure.web;

import co.eci.c15.salas.application.ConfigurarSalaUseCase;
import co.eci.c15.salas.application.CrearSalaUseCase;
import co.eci.c15.salas.application.ListarEquiposUseCase;
import co.eci.c15.salas.application.ListarSalasUseCase;
import co.eci.c15.salas.application.MarcarListoUseCase;
import co.eci.c15.salas.application.SalaDetalleDto;
import co.eci.c15.salas.application.SalaDto;
import co.eci.c15.salas.application.SalirDeSalaUseCase;
import co.eci.c15.salas.application.UnirseSalaUseCase;
import co.eci.c15.salas.application.VerSalaUseCase;
import co.eci.c15.salas.domain.ConfiguracionInvalidaException;
import co.eci.c15.salas.domain.JugadorSinEquipoException;
import co.eci.c15.salas.domain.NoEsAnfitrionException;
import co.eci.c15.salas.domain.PartidaYaIniciadaException;
import co.eci.c15.salas.domain.SalaLlenaException;
import co.eci.c15.salas.domain.SalaNoEncontradaException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

@RestController
@RequestMapping("/api/salas")
public class SalaController {

    private final CrearSalaUseCase crear;
    private final ListarSalasUseCase listar;
    private final ConfigurarSalaUseCase configurar;
    private final UnirseSalaUseCase unirse;
    private final ListarEquiposUseCase listarEquipos;
    private final VerSalaUseCase verSala;
    private final MarcarListoUseCase marcarListo;
    private final SalirDeSalaUseCase salir;

    public SalaController(CrearSalaUseCase crear, ListarSalasUseCase listar, ConfigurarSalaUseCase configurar,
                          UnirseSalaUseCase unirse, ListarEquiposUseCase listarEquipos,
                          VerSalaUseCase verSala, MarcarListoUseCase marcarListo, SalirDeSalaUseCase salir) {
        this.crear = crear;
        this.listar = listar;
        this.configurar = configurar;
        this.unirse = unirse;
        this.listarEquipos = listarEquipos;
        this.verSala = verSala;
        this.marcarListo = marcarListo;
        this.salir = salir;
    }

    @GetMapping
    public List<SalaDto> catalogo() {
        return listar.ejecutar();
    }

    @GetMapping("/{salaId}")
    public ResponseEntity<?> detalle(@PathVariable("salaId") String salaId) {
        try {
            return ResponseEntity.ok(verSala.ejecutar(salaId));
        } catch (SalaNoEncontradaException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        } catch (PartidaYaIniciadaException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping
    public ResponseEntity<?> crear(@RequestBody Map<String, Object> body, Principal yo) {
        try {
            Object nombre = body.get("nombre");
            Integer minutos = body.get("minutos") == null ? null : entero(body, "minutos");
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(crear.ejecutar(nombre == null ? null : nombre.toString(), yo.getName(), minutos));
        } catch (ConfiguracionInvalidaException | IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PatchMapping("/{salaId}/configuracion")
    public ResponseEntity<?> configurar(@PathVariable("salaId") String salaId, @RequestBody Map<String, Object> body,
                                        Principal yo) {
        try {
            return ResponseEntity.ok(configurar.ejecutar(salaId, yo.getName(),
                    entero(body, "numEquipos"), entero(body, "jugadoresPorEquipo")));
        } catch (NoEsAnfitrionException | PartidaYaIniciadaException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", e.getMessage()));
        } catch (ConfiguracionInvalidaException | IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{salaId}/jugadores")
    public ResponseEntity<?> unirse(@PathVariable("salaId") String salaId, Principal yo) {
        try {
            return ResponseEntity.ok(unirse.ejecutar(salaId, yo.getName()));
        } catch (SalaLlenaException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", e.getMessage()));
        } catch (PartidaYaIniciadaException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/{salaId}/equipos")
    public ResponseEntity<?> equipos(@PathVariable("salaId") String salaId) {
        try {
            return ResponseEntity.ok(listarEquipos.ejecutar(salaId));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{salaId}/listo")
    public ResponseEntity<?> marcarListo(@PathVariable("salaId") String salaId, Principal yo) {
        return listo(() -> marcarListo.marcar(salaId, yo.getName()));
    }

    @DeleteMapping("/{salaId}/listo")
    public ResponseEntity<?> desmarcarListo(@PathVariable("salaId") String salaId, Principal yo) {
        return listo(() -> marcarListo.desmarcar(salaId, yo.getName()));
    }

    /**
     * Salir del lobby (boton VOLVER). Si es el anfitrion la sala se cierra. Siempre 204: si la sala ya
     * no existe, ya empezo la partida o el jugador no estaba, no hay nada que hacer.
     */
    @DeleteMapping("/{salaId}/jugadores/{userId}")
    public ResponseEntity<Void> salir(@PathVariable("salaId") String salaId, @PathVariable("userId") String userId,
                                      Principal yo) {
        if (!userId.equals(yo.getName())) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        salir.ejecutar(salaId, userId);
        return ResponseEntity.noContent().build();
    }

    private static ResponseEntity<?> listo(Supplier<SalaDetalleDto> accion) {
        try {
            return ResponseEntity.ok(accion.get());
        } catch (SalaNoEncontradaException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        } catch (PartidaYaIniciadaException | JugadorSinEquipoException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    private static int entero(Map<String, Object> body, String campo) {
        if (body.get(campo) instanceof Number n) return n.intValue();
        throw new IllegalArgumentException("El campo " + campo + " es obligatorio y debe ser numérico");
    }
}