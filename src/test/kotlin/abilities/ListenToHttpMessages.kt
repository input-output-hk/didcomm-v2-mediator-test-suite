package abilities

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import net.serenitybdd.screenplay.Ability
import net.serenitybdd.screenplay.Actor
import net.serenitybdd.screenplay.HasTeardown

open class ListenToHttpMessages(
    private val host: String,
    private val port: Int
) : Ability, HasTeardown {

    private val server: ApplicationEngine
    private var receivedResponse: String? = null

    init {
        server = embeddedServer(
            Netty,
            host = "0.0.0.0",
            port = port,
            module = { route(this) }
        )
            .start(wait = false)
    }

    private fun route(application: Application) {
        application.routing {
            post("/") {
                receivedResponse = call.receiveText()
                call.respond(HttpStatusCode.OK)
            }
        }
    }

    companion object {
        fun at(host: String, port: Int): ListenToHttpMessages {
            return ListenToHttpMessages(host, port)
        }

        fun `as`(actor: Actor): ListenToHttpMessages {
            return actor.abilityTo(ListenToHttpMessages::class.java)
        }
    }

    fun receivedResponse(): String? {
        return receivedResponse
    }

    override fun toString(): String {
        return "Listen HTTP port at $host:$port"
    }

    override fun tearDown() {
        server.stop()
    }
}
