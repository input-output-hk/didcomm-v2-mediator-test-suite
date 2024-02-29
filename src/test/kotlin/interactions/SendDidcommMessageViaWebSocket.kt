package interactions

import abilities.CommunicateOverWebSocket
import abilities.CommunicateViaDidcomm
import kotlinx.coroutines.runBlocking
import net.serenitybdd.core.Serenity
import net.serenitybdd.screenplay.Actor
import net.serenitybdd.screenplay.Interaction
import org.didcommx.didcomm.message.MessageBuilder

open class SendDidcommMessageViaWebSocket(
    private val messageBuilder: MessageBuilder,
    private val returnRoute: String = "all"
) : Interaction {

    override fun <T : Actor> performAs(actor: T) {
        val message = messageBuilder
            .from(actor.recall("peerDid"))
            .to(listOf(actor.usingAbilityTo(CommunicateViaDidcomm::class.java).mediatorPeerDid))
            .customHeader("return_route", returnRoute)
            .build()
        Serenity.recordReportData().withTitle("DIDComm Message").andContents(message.toString())

        val packedMessage = actor.usingAbilityTo(CommunicateViaDidcomm::class.java).packMessage(message)
        runBlocking {
            actor.abilityTo(CommunicateOverWebSocket::class.java).sendMessage(actor, packedMessage)
        }
    }
}
