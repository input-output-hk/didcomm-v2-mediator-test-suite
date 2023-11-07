package features.ping

import abilities.CommunicateViaDidcomm
import abilities.ListenToHttpMessages
import common.DidcommMessageTypes
import common.Ensure
import interactions.SendDidcommMessage
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import io.restassured.internal.http.Status
import net.serenitybdd.rest.SerenityRest
import net.serenitybdd.screenplay.Actor
import org.didcommx.didcomm.message.Message
import org.didcommx.didcomm.utils.idGeneratorDefault

class PingProtocolSteps {
    @When("{actor} sends trusted ping message to mediator with return_route {string}")
    fun iSendTrustedPingMessageToMediatorNatively(recipient: Actor, returnRoute: String) {
        val messageTrustPing = Message.builder(
            id = idGeneratorDefault(),
            body = mapOf("response_requested" to true),
            type = DidcommMessageTypes.PING_REQUEST
        )

        recipient.attemptsTo(
            SendDidcommMessage(messageTrustPing, returnRoute = returnRoute)
        )
    }

    @Then("{actor} receives trusted ping message back synchronously")
    fun recipientGetTrustedPingMessageBackSynchronously(recipient: Actor) {
        val syncResponse = recipient.usingAbilityTo(CommunicateViaDidcomm::class.java).unpackLastDidcommMessage()
        recipient.attemptsTo(
            Ensure.that(Status.SUCCESS.matches(SerenityRest.lastResponse().statusCode)).isTrue(),
            Ensure.that(syncResponse.type).isEqualTo(DidcommMessageTypes.PING_RESPONSE),
            Ensure.that(syncResponse.to!!.first()).isEqualTo(recipient.recall("peerDid"))
        )
    }

    @Then("{actor} receives trusted ping message back asynchronously")
    fun recipientGetTrustedPingMessageBackAsynchronously(recipient: Actor) {
        val didCommResponse = recipient.usingAbilityTo(ListenToHttpMessages::class.java).receivedResponse()
            ?: throw Exception(
                "No async response received from mediator! " +
                    "There could be a problem with the mediator or a listener port can be not available."
            )
        val didCommResponseMessage = recipient.usingAbilityTo(CommunicateViaDidcomm::class.java).unpackMessage(didCommResponse)
        recipient.attemptsTo(
            Ensure.that(Status.SUCCESS.matches(SerenityRest.lastResponse().statusCode)).isTrue(),
            Ensure.that(didCommResponseMessage.type).isEqualTo(DidcommMessageTypes.PING_RESPONSE),
            Ensure.that(didCommResponseMessage.to!!.first()).isEqualTo(recipient.recall("peerDid"))
        )
    }

    @Then("{actor} receives no async message back")
    fun recipientReceivesNoAsyncMessageBack(recipient: Actor) {
        val didCommResponse: String? = recipient.usingAbilityTo(ListenToHttpMessages::class.java).receivedResponse()
        recipient.attemptsTo(
            Ensure.that(didCommResponse).isNull()
        )
    }

    @Then("{actor} receives no sync message back")
    fun recipientReceivesNoSyncMessageBack(recipient: Actor) {
        recipient.attemptsTo(
            Ensure.that(SerenityRest.lastResponse().body.print()).isEmpty()
        )
    }
}
