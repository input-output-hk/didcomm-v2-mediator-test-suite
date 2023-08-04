package features.ping

import abilities.CommunicateViaDidcomm
import abilities.ListenToHttpMessages
import common.DidcommMessageTypes
import common.Ensure
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
    @When("{actor} sends trusted ping message to mediator with return_route {string}")
    fun iSendTrustedPingMessageToMediatorNatively(recipient: Actor, returnRoute: String) {
        val messageTrustPing = Message.builder(
            id = idGeneratorDefault(),
            body = mapOf("response_requested" to true),
            type = DidcommMessageTypes.PING_REQUEST
        ).customHeader("return_route", returnRoute)

        recipient.attemptsTo(
            SendDidcommMessage(messageTrustPing)
        )
    }

    @Then("{actor} receives trusted ping message back synchronously")
    fun recipientGetTrustedPingMessageBackSynchronously(recipient: Actor) {
        val syncResponse = recipient.usingAbilityTo(CommunicateViaDidcomm::class.java).unpackLastDidcommMessage()
        recipient.attemptsTo(
            Ensure.that(SerenityRest.lastResponse().statusCode).isEqualTo(SC_OK),
            Ensure.that(syncResponse.type).isEqualTo(DidcommMessageTypes.PING_RESPONSE),
            Ensure.that(syncResponse.to!!.first()).isEqualTo(recipient.recall("peerDid"))
        )
    }

    @Then("{actor} receives trusted ping message back asynchronously")
    fun recipientGetTrustedPingMessageBackAsynchronously(recipient: Actor) {
        val didCommResponse = recipient.usingAbilityTo(ListenToHttpMessages::class.java).receivedResponse()!!
        val didCommResponseMessage = recipient.usingAbilityTo(CommunicateViaDidcomm::class.java).unpackMessage(didCommResponse)
        recipient.attemptsTo(
            Ensure.that(SerenityRest.lastResponse().statusCode).isEqualTo(SC_OK),
            Ensure.that(didCommResponseMessage.type).isEqualTo(DidcommMessageTypes.PING_RESPONSE),
            Ensure.that(didCommResponseMessage.to!!.first()).isEqualTo(recipient.recall("peerDid"))
        )
    }
}
