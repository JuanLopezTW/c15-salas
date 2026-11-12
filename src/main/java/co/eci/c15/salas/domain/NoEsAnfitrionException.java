package co.eci.c15.salas.domain;

public class NoEsAnfitrionException extends RuntimeException {
    public NoEsAnfitrionException(String userId) {
        super("El usuario '" + userId + "' no es el anfitrión de esta sala");
    }
}
