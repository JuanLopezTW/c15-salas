package co.eci.c15.salas.infrastructure.persistence;

import co.eci.c15.salas.domain.Equipo;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@Import(EquipoRepositoryAdapter.class)
class EquipoRepositoryAdapterTest {

    @Autowired
    private EquipoRepositoryAdapter adapter;

    @Autowired
    private TestEntityManager em;

    @Test
    void conservaDatosYMiembros() {
        Equipo equipo = Equipo.crear("sala-1", 1, 3);
        equipo.unirMiembro("user-1");
        adapter.save(equipo);
        em.flush();
        em.clear();

        Equipo leido = adapter.findById(equipo.getId()).orElseThrow();
        assertEquals("sala-1", leido.getSalaId());
        assertEquals(1, leido.getNumero());
        assertEquals(3, leido.getCupoMaximo());
        assertEquals(Set.of("user-1"), leido.getMiembros());
    }

    @Test
    void conservaLosMiembrosListos() {
        Equipo equipo = Equipo.crear("sala-1", 1, 3);
        equipo.unirMiembro("user-1");
        equipo.unirMiembro("user-2");
        equipo.marcarListo("user-1");
        adapter.save(equipo);
        em.flush();
        em.clear();

        Equipo leido = adapter.findById(equipo.getId()).orElseThrow();
        assertEquals(Set.of("user-1"), leido.getListos());

        leido.quitarMiembro("user-1");
        adapter.save(leido);
        em.flush();
        em.clear();
        assertTrue(adapter.findById(equipo.getId()).orElseThrow().getListos().isEmpty());
    }

    @Test
    void listaLosEquiposDeLaSalaOrdenadosPorNumero() {
        adapter.save(Equipo.crear("sala-1", 2, 4));
        adapter.save(Equipo.crear("sala-1", 1, 4));
        adapter.save(Equipo.crear("sala-2", 1, 4));

        List<Equipo> equipos = adapter.findBySalaId("sala-1");
        assertEquals(List.of(1, 2), equipos.stream().map(Equipo::getNumero).toList());
    }

    @Test
    void actualizaMiembrosYElimina() {
        Equipo equipo = Equipo.crear("sala-1", 1, 3);
        equipo.unirMiembro("user-1");
        adapter.save(equipo);

        Equipo leido = adapter.findById(equipo.getId()).orElseThrow();
        leido.quitarMiembro("user-1");
        leido.unirMiembro("user-2");
        adapter.save(leido);
        assertEquals(Set.of("user-2"), adapter.findById(equipo.getId()).orElseThrow().getMiembros());

        adapter.delete(equipo.getId());
        assertTrue(adapter.findById(equipo.getId()).isEmpty());
    }
}