package features.pickupmessagelivemode

import abilities.CommunicateViaDidcomm
import common.DidcommMessageTypes
import common.Ensure
import interactions.SendDidcommMessage
import interactions.SendDidcommMessageViaWebSocket
import interactions.SendEncryptedDidcommMessage
import io.cucumber.java.en.Given
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import io.restassured.internal.http.Status
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.MissingFieldException
import kotlinx.serialization.json.Json
import models.MessagePickupStatusBody
import models.PeerDID
import net.serenitybdd.rest.SerenityRest
import net.serenitybdd.screenplay.Actor
import org.didcommx.didcomm.message.Attachment
import org.didcommx.didcomm.message.Message
import org.didcommx.didcomm.protocols.routing.ForwardMessage
import org.didcommx.didcomm.utils.idGeneratorDefault
import org.didcommx.didcomm.utils.toJSONString
import java.util.*

class PickupMessageLiveModeSteps {

    @Given("{actor} sent a forward message in live mode to {actor}")
    fun senderSentAForwardMessageToRecipient(sender: Actor, recipient: Actor) {
        val basicMessage = Message.builder(
            id = idGeneratorDefault(),
            body = mapOf("basic" to "message"),
            type = DidcommMessageTypes.BASIC_MESSAGE
        ).from(sender.recall("peerDid"))
            .to(listOf(recipient.recall<PeerDID>("communicationPeerDidService").did)).build()

        val wrapInForwardResult = sender.usingAbilityTo(CommunicateViaDidcomm::class.java).wrapInForward(
            basicMessage,
            recipient.recall<PeerDID>("communicationPeerDidService").did
        )
        sender.attemptsTo(
            SendEncryptedDidcommMessage(wrapInForwardResult.msgEncrypted.packedMessage)
        )

        sender.attemptsTo(
            Ensure.that(Status.SUCCESS.matches(SerenityRest.lastResponse().statusCode)).isTrue(),
            Ensure.that(SerenityRest.lastResponse().body.prettyPrint()).isEqualTo("")
                .withReportedError("Response body for POSTing forward message must be empty!")
        )

        sender.remember("forwardMessage", wrapInForwardResult.msg)
        sender.remember("initialMessage", basicMessage)
    }

    @When("{actor} sends a live delivery change message with live_delivery to true")
    fun recipientSendsALiveDeliveryChangeMessage(recipient: Actor) {
        val liveDeliveryChangeMessage = Message.builder(
            id = idGeneratorDefault(),
            body = mapOf("live_delivery" to "true"),
            type = DidcommMessageTypes.PICKUP_LIVE_MODE_DELIVERY
        )

        recipient.attemptsTo(
            SendDidcommMessage(liveDeliveryChangeMessage)
        )
    }

    @When("{actor} sends a status-request in live mode message")
    fun recipientSendsAStatusRequestMessage(recipient: Actor) {
        val statusRequestMessage = Message.builder(
            id = idGeneratorDefault(),
            body = mapOf("recipient_did" to recipient.recall<PeerDID>("communicationPeerDidService").did),
            type = DidcommMessageTypes.PICKUP_STATUS_REQUEST
        )

        recipient.attemptsTo(
            SendDidcommMessageViaWebSocket(statusRequestMessage)
        )
    }

    @OptIn(ExperimentalSerializationApi::class)
    @Then("Mediator responds with a status message with {int} queued messages in live mode of {actor}")
    fun mediatorRespondsWithAStatusMessageDetailingTheQueuedMessagesOfRecipient(numberOfMessages: Int, recipient: Actor) {
        val response = recipient.recall<String>("websocketResponse")
        val didcommResponse = recipient.usingAbilityTo(CommunicateViaDidcomm::class.java).unpackMessage(response)
        try {
            val pickupStatus = Json.decodeFromString<MessagePickupStatusBody>(didcommResponse.body.toJSONString())
            recipient.attemptsTo(
                Ensure.that(didcommResponse.type).isEqualTo(DidcommMessageTypes.PICKUP_STATUS),
                Ensure.that(pickupStatus.recipient_did).isEqualTo(recipient.recall<PeerDID>("communicationPeerDidService").did),
                Ensure.that(pickupStatus.message_count).isEqualTo(numberOfMessages)
            )
        } catch (_: MissingFieldException) {
            recipient.attemptsTo(
                Ensure.that(didcommResponse.type).isEqualTo(DidcommMessageTypes.PICKUP_DELIVERY),
                Ensure.that(didcommResponse.attachments!!.size).isEqualTo(0)
            )
        }
    }

