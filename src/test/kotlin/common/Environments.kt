package common

object Environments {
    val MEDIATOR_PEER_DID = System.getenv("MEDIATOR_PEER_DID") ?: "did:peer:2.Ez6LSghwSE437wnDE1pt3X6hVDUQzSjsHzinpX3XFvMjRAm7y.Vz6Mkhh1e5CEYYq6JBUcTZ6Cp2ranCWRrv7Yax3Le4N59R6dd.SeyJ0IjoiZG0iLCJzIjoiaHR0cHM6Ly9tZWRpYXRvci10ZXN0LWVudi5hdGFsYXByaXNtLmlvL21lZGlhdG9yIiwiciI6W10sImEiOlsiZGlkY29tbS92MiJdfQ"
    val MEDIATOR_URL = System.getenv("MEDIATOR_URL") ?: "http://localhost:8080"
    val RECIPIENT_LISTENER_HOST = System.getenv("RECIPIENT_LISTENER_URL") ?: "host.docker.internal"
    val RECIPIENT_LISTENER_PORT = (System.getenv("RECIPIENT_LISTENER_PORT") ?: "9999").toInt()
}
