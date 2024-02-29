package abilities

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.serenitybdd.screenplay.Ability
import net.serenitybdd.screenplay.Actor
import java.net.URI

class CommunicateOverWebSocket(private val host: String, private val port: Int, private val path: String) : Ability {

    private val client = HttpClient(CIO) {
        install(WebSockets)
    }

    suspend fun sendMessage(actor: Actor, packedMessage: String) {
        CoroutineScope(Dispatchers.IO).launch {
            client.webSocket(host = host, port = port, path = path) {
                send(Frame.Text(packedMessage))
                val responseFrame = incoming.receive()
                if (responseFrame is Frame.Text) {
                    val responseMessage = responseFrame.readText()
                    actor.remember("websocketResponse", responseMessage)
                }
            }
        }.join()
    }

    companion object {
        fun using(webserviceURL: String): CommunicateOverWebSocket {
            val uri = URI(webserviceURL)
            return CommunicateOverWebSocket(uri.host, uri.port, uri.path)
        }
    }
}
