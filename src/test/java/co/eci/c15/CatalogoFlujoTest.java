package co.eci.c15;

import co.eci.c15.salas.application.SalaDto;
import co.eci.c15.salas.domain.Sala;
import co.eci.c15.salas.domain.SalaRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.http.MediaType;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.hasItem;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Catalogo de salas en tiempo real (HU-14.3) contra la app ensamblada (H2). */
@SpringBootTest
@AutoConfigureMockMvc
class CatalogoFlujoTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper json;

    @Autowired
    private SalaRepository salas;

    @MockitoSpyBean
    private SimpMessagingTemplate messaging;

    @Test
    void elCatalogoMuestraLasSalasConJugadoresYEstadoOrdenadasPorNombre() throws Exception {
        String zeta = crearSala("zz Catalogo");
        String alfa = crearSala("aa Catalogo");
        mvc.perform(post("/api/salas/{id}/jugadores", alfa).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":\"ana\"}"))
                .andExpect(status().isOk());
        Sala enPartida = salas.findById(zeta).orElseThrow();
        enPartida.iniciarPartida();
        salas.save(enPartida);

        String body = mvc.perform(get("/api/salas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id", hasItem(alfa)))
                .andReturn().getResponse().getContentAsString();
        List<SalaDto> catalogo = List.of(json.readValue(body, SalaDto[].class));

        SalaDto a = buscar(catalogo, alfa);
        assertEquals(1, a.jugadoresActuales());
        assertEquals(4, a.cupoTotal());
        assertEquals("DISPONIBLE", a.estado());
        assertEquals("EN_PARTIDA", buscar(catalogo, zeta).estado());
        assertTrue(catalogo.indexOf(a) < catalogo.indexOf(buscar(catalogo, zeta)));
    }

    @Test
    @SuppressWarnings("unchecked")
    void crearYUnirsePublicanElCatalogoActualizadoEnTiempoReal() throws Exception {
        String salaId = crearSala("Sala en vivo");
        ArgumentCaptor<Object> enviado = ArgumentCaptor.forClass(Object.class);
        verify(messaging, atLeastOnce()).convertAndSend(eq("/topic/salas"), enviado.capture());
        assertEquals(0, buscar((List<SalaDto>) enviado.getValue(), salaId).jugadoresActuales());

        clearInvocations(messaging);
        mvc.perform(post("/api/salas/{id}/jugadores", salaId).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":\"beto\"}"))
                .andExpect(status().isOk());

        verify(messaging, atLeastOnce()).convertAndSend(eq("/topic/salas"), enviado.capture());
        assertEquals(1, buscar((List<SalaDto>) enviado.getValue(), salaId).jugadoresActuales());
    }

    private static SalaDto buscar(List<SalaDto> catalogo, String salaId) {
        return catalogo.stream().filter(s -> s.id().equals(salaId)).findFirst()
                .orElseThrow(() -> new AssertionError("La sala " + salaId + " no esta en el catalogo"));
    }

    private String crearSala(String nombre) throws Exception {
        return json.readTree(mvc.perform(post("/api/salas").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"" + nombre + "\",\"anfitrionId\":\"host\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString()).get("id").asText();
    }
}
