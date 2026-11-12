package co.eci.c15.salas.infrastructure.persistence;

import co.eci.c15.salas.domain.Sala;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@Import(SalaRepositoryAdapter.class)
class SalaRepositoryAdapterTest {

    @Autowired
    private SalaRepositoryAdapter adapter;

    @Autowired
    private TestEntityManager em;

    @Test
    void conservaIdConfiguracionDuracionYJugadores() {
        Sala sala = Sala.crear("Sala 1", "anfitrion-1", 15);
        sala.unirJugador("user-1");
        sala.unirJugador("user-2");
        adapter.save(sala);
        em.flush();
        em.clear();

        Sala leida = adapter.findById(sala.getId()).orElseThrow();
        assertEquals(sala.getId(), leida.getId());
        assertEquals("Sala 1", leida.getNombre());
        assertEquals("anfitrion-1", leida.getAnfitrionId());
        assertEquals(2, leida.getNumEquipos());
        assertEquals(2, leida.getJugadoresPorEquipo());
        assertEquals(15, leida.getDuracionMinutos());
        assertEquals(Set.of("user-1", "user-2"), leida.getJugadores());
    }

    @Test
    void unaSalaGuardadaAntesDeLaDuracionUsaLaDuracionPorDefecto() {
        Sala sala = Sala.crear("Vieja", "anfitrion-1", 15);
        adapter.save(sala);
        em.flush();
        em.getEntityManager().createNativeQuery("update salas set duracion_minutos = null where id = ?1")
                .setParameter(1, sala.getId()).executeUpdate();
        em.clear();

        assertEquals(Sala.DURACION_DEFECTO_MINUTOS, adapter.findById(sala.getId()).orElseThrow().getDuracionMinutos());
    }

    @Test
    void guardarDosVecesActualizaLaMismaFila() {
        Sala sala = Sala.crear("Sala 1", "anfitrion-1");
        adapter.save(sala);

        Sala leida = adapter.findById(sala.getId()).orElseThrow();
        leida.unirJugador("user-1");
        adapter.save(leida);

        assertEquals(1, adapter.findAll().size());
        assertEquals(Set.of("user-1"), adapter.findById(sala.getId()).orElseThrow().getJugadores());
    }

    @Test
    void conservaEstadoEnPartida() {
        Sala sala = Sala.crear("Sala 1", "anfitrion-1");
        sala.iniciarPartida();
        adapter.save(sala);

        assertEquals(Sala.Estado.EN_PARTIDA, adapter.findById(sala.getId()).orElseThrow().getEstado());
    }

    @Test
    void deleteBorraLaSalaYSusJugadores() {
        Sala sala = Sala.crear("Sala 1", "anfitrion-1");
        sala.unirJugador("user-1");
        Sala otra = Sala.crear("Sala 2", "anfitrion-2");
        adapter.save(sala);
        adapter.save(otra);
        em.flush();

        adapter.delete(sala.getId());
        em.flush();
        em.clear();

        assertTrue(adapter.findById(sala.getId()).isEmpty());
        assertEquals(1, adapter.findAll().size());
    }
}
