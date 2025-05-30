//package models
//
//import io.ktor.http.HttpStatusCode
//import io.ktor.server.application.Application
//import io.ktor.server.application.call
//import io.ktor.server.engine.ApplicationEngine
//import io.ktor.server.engine.embeddedServer
//import io.ktor.server.netty.Netty
//import io.ktor.server.request.receiveText
//import io.ktor.server.response.respond
//import io.ktor.server.routing.post
//import io.ktor.server.routing.routing
//
//open class ListenToHttpMessages(
//    private val host: String,
//    private val port: Int
//) {
//    private val server: ApplicationEngine
//    private var receivedResponse: String? = null
//
//    init {
//        server = embeddedServer(
//            Netty,
//            host = "0.0.0.0",
//            port = port,
//            module = { route(this) }
//        )
//            .start(wait = false)
//    }
//
//    private fun route(application: Application) {
//        application.routing {
//            post("/") {
//                receivedResponse = call.receiveText()
//                println("????????????????????")
//                println(receivedResponse)
//                call.respond(HttpStatusCode.OK)
//            }
//        }
//    }
//
//    companion object {
//        fun at(host: String, port: Int): ListenToHttpMessages {
//            return ListenToHttpMessages(host, port)
//        }
//    }
//
//    fun receivedResponse(): String? {
//        return receivedResponse
//    }
//
//    override fun toString(): String {
//        return "Listen HTTP port at $host:$port"
//    }
//}
