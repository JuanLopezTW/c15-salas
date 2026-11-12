package co.eci.c15;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Duration;

import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Lo que llega de los otros servicios por Kafka con el JSON del contrato: las sesiones del broker y el fin de
 * partida de gameplay (no como clases Java, sino como las publicarian ellos).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
class EventosKafkaFlujoTest {

    @DynamicPropertySource
    static void kafka(DynamicPropertyRegistry propiedades) {
        KafkaDePrueba.propiedades(propiedades, "eventosKafka");
    }

    @Autowired
    private KafkaTemplate<String, String> kafka;

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper json;

    @Test
    void elInvitadoQueCierraElLobbyEnElBrokerSaleDeLaSala() throws Exception {
        String salaId = salaConJugadores("Kafka lobby", "k-ana", "k-beto");

        kafka.send("c15.eventosKafka.sesion", salaId, sesion("ABIERTA", salaId, "k-beto"));
        kafka.send("c15.eventosKafka.sesion", salaId, sesion("CERRADA", salaId, "k-beto"));

        await().atMost(Duration.ofSeconds(20)).untilAsserted(() -> mvc.perform(get("/api/salas/{id}", salaId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jugadores", not(hasItem("k-beto")))));
    }

    @Test
    void alTerminarLaPartidaEnGameplayLaSalaSeBorra() throws Exception {
        String salaId = salaConJugadores("Kafka fin", "k-caro");

        kafka.send("c15.eventosKafka.partida-terminada", salaId, "{\"matchId\":\"" + salaId + "\",\"reason\":\"TIME_UP\","
                + "\"winnerTeamId\":null,\"scores\":{},\"equipos\":{},\"campoNuevo\":true}");

        await().atMost(Duration.ofSeconds(20)).untilAsserted(() ->
                mvc.perform(get("/api/salas/{id}", salaId)).andExpect(status().isNotFound()));
    }

    private static String sesion(String estado, String salaId, String userId) {
        return "{\"estado\":\"" + estado + "\",\"ambito\":\"SALA\",\"id\":\"" + salaId + "\",\"userId\":\"" + userId
                + "\",\"sessionId\":\"ws-" + userId + "\"}";
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

    private JsonNode leer(String cuerpo) throws Exception {
        return json.readTree(cuerpo);
    }
}
