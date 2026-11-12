package co.eci.c15;

import co.eci.c15.salas.domain.Sala;
import co.eci.c15.salas.domain.SalaRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Flujo completo de salas y equipos contra la app ensamblada (H2). */
@SpringBootTest
@AutoConfigureMockMvc
class SalasEquiposFlujoTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper json;

    @Autowired
    private SalaRepository salas;

    @Test
    void crearSalaConfigurarYUnirseAEquipo() throws Exception {
        String salaId = leer(mvc.perform(post("/api/salas").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Sala flujo\",\"minutos\":10}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.minutos").value(10))
                .andExpect(jsonPath("$.cupoTotal").value(4))
                .andReturn().getResponse().getContentAsString()).get("id").asText();

        mvc.perform(get("/api/salas/{id}/equipos", salaId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));

        // Por estabilidad las salas son de 2 equipos de 2: fuera de eso es 400
        mvc.perform(patch("/api/salas/{id}/configuracion", salaId).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"numEquipos\":3,\"jugadoresPorEquipo\":2}"))
                .andExpect(status().isBadRequest());
        mvc.perform(patch("/api/salas/{id}/configuracion", salaId).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"numEquipos\":2,\"jugadoresPorEquipo\":2}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(salaId))
                .andExpect(jsonPath("$.numEquipos").value(2));

        JsonNode equipos = leer(mvc.perform(get("/api/salas/{id}/equipos", salaId))
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].cupoMaximo").value(2))
                .andReturn().getResponse().getContentAsString());
        String equipo1 = equipos.get(0).get("id").asText();

        mvc.perform(post("/api/equipos/{id}/miembros", equipo1).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":\"juan\"}"))
                .andExpect(status().isForbidden());

        mvc.perform(post("/api/salas/{id}/jugadores", salaId).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":\"juan\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jugadoresActuales").value(1));

        mvc.perform(post("/api/equipos/{id}/miembros", equipo1).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":\"juan\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.miembros[0]").value("juan"));
    }

    @Test
    void unaDuracionQueNoSeOfreceDevuelve400() throws Exception {
        mvc.perform(post("/api/salas").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Sala larga\",\"minutos\":30}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/salas").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Sala rara\",\"minutos\":\"diez\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void configuracionSinCamposDevuelve400() throws Exception {
        String salaId = leer(mvc.perform(post("/api/salas").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Sala 400\",\"anfitrionId\":\"host\"}"))
                .andReturn().getResponse().getContentAsString()).get("id").asText();

        mvc.perform(patch("/api/salas/{id}/configuracion", salaId).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"solicitanteId\":\"host\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void equiposDeSalaInexistenteDevuelve404() throws Exception {
        mvc.perform(get("/api/salas/{id}/equipos", "no-existe"))
                .andExpect(status().isNotFound());
    }

    @Test
    void detalleDeLaSalaMuestraEquiposYJugadoresSinEquipo() throws Exception {
        String salaId = crearSala("Sala lobby");
        unirseASala(salaId, "host");
        unirseASala(salaId, "ana");
        String equipo1 = leer(mvc.perform(get("/api/salas/{id}/equipos", salaId))
                .andReturn().getResponse().getContentAsString()).get(0).get("id").asText();
        mvc.perform(post("/api/equipos/{id}/miembros", equipo1).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":\"host\"}"))
                .andExpect(status().isOk());

        mvc.perform(get("/api/salas/{id}", salaId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(salaId))
                .andExpect(jsonPath("$.nombre").value("Sala lobby"))
                .andExpect(jsonPath("$.estado").value("DISPONIBLE"))
                .andExpect(jsonPath("$.jugadores", hasSize(2)))
                .andExpect(jsonPath("$.sinEquipo[0]").value("ana"))
                .andExpect(jsonPath("$.equipos", hasSize(2)))
                .andExpect(jsonPath("$.equipos[0].miembros[0]").value("host"))
                .andExpect(jsonPath("$.equipos[1].miembros", hasSize(0)));
    }

    @Test
    void detalleDeSalaInexistenteDevuelve404() throws Exception {
        mvc.perform(get("/api/salas/{id}", "no-existe"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Sala no encontrada: no-existe"));
    }

    @Test
    void detalleDeSalaConPartidaIniciadaDevuelve409() throws Exception {
        String salaId = crearSala("Sala cerrada");
        Sala sala = salas.findById(salaId).orElseThrow();
        sala.iniciarPartida();
        salas.save(sala);

        mvc.perform(get("/api/salas/{id}", salaId))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void jugadoresMarcanListoYElEquipoQuedaListo() throws Exception {
        String salaId = crearSala("Sala listo");
        String equipo1 = leer(mvc.perform(get("/api/salas/{id}/equipos", salaId))
                .andReturn().getResponse().getContentAsString()).get(0).get("id").asText();
        for (String u : new String[]{"ana", "beto", "caro"}) unirseASala(salaId, u);
        for (String u : new String[]{"ana", "beto"}) {
            mvc.perform(post("/api/equipos/{id}/miembros", equipo1).contentType(MediaType.APPLICATION_JSON)
                            .content("{\"userId\":\"" + u + "\"}"))
                    .andExpect(status().isOk());
        }

        mvc.perform(post("/api/salas/{id}/listo", salaId).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":\"ana\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.equipos[0].listos[0]").value("ana"))
                .andExpect(jsonPath("$.equipos[0].listo").value(false));

        mvc.perform(post("/api/salas/{id}/listo", salaId).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":\"beto\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.equipos[0].listo").value(true));

        mvc.perform(get("/api/salas/{id}", salaId))
                .andExpect(jsonPath("$.equipos[0].listos", hasSize(2)))
                .andExpect(jsonPath("$.equipos[0].listo").value(true));

        mvc.perform(delete("/api/salas/{id}/listo", salaId).param("userId", "beto"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.equipos[0].listo").value(false));
    }

    @Test
    void listoSinEquipoDevuelve409YSalaInexistente404() throws Exception {
        String salaId = crearSala("Sala sin equipo");
        unirseASala(salaId, "ana");

        mvc.perform(post("/api/salas/{id}/listo", salaId).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":\"ana\"}"))
                .andExpect(status().isConflict());

        mvc.perform(post("/api/salas/{id}/listo", "no-existe").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":\"ana\"}"))
                .andExpect(status().isNotFound());
    }

    private String crearSala(String nombre) throws Exception {
        return leer(mvc.perform(post("/api/salas").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"" + nombre + "\",\"anfitrionId\":\"host\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString()).get("id").asText();
    }

    private void unirseASala(String salaId, String userId) throws Exception {
        mvc.perform(post("/api/salas/{id}/jugadores", salaId).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":\"" + userId + "\"}"))
                .andExpect(status().isOk());
    }

    private JsonNode leer(String body) throws Exception {
        return json.readTree(body);
    }
}
