package features.ping

import abilities.CommunicateViaDidcomm
import abilities.ListenToHttpMessages
import common.DidcommMessageTypes
import common.Ensure
import common.Environments.MEDIATOR_PEER_DID
import interactions.SendDidcommMessage
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import net.serenitybdd.rest.SerenityRest
import net.serenitybdd.screenplay.Actor
import org.apache.http.HttpStatus.SC_OK
import org.didcommx.didcomm.message.Message
import org.didcommx.didcomm.utils.idGeneratorDefault
import java.util.*

class PingProtocolSteps {
    @When("{actor} sends trusted ping message to mediator")
    fun iSendTrustedPingMessageToMediatorNatively(recipient: Actor) {
        val messageTrustPing = Message.builder(
            id = idGeneratorDefault(),
            body = mapOf("response_requested" to true),
            type = DidcommMessageTypes.PING_REQUEST
        ).from(recipient.recall("peerDid"))
            .to(listOf(MEDIATOR_PEER_DID)).build()

        recipient.attemptsTo(
            SendDidcommMessage(messageTrustPing)
        )
    }

    @Then("{actor} receives trusted ping message back")
    fun recipientGetTrustedPingMessageBackNatively(recipient: Actor) {
        val didCommResponse = recipient.usingAbilityTo(ListenToHttpMessages::class.java).receivedResponse()!!
        val didCommResponseMessage = recipient.usingAbilityTo(CommunicateViaDidcomm::class.java).unpackMessage(didCommResponse)

        recipient.attemptsTo(
            Ensure.that(SerenityRest.lastResponse().statusCode).isEqualTo(SC_OK),
            Ensure.that(didCommResponseMessage.type).isEqualTo(DidcommMessageTypes.PING_RESPONSE),
            Ensure.that(didCommResponseMessage.from!!).isEqualTo(MEDIATOR_PEER_DID.toString()),
            Ensure.that(didCommResponseMessage.to!!.first()).isEqualTo(recipient.recall("peerDid")),
        )
    }
}
