package features.pickupmessage

import abilities.CommunicateViaDidcomm
import common.*
import interactions.SendDidcommMessage
import interactions.SendEncryptedDidcommMessage
import io.cucumber.java.en.Given
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import io.ktor.http.*
import kotlinx.serialization.json.Json
import models.MessagePickupStatusBody
import net.serenitybdd.screenplay.Actor
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

        val wrapInForwardResult = sender.usingAbilityTo(CommunicateViaDidcomm::class.java).wrapInForward(
            basicMessage,
            recipient.recall("peerDid")
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
        )

        recipient.attemptsTo(
            SendDidcommMessage(statusRequestMessage)
        )
    }

    @Then("Mediator responds with a status message with {int} queued messages of {actor}")
    fun mediatorRespondsWithAStatusMessageDetailingTheQueuedMessagesOfRecipient(numberOfMessages: Int, recipient: Actor) {
        val didcommResponse = recipient.usingAbilityTo(CommunicateViaDidcomm::class.java).unpackLastDidcommMessage()
        val pickupStatus = Json.decodeFromString<MessagePickupStatusBody>(didcommResponse.body.toJSONString())
        recipient.attemptsTo(
            Ensure.that(pickupStatus.recipient_did).isEqualTo(recipient.recall("peerDid")),
            Ensure.that(pickupStatus.message_count).isEqualTo(numberOfMessages)
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
        )
        recipient.attemptsTo(
            SendDidcommMessage(deliveryRequestMessage)
        )
    }

    @Then("Mediator delivers message of {actor} to {actor}")
    fun mediatorDeliversMessageOfSenderToRecipient(sender: Actor, recipient: Actor) {
        val didcommResponse = recipient.usingAbilityTo(CommunicateViaDidcomm::class.java).unpackLastDidcommMessage()
        val encryptedMessage = didcommResponse.attachments!!.first().data as Attachment.Data.Base64
        val decryptedMessage = Base64.getUrlDecoder().decode(encryptedMessage.base64).decodeToString()
        val achievedMessage = recipient.usingAbilityTo(CommunicateViaDidcomm::class.java).unpackMessage(decryptedMessage)
        val initialMessage = sender.recall<Message>("initialMessage")
        recipient.attemptsTo(
            Ensure.that(didcommResponse.type).isEqualTo(DidcommMessageTypes.PICKUP_DELIVERY),
            Ensure.that(didcommResponse.attachments!!.size).isEqualTo(1),
            Ensure.that(achievedMessage.id).isEqualTo(initialMessage.id),
            Ensure.that(achievedMessage.body.toJSONString()).isEqualTo(initialMessage.body.toJSONString()),
            Ensure.that(achievedMessage.from.toString()).isEqualTo(initialMessage.from.toString()),
            Ensure.that(achievedMessage.to!!.first()).isEqualTo(initialMessage.to!!.first())
        )
        recipient.remember("deliveryThid", didcommResponse.thid!!)
        recipient.remember("attachmentId", didcommResponse.attachments!!.first().id)
    }

    @When("{actor} sends a messages-received message")
    fun recipientSendsAMessagesReceivedMessage(recipient: Actor) {
        val messagesReceivedMessage = Message.builder(
            id = idGeneratorDefault(),
            body = mapOf(
                "message_id_list" to listOf(recipient.recall<String>("attachmentId"))
            ),
            type = DidcommMessageTypes.PICKUP_MESSAGES_RECEIVED
        ).thid(recipient.recall<String>("deliveryThid"))

        recipient.attemptsTo(
            SendDidcommMessage(messagesReceivedMessage)
        )
    }

    @Then("Mediator responds that there are no messages for {actor}")
    fun mediatorRespondsThatThereAreNoMessagesFrom(recipient: Actor) {
        val didcommResponse = recipient.usingAbilityTo(CommunicateViaDidcomm::class.java).unpackLastDidcommMessage()
        recipient.attemptsTo(
            Ensure.that(didcommResponse.type).isEqualTo(DidcommMessageTypes.PICKUP_STATUS),
            Ensure.that(didcommResponse.attachments!!.size).isEqualTo(0)
        )
    }
}
