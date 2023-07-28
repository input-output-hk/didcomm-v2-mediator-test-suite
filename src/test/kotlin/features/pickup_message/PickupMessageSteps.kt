package features.pickup_message

import abilities.CommunicateViaDidcomm
import common.*
import common.Environments.MEDIATOR_PEER_DID
import interactions.SendEncryptedDidcommMessage
import interactions.SendDidcommMessage
import io.cucumber.java.en.Given
import net.serenitybdd.screenplay.Actor
import io.cucumber.java.en.When
import io.cucumber.java.en.Then
import io.ktor.http.*
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import models.MessagePickupStatusBody
import org.didcommx.didcomm.message.Attachment
import org.didcommx.didcomm.message.Message
import org.didcommx.didcomm.utils.idGeneratorDefault
import org.didcommx.didcomm.utils.toJSONString
import java.util.*

class PickupMessageSteps {

    @Given("{actor} sent a forward message to {actor}")
    fun senderSentAForwardMessageToRecipient(sender: Actor, recipient: Actor) {

        val basicMessage = Message.builder(
            id = idGeneratorDefault(),
            body = mapOf("basic" to "message"),
            type = DidcommMessageTypes.BASIC_MESSAGE
        ).from(sender.recall("peerDid"))
            .to(listOf(recipient.recall("peerDid"))).build()

        // Native forward message creation
//        val packedBasic = sender.usingAbilityTo(CommunicateViaDidcomm::class.java).packMessage(
//            basicMessage, recipient.recall("peerDid")
//        )
//        val forwardMessage = Message.builder(
//            id = UUID.randomUUID().toString(),
//            body = mapOf("next" to recipient.recall("peerDid")),
//            type = DidcommMessageTypes.FORWARD_REQUEST,
//        ).from(sender.recall("peerDid"))
//            .to(listOf(MEDIATOR_PEER_DID))
//            .attachments(
//                listOf(
//                    Attachment.builder(
//                        didcommIdGeneratorDefault(), Attachment.Data.Json(JSONObjectUtils.parse(packedBasic))
//                    ).mediaType(ContentType.Application.Json.toString()).build()
//                )
//            ).build()
//        sender.attemptsTo(
//            SendPlainDidcommMessage(forwardMessage)
//        )

        val wrapInForwardResult = sender.usingAbilityTo(CommunicateViaDidcomm::class.java).wrapInForward(
            basicMessage,
            recipient.recall("peerDid"),
            routingKeys = listOf(MEDIATOR_PEER_DID)
        )
        sender.attemptsTo(
            SendEncryptedDidcommMessage(wrapInForwardResult.msgEncrypted.packedMessage)
        )

        sender.remember("initialMessage", basicMessage)

    }

    @When("{actor} sends a status-request message")
    fun recipientSendsAStatusRequestMessage(recipient: Actor) {

        val statusRequestMessage = Message.builder(
            id = idGeneratorDefault(),
            body = mapOf("recipient_did" to recipient.recall("peerDid")),
            type = DidcommMessageTypes.PICKUP_STATUS_REQUEST
        ).from(recipient.recall("peerDid"))
            .to(listOf(MEDIATOR_PEER_DID)).build()

        recipient.attemptsTo(
            SendDidcommMessage(statusRequestMessage)
        )
    }

    @Then("Mediator responds with a status message detailing the queued messages of {actor}")
    fun mediatorRespondsWithAStatusMessageDetailingTheQueuedMessagesOfRecipient(recipient: Actor) {
        val didcommResponse = recipient.usingAbilityTo(CommunicateViaDidcomm::class.java).unpackLastDidcommMessage()
        val pickupStatus = Json.decodeFromString<MessagePickupStatusBody>(didcommResponse.body.toJSONString())
        recipient.attemptsTo(
            Ensure.that(pickupStatus.recipient_did).isEqualTo(recipient.recall("peerDid")),
            Ensure.that(pickupStatus.message_count).isGreaterThan(0)
        )
    }

    @When("{actor} sends a delivery-request message")
    fun recipientSendsADeliveryRequestMessage(recipient: Actor) {
        val deliveryRequestMessage = Message.builder(
            id = idGeneratorDefault(),
            body = mapOf(
                "recipient_did" to recipient.recall("peerDid"),
                "limit" to 3
            ),
            type = DidcommMessageTypes.PICKUP_DELIVERY_REQUEST
        ).from(recipient.recall("peerDid"))
            .to(listOf(MEDIATOR_PEER_DID)).build()
        recipient.attemptsTo(
            SendDidcommMessage(deliveryRequestMessage)
        )
    }

    @Then("Mediator delivers message of {actor} to {actor}")
    fun mediatorDeliversMessageOfSenderToRecipient(sender: Actor, recipient: Actor) {
        val didcommResponse = recipient.usingAbilityTo(CommunicateViaDidcomm::class.java).unpackLastDidcommMessage()
        val encryptedMessage: Attachment.Data.Base64 = didcommResponse.attachments!!.first().data as Attachment.Data.Base64
        val decryptedMessage = Base64.getUrlDecoder().decode(encryptedMessage.base64).decodeToString()
        val achievedMessage = recipient.usingAbilityTo(CommunicateViaDidcomm::class.java).unpackMessage(decryptedMessage)
        val initialMessage = sender.recall<Message>("initialMessage")
        recipient.attemptsTo(
            Ensure.that(didcommResponse.attachments!!.size).isEqualTo(1),
            Ensure.that(achievedMessage.id).isEqualTo(initialMessage.id),
            Ensure.that(achievedMessage.body.toJSONString()).isEqualTo(initialMessage.body.toJSONString()),
            Ensure.that(achievedMessage.from.toString()).isEqualTo(initialMessage.from.toString()),
            Ensure.that(achievedMessage.to!!.first()).isEqualTo(initialMessage.to!!.first())
        )
    }
}
