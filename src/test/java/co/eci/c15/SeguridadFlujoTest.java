package co.eci.c15;

import co.eci.c15.salas.domain.SalaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** La seguridad real, sin el ayudante de tokens de los otros flujos: el jugador sale del token y de nada mas. */
@SpringBootTest
class SeguridadFlujoTest {

    @Autowired
    private WebApplicationContext contexto;

    @Autowired
    private SalaRepository salas;

    /** Sin esto el primer token falso iria a buscar las llaves del proveedor por la red. */
    @MockitoBean
    private JwtDecoder tokens;

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        when(tokens.decode("token-falso")).thenThrow(new BadJwtException("firma invalida"));
        mvc = MockMvcBuilders.webAppContextSetup(contexto).apply(springSecurity()).build();
    }

    private static RequestPostProcessor como(String sub) {
        return jwt().jwt(token -> token.subject(sub));
    }

    private String crearSala(String nombre, String sub) throws Exception {
        String respuesta = mvc.perform(post("/api/salas").with(como(sub)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"" + nombre + "\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return respuesta.replaceAll(".*\"id\":\"([^\"]+)\".*", "$1");
    }

    @Test
    void sinTokenLaApiResponde401() throws Exception {
        mvc.perform(get("/api/salas")).andExpect(status().isUnauthorized());
        // Escribir sin token ni siquiera pasa CSRF
        mvc.perform(post("/api/salas").contentType(MediaType.APPLICATION_JSON).content("{\"nombre\":\"x\"}"))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/salas").header("Authorization", "Bearer token-falso"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void conTokenBearerNoSePideTokenCsrfYElInvalidoDa401() throws Exception {
        // Sin .with(csrf()): lo que vale es el header, como lo manda el front
        when(tokens.decode("token-bueno")).thenReturn(Jwt.withTokenValue("token-bueno").header("alg", "RS256")
                .subject("auth0|csrf").issuedAt(Instant.EPOCH).expiresAt(Instant.parse("2999-01-01T00:00:00Z")).build());

        mvc.perform(post("/api/salas").header("Authorization", "Bearer token-bueno")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"nombre\":\"Con bearer\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.anfitrionId").value("auth0|csrf"));
        mvc.perform(post("/api/salas").header("Authorization", "Bearer token-falso")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"nombre\":\"Falso\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void laVersionNoPideToken() throws Exception {
        mvc.perform(get("/api/version")).andExpect(status().isOk());
    }

    @Test
    void elAnfitrionEsElDelTokenAunqueElCuerpoDigaOtraCosa() throws Exception {
        mvc.perform(post("/api/salas").with(como("auth0|ana")).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Suplantada\",\"anfitrionId\":\"auth0|beto\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.anfitrionId").value("auth0|ana"));
    }

    @Test
    void elJugadorQueSeUneEsElDelTokenAunqueElCuerpoDigaOtraCosa() throws Exception {
        String salaId = crearSala("Union suplantada", "auth0|ana2");

        mvc.perform(post("/api/salas/{id}/jugadores", salaId).with(como("auth0|carla"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"userId\":\"auth0|beto\"}"))
                .andExpect(status().isOk());

        assertEquals(true, salas.findById(salaId).orElseThrow().contieneJugador("auth0|carla"));
        assertEquals(false, salas.findById(salaId).orElseThrow().contieneJugador("auth0|beto"));
    }

    @Test
    void nadiePuedeSacarAOtroDeLaSala() throws Exception {
        String salaId = crearSala("Sacar a otro", "auth0|ana3");

        mvc.perform(delete("/api/salas/{s}/jugadores/{u}", salaId, "auth0|ana3").with(como("auth0|beto")))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/salas/{s}", salaId).with(como("auth0|ana3"))).andExpect(status().isOk());

        // El id del proveedor lleva "|": debe viajar bien en la ruta
        mvc.perform(delete("/api/salas/{s}/jugadores/{u}", salaId, "auth0|ana3").with(como("auth0|ana3")))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/salas/{s}", salaId).with(como("auth0|ana3"))).andExpect(status().isNotFound());
    }

}
