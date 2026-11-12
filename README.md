# c15-salas

Servicio de **salas** del EscapeRoom C15 (ARSW, equipo C15-PortalSuba): salas, equipos, listos, cuenta regresiva
y arranque de la partida. Sale del monolito [ScapeRoomC15](https://github.com/JuanLopezTW/ScapeRoomC15), que se
queda desplegado como respaldo.

## Qué hace

- **API:**
  - `GET/POST /api/salas`
  - `GET /api/salas/{id}` (detalle/lobby; 409 si ya está en partida)
  - `PATCH /api/salas/{id}/configuracion`
  - `POST /api/salas/{id}/jugadores`
  - `DELETE /api/salas/{id}/jugadores/{userId}`
  - `POST|DELETE /api/salas/{id}/listo`
  - `GET /api/salas/{id}/equipos`
  - `POST /api/equipos/{id}/miembros`
- **Reglas:**
  - Salas de 2 equipos de 2.
  - La duración (5, 10 o 15 min) la elige el anfitrión.
  - Cuando todos están listos, hay una cuenta regresiva y la partida arranca sola (`matchId = salaId`).
- **Publica** en Kafka:
  - `c15.partida-iniciada` (equipos + duración), que lo usa gameplay;
  - `c15.sala-cerrada`.
- **Escucha** de Kafka:
  - `c15.partida-terminada`: borra la sala.
  - `c15.partida-iniciada`: marca del barrendero.
  - `c15.sesion`: las conexiones del lobby que avisa el broker. Un invitado que se desconecta sale al instante; el anfitrión tiene 20 s de gracia.
- **Publica** en los topics STOMP `/topic/salas`, `/topic/salas/{id}` y `/topic/salas/{id}/inicio` por RabbitMQ. No tiene WebSocket propio: los jugadores se conectan a `c15-broker`.
- **Seguridad:** valida el JWT de Auth0 en toda la API salvo `/api/version`. El `userId` es el `sub` del token.

`co.eci.c15.plataforma` (seguridad, observabilidad, Kafka, STOMP) es una copia de lo que tienen todos los
servicios. Si se cambia aquí, hay que cambiarlo también en los demás.

## Correr

```bash
mvn verify                  # pruebas + cobertura (minimo 80%); las de Kafka y Redis necesitan Docker
mvn spring-boot:run         # necesita Postgres, Kafka, RabbitMQ (y Redis si ESTADO_REDIS=true): ver .env.example
```

Para levantar todo el sistema junto, usa el compose de `c15-gateway`.
