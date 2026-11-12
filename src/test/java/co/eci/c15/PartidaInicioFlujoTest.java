package co.eci.c15;

import co.eci.c15.common.events.PartidaIniciadaEvent;
import co.eci.c15.plataforma.eventos.SeguimientoEventos;
import co.eci.c15.salas.application.CuentaRegresivaPartida;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Arranque automatico de la partida (HU-64.4) contra la app ensamblada (H2). */
@SpringBootTest
@AutoConfigureMockMvc
class PartidaInicioFlujoTest {

    @Autowired
    private SeguimientoEventos entreServicios;

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper json;

    @Autowired
    private CuentaRegresivaPartida cuentaRegresiva;

    @Autowired
    private IniciosDePartida inicios;

    @Test
    void conDosEquiposListosLaPartidaArrancaSolaYAvisaAGameplayConSusEquiposYDuracion() throws Exception {
        String salaId = salaConDosEquipos("Sala inicio");
        for (String u : new String[]{"ana", "beto", "caro", "dani"}) marcarListo(salaId, u);
        await().atMost(Duration.ofSeconds(5)).until(() -> cuentaRegresiva.enCurso(salaId));

        await().atMost(Duration.ofSeconds(10)).untilAsserted(() ->
                mvc.perform(get("/api/salas/{id}", salaId)).andExpect(status().isConflict()));
        await().atMost(Duration.ofSeconds(10)).until(entreServicios::entregados);

        // Lo que necesita gameplay para arrancar el cronometro y repartir roles: los 2 equipos y los 10 min de la sala
        PartidaIniciadaEvent inicio = inicios.de(salaId).orElseThrow();
        assertEquals(Duration.ofMinutes(10), inicio.duracion());
        assertEquals(2, inicio.equipos().size());
        assertTrue(inicio.equipos().values().stream().allMatch(e -> e.size() == 2), inicio.equipos().toString());
        mvc.perform(post("/api/salas/{id}/listo", salaId).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":\"ana\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void siAlguienSeDesmarcaDuranteLaCuentaRegresivaNoArranca() throws Exception {
        String salaId = salaConDosEquipos("Sala cancelada");
        for (String u : new String[]{"ana", "beto", "caro", "dani"}) marcarListo(salaId, u);
        await().atMost(Duration.ofSeconds(5)).until(() -> cuentaRegresiva.enCurso(salaId));

        mvc.perform(delete("/api/salas/{id}/listo", salaId).param("userId", "dani"))
                .andExpect(status().isOk());
        await().atMost(Duration.ofSeconds(5)).until(() -> !cuentaRegresiva.enCurso(salaId));

        Thread.sleep(1500);
        mvc.perform(get("/api/salas/{id}", salaId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("DISPONIBLE"));
        assertTrue(inicios.de(salaId).isEmpty(), "no se aviso a gameplay");
    }

    @Test
    void unJugadorSinEquipoImpideElArranque() throws Exception {
        String salaId = salaConEquipos("Sala con suelto", new String[][]{{"ana", "beto"}, {"caro"}});
        unirseASala(salaId, "dani");
        for (String u : new String[]{"ana", "beto", "caro"}) marcarListo(salaId, u);

        // La evaluacion es asincrona: comprobar que no arranca durante un rato
        await().during(Duration.ofMillis(800)).atMost(Duration.ofSeconds(3))
                .until(() -> !cuentaRegresiva.enCurso(salaId));
    }

    @Test
    void siTodosMarcanListoALaVezLaPartidaArrancaIgual() throws Exception {
        // Antes el arranque se evaluaba antes del commit: con "listos" simultaneos nadie veia a
        // todos listos y la partida no arrancaba nunca (lo encontro la prueba de carga).
        List<String> salas = new ArrayList<>();
        for (int i = 0; i < 5; i++) salas.add(salaConDosEquipos("Sala simultanea " + i));

        ExecutorService pool = Executors.newFixedThreadPool(4);
        try {
            for (String salaId : salas) {
                CountDownLatch salida = new CountDownLatch(1);
                List<Future<?>> listos = new ArrayList<>();
                for (String u : new String[]{"ana", "beto", "caro", "dani"}) {
                    listos.add(pool.submit(() -> {
                        salida.await();
                        marcarListo(salaId, u);
                        return null;
                    }));
                }
                salida.countDown();
                for (Future<?> f : listos) f.get();
            }
        } finally {
            pool.shutdown();
        }

        for (String salaId : salas) {
            await().atMost(Duration.ofSeconds(10)).untilAsserted(() ->
                    mvc.perform(get("/api/salas/{id}", salaId)).andExpect(status().isConflict()));
        }
    }

    /** Sala de 10 minutos con dos equipos: ana y beto en el 1, caro y dani en el 2. */
    private String salaConDosEquipos(String nombre) throws Exception {
        return salaConEquipos(nombre, new String[][]{{"ana", "beto"}, {"caro", "dani"}});
    }

    private String salaConEquipos(String nombre, String[][] reparto) throws Exception {
        String salaId = leer(mvc.perform(post("/api/salas").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"" + nombre + "\",\"minutos\":10}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString()).get("id").asText();
        JsonNode equipos = leer(mvc.perform(get("/api/salas/{id}/equipos", salaId))
                .andReturn().getResponse().getContentAsString());
        for (int i = 0; i < reparto.length; i++) {
            for (String u : reparto[i]) {
                unirseASala(salaId, u);
                mvc.perform(post("/api/equipos/{id}/miembros", equipos.get(i).get("id").asText())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"userId\":\"" + u + "\"}"))
                        .andExpect(status().isOk());
            }
        }
        return salaId;
    }

    private void unirseASala(String salaId, String userId) throws Exception {
        mvc.perform(post("/api/salas/{id}/jugadores", salaId).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":\"" + userId + "\"}"))
                .andExpect(status().isOk());
    }

    private void marcarListo(String salaId, String userId) throws Exception {
        mvc.perform(post("/api/salas/{id}/listo", salaId).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":\"" + userId + "\"}"))
                .andExpect(status().isOk());
    }

    private JsonNode leer(String body) throws Exception {
        return json.readTree(body);
    }
}
