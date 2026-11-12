package co.eci.c15;

import co.eci.c15.common.events.SesionEvent;
import org.springframework.context.ApplicationEventPublisher;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Duration;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Un invitado que se desconecta del lobby sale de una; el anfitrion tiene gracia y si no vuelve, la sala se cierra. */
@SpringBootTest
@AutoConfigureMockMvc
class SalirDeSalaFlujoTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper json;

    /** El broker avisa las conexiones por Kafka; aqui llegan como si ya se hubieran recibido. */
    @Autowired
    private ApplicationEventPublisher eventos;

    @Test
    void unJugadorQueCierraLaPaginaSaleDeLaSalaYDeSuEquipo() throws Exception {
        String salaId = salaConJugadores("Lobby jugador", "ana", "beto");
        String equipoId = primerEquipo(salaId);
        unirseAEquipo(equipoId, "beto");
        abrir(salaId, "beto", "sesion-beto");

        cerrar(salaId, "beto", "sesion-beto");

        mvc.perform(get("/api/salas/{id}", salaId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jugadores", not(hasItem("beto"))))
                .andExpect(jsonPath("$.jugadores", hasSize(1)));
        mvc.perform(get("/api/salas/{id}/equipos", salaId))
                .andExpect(jsonPath("$[0].miembros", hasSize(0)));
    }

    @Test
    void siElAnfitrionCierraLaPaginaLaSalaDesaparece() throws Exception {
        String salaId = salaConJugadores("Lobby anfitrion", "ana", "beto");
        abrir(salaId, "ana", "sesion-ana");

        cerrar(salaId, "ana", "sesion-ana");

        // Durante la gracia la sala sigue
        mvc.perform(get("/api/salas/{id}", salaId)).andExpect(status().isOk());

        cuandoPaseLaGracia(() -> mvc.perform(get("/api/salas/{id}", salaId)).andExpect(status().isNotFound()));
        mvc.perform(get("/api/salas"))
                .andExpect(jsonPath("$[?(@.id == '" + salaId + "')]", hasSize(0)));
    }

    @Test
    void siElAnfitrionVuelveDentroDeLaGraciaLaSalaSigueYSusJugadoresTambien() throws Exception {
        String salaId = salaConJugadores("Lobby corte", "ana", "beto");
        abrir(salaId, "ana", "sesion-ana-1");

        cerrar(salaId, "ana", "sesion-ana-1");
        abrir(salaId, "ana", "sesion-ana-2");
        Thread.sleep(600); // mas que la gracia de los tests

        mvc.perform(get("/api/salas/{id}", salaId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jugadores", hasItem("ana")))
                .andExpect(jsonPath("$.jugadores", hasItem("beto")));
    }

    @Test
    void elBotonVolverSacaAlJugadorYSiEsElAnfitrionCierraLaSala() throws Exception {
        String salaId = salaConJugadores("Lobby volver", "ana", "beto");

        mvc.perform(delete("/api/salas/{id}/jugadores/{u}", salaId, "beto")).andExpect(status().isNoContent());
        mvc.perform(get("/api/salas/{id}", salaId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jugadores", not(hasItem("beto"))));

        mvc.perform(delete("/api/salas/{id}/jugadores/{u}", salaId, "ana")).andExpect(status().isNoContent());
        mvc.perform(get("/api/salas/{id}", salaId)).andExpect(status().isNotFound());

        // Repetirlo no falla: no hay nada que hacer
        mvc.perform(delete("/api/salas/{id}/jugadores/{u}", salaId, "ana")).andExpect(status().isNoContent());
    }

    private static void cuandoPaseLaGracia(org.awaitility.core.ThrowingRunnable comprobacion) {
        Awaitility.await().atMost(Duration.ofSeconds(5)).untilAsserted(comprobacion);
    }

    private void abrir(String salaId, String userId, String sessionId) {
        eventos.publishEvent(new SesionEvent(SesionEvent.ABIERTA, SesionEvent.SALA, salaId, userId, sessionId));
    }

    private void cerrar(String salaId, String userId, String sessionId) {
        eventos.publishEvent(new SesionEvent(SesionEvent.CERRADA, SesionEvent.SALA, salaId, userId, sessionId));
    }

    /** Sala creada por el primero (anfitrion) con todos dentro. */
    private String salaConJugadores(String nombre, String... usuarios) throws Exception {
        String salaId = leer(mvc.perform(post("/api/salas").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"" + nombre + "\",\"anfitrionId\":\"" + usuarios[0] + "\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString()).get("id").asText();
        for (String u : usuarios) {
            mvc.perform(post("/api/salas/{id}/jugadores", salaId).contentType(MediaType.APPLICATION_JSON)
                            .content("{\"userId\":\"" + u + "\"}"))
                    .andExpect(status().isOk());
        }
        return salaId;
    }

    private String primerEquipo(String salaId) throws Exception {
        return leer(mvc.perform(get("/api/salas/{id}/equipos", salaId))
                .andReturn().getResponse().getContentAsString()).get(0).get("id").asText();
    }

    private void unirseAEquipo(String equipoId, String userId) throws Exception {
        mvc.perform(post("/api/equipos/{id}/miembros", equipoId).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":\"" + userId + "\"}"))
                .andExpect(status().isOk());
    }

    private JsonNode leer(String body) throws Exception {
        return json.readTree(body);
    }
}