    @When("{actor} sends a delivery-request message in live mode over websocket")
    fun recipientSendsADeliveryRequestMessage(recipient: Actor) {
        val deliveryRequestMessage = Message.builder(
            id = idGeneratorDefault(),
            body = mapOf(
                "recipient_did" to recipient.recall<PeerDID>("communicationPeerDidService").did,
                "limit" to 1
            ),
            type = DidcommMessageTypes.PICKUP_DELIVERY_REQUEST
        )
        recipient.attemptsTo(
            SendDidcommMessageViaWebSocket(deliveryRequestMessage)
        )
    }

    @Then("Mediator delivers message in live mode of {actor} to {actor}")
    fun mediatorDeliversMessageOfSenderToRecipient(sender: Actor, recipient: Actor) {
        val response = recipient.recall<String>("websocketResponse")
        val didcommResponse = recipient.usingAbilityTo(CommunicateViaDidcomm::class.java).unpackMessage(response)

        recipient.attemptsTo(
            Ensure.that(didcommResponse.attachments!!.size).isEqualTo(1)
        )

        val encryptedMessageSent = sender.recall<ForwardMessage>("forwardMessage").message.attachments!!.first().data as Attachment.Data.Json

        val attachment: Attachment = didcommResponse.attachments!!.findLast { attachment ->
            if (attachment.data.toJSONObject().keys.contains("json")) {
                val data: Attachment.Data.Json = attachment.data as Attachment.Data.Json
                data.json!!.toJSONString() == encryptedMessageSent.json!!.toJSONString()
            } else if (attachment.data.toJSONObject().keys.contains("base64")) {
                val data: Attachment.Data.Base64 = attachment.data as Attachment.Data.Base64
                Base64.getUrlDecoder().decode(data.base64).decodeToString() == encryptedMessageSent.json!!.toJSONString()
            } else {
                throw Exception("Delivery Message attachment format is not JSON or Base64!")
            }
        } ?: throw Exception("Sender's attachment is not found in the delivery message! The message was not delivered.")

        val message: String = if (attachment.data.toJSONObject().keys.contains("json")) {
            val data: Attachment.Data.Json = attachment.data as Attachment.Data.Json
            data.json!!.toString()
        } else if (attachment.data.toJSONObject().keys.contains("base64")) {
            val data: Attachment.Data.Base64 = attachment.data as Attachment.Data.Base64
            Base64.getUrlDecoder().decode(data.base64).decodeToString()
        } else {
            throw Exception("Delivery Message attachment format is not JSON or Base64!")
        }

        val achievedMessage = recipient.usingAbilityTo(CommunicateViaDidcomm::class.java).unpackMessage(
            message,
            recipient.recall<PeerDID>("communicationPeerDidService").getSecretResolverInMemory()
        )
        val initialMessage = sender.recall<Message>("initialMessage")
        recipient.attemptsTo(
            Ensure.that(didcommResponse.type).isEqualTo(DidcommMessageTypes.PICKUP_DELIVERY)
                .withReportedError("Response type for delivery request MUST be delivery!"),
            Ensure.that(achievedMessage.id).isEqualTo(initialMessage.id)
                .withReportedError("Sender message ID has been changed!"),
            Ensure.that(achievedMessage.body.toJSONString()).isEqualTo(initialMessage.body.toJSONString())
                .withReportedError("Sender message body has been changed!"),
            Ensure.that(achievedMessage.from.toString()).isEqualTo(initialMessage.from.toString())
                .withReportedError("Sender message 'from' has been changed!"),
            Ensure.that(achievedMessage.to!!.first()).isEqualTo(initialMessage.to!!.first())
                .withReportedError("Sender message 'to' has been changed!")
        )
        recipient.remember("deliveryThid", didcommResponse.thid!!)
        recipient.remember("attachmentId", didcommResponse.attachments!!.first().id)
    }

    @When("{actor} sends a messages-received message in live mode")
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
}
