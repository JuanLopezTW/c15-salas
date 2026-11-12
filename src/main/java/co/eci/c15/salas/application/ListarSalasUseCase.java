package co.eci.c15.salas.application;

import co.eci.c15.salas.domain.Sala;
import co.eci.c15.salas.domain.SalaRepository;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

/**
 * Catalogo de salas (HU-14.3): todas las salas, incluidas las que ya estan en partida (el
 * front las muestra con su estado), ordenadas por nombre sin importar mayusculas. Las salas
 * cuya partida termino ya no existen ({@link FinalizarPartidaUseCase}).
 */
@Service
public class ListarSalasUseCase {

    private static final Comparator<Sala> POR_NOMBRE = Comparator
            .comparing(Sala::getNombre, String.CASE_INSENSITIVE_ORDER)
            .thenComparing(Sala::getId);

    private final SalaRepository salas;

    public ListarSalasUseCase(SalaRepository salas) {
        this.salas = salas;
    }

    public List<SalaDto> ejecutar() {
        return salas.findAll().stream().sorted(POR_NOMBRE).map(SalaDto::from).toList();
    }
}
