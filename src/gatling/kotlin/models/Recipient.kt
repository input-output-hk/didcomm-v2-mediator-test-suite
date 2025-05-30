package models

import io.restassured.RestAssured
import io.restassured.builder.RequestSpecBuilder
import io.restassured.config.EncoderConfig
import io.restassured.response.Response
import kotlinx.serialization.json.Json
import org.didcommx.didcomm.message.Attachment
import org.didcommx.didcomm.message.Message
import org.didcommx.didcomm.message.MessageBuilder
import org.didcommx.didcomm.utils.idGeneratorDefault
import org.didcommx.didcomm.utils.toJSONString
import java.util.Base64

class Recipient(
    val mediatorPeerDid: String,
    val mediatorUrl: String,
    val host: String = "host.docker.internal",
    val port: Int = 9999
) {
    var routingDid: String? = null
    var communicationPeerDidService: PeerDID? = null

//    val serviceEndpoint = "http://${host}:998}/"
    val didcomm = CommunicateViaDidcomm(mediatorPeerDid)//, serviceEndpoint)
//    val listen = ListenToHttpMessages(host, port)

    fun sendMessage(messageBuilder: MessageBuilder): Response {
        val contentType = "application/didcomm-encrypted+json"
        val returnRoute = "all"

        val message = messageBuilder
            .from(didcomm.getDid())
            .to(listOf(mediatorPeerDid))
            .customHeader("return_route", returnRoute)
            .build()

        val spec = RequestSpecBuilder().noContentType()
            .setBaseUri("")
            .setBasePath(mediatorUrl)
            .setContentType(contentType)
            .setConfig(
                RestAssured.config()
                    .encoderConfig(
                        EncoderConfig
                            .encoderConfig()
                            .appendDefaultContentCharsetToContentTypeIfUndefined(false)
                    )
            )
            .setBody(didcomm.packMessage(message))
            .build()
        val response = RestAssured.given().spec(spec).post("/")
        return response
    }

    fun recipientSendsAMediateRequestMessageToTheMediator() {
        val messageMediationRequest = Message.builder(
            id = idGeneratorDefault(),
            body = mapOf(),
            type = DidcommMessageTypes.MEDIATE_REQUEST
        )

        val response = sendMessage(messageMediationRequest)
        val didcommResponse = didcomm.unpackMessage(response.asString())
        val mediationResponse = Json.decodeFromString<MediationResponse>(didcommResponse.body.toJSONString())
        assert(didcommResponse.type == DidcommMessageTypes.MEDIATE_GRANT, { "Didcomm type is not expected" })
        assert(mediationResponse.routing_did.isNotEmpty(), { "Mediator response is empty" })
        routingDid = mediationResponse.routing_did
    }

    fun recipientSendsALiveDeliveryChangeMessage() {
        val liveDeliveryChangeMessage = Message.builder(
            id = idGeneratorDefault(),
            body = mapOf("live_delivery" to "true"),
            type = DidcommMessageTypes.PICKUP_LIVE_MODE_DELIVERY
        )

        sendMessage(liveDeliveryChangeMessage)
    }

    fun recipientSendsAKeylistUpdateMessageToTheMediatorWithANewPeerDid() {
        this.communicationPeerDidService = didcomm.createNewPeerDidService(this.routingDid!!)
        assert(communicationPeerDidService!!.did != didcomm.getDid(), { "did was equal" })

        val messageKeylistUpdate = Message.builder(
            id = idGeneratorDefault(),
            body = mapOf(
                "updates" to listOf(
                    mapOf(
                        "action" to "add",
                        "recipient_did" to communicationPeerDidService!!.did
                    )
                )
            ),
            type = DidcommMessageTypes.MEDIATE_KEYLIST_UPDATE
        )
        val response = sendMessage(messageKeylistUpdate)
//        response.prettyPrint()
//        val didcommResponse = didcomm.unpackMessage(response.asString())
//        println(didcommResponse.body.toJSONString())
    }

    fun recipientSendsAStatusRequestMessage() {
        val statusRequestMessage = Message.builder(
            id = idGeneratorDefault(),
            body = mapOf("recipient_did" to communicationPeerDidService!!.did),
            type = DidcommMessageTypes.PICKUP_STATUS_REQUEST
        )

        val response = sendMessage(statusRequestMessage)
//        response.prettyPrint()
//        val didcommResponse = didcomm.unpackMessage(response.asString())
//        println(didcommResponse.body.toJSONString())
    }

    fun recipientSendsADeliveryRequestMessage(): ReceivedMessages {
        val deliveryRequestMessage = Message.builder(
            id = idGeneratorDefault(),
            body = mapOf(
                "recipient_did" to communicationPeerDidService!!.did,
                "limit" to 10
            ),
            type = DidcommMessageTypes.PICKUP_DELIVERY_REQUEST
        )
        val response = sendMessage(deliveryRequestMessage)
        val receivedMessage = didcomm.unpackMessage(response.asString())
        val thid = receivedMessage.thid
        val idList = mutableListOf<String>()
        receivedMessage.attachments?.forEach {
            val base64 = it.data as Attachment.Data.Base64
            val message = Base64.getUrlDecoder().decode(base64.base64).decodeToString()
            val achievedMessage = didcomm.unpackMessage(
                message,
                communicationPeerDidService!!.getSecretResolverInMemory()
            )
            idList.add(it.id)
            println(achievedMessage)
        }
        return ReceivedMessages(thid!!, idList)
    }

    fun recipientSendsAMessagesReceivedMessage(thid: String, readMessages: List<String>) {
        val messagesReceivedMessage = Message.builder(
            id = idGeneratorDefault(),
            body = mapOf(
//                "message_id_list" to listOf(recipient.recall<String>("attachmentId"))
                "message_id_list" to readMessages
            ),
            type = DidcommMessageTypes.PICKUP_MESSAGES_RECEIVED
        ).thid(thid)

        sendMessage(messagesReceivedMessage)
    }
}
