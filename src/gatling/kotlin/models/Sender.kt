package models

import io.restassured.RestAssured
import io.restassured.builder.RequestSpecBuilder
import io.restassured.config.EncoderConfig
import io.restassured.internal.http.Status
import io.restassured.response.Response
import org.didcommx.didcomm.message.Message
import org.didcommx.didcomm.message.MessageBuilder
import org.didcommx.didcomm.utils.idGeneratorDefault
import org.didcommx.didcomm.utils.toJSONString

class Sender(
    val mediatorPeerDid: String,
    val mediatorUrl: String,
) {
    val didcomm = CommunicateViaDidcomm(mediatorPeerDid)

    fun sendEncryptedMessage(message: String): Response {
        // We have to rewrite spec to remove all unnecessary hardcoded headers
        // from standard serenity rest interaction
        val spec = RequestSpecBuilder().noContentType()
            .setBaseUri("")
            .setBasePath(mediatorUrl)
            .setContentType("application/didcomm-encrypted+json")
            .setConfig(
                RestAssured.config()
                    .encoderConfig(
                        EncoderConfig
                            .encoderConfig()
                            .appendDefaultContentCharsetToContentTypeIfUndefined(false)
                    )
            )
            .setBody(message)
            .build()
        return RestAssured.given().spec(spec).post("/")
    }

    fun sendMessage(messageBuilder: MessageBuilder): Response {
        val contentType = "application/didcomm-encrypted+json"
        val returnRoute = "all"

        val message = messageBuilder
            .from(didcomm.getDid())
            .to(listOf(mediatorPeerDid))
            .customHeader("return_route", returnRoute)
            .build()

        val spec = RequestSpecBuilder().noContentType()
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

    fun senderSentAForwardMessageToRecipient(n: String, recipient: Recipient) {
        val basicMessage = Message.builder(
            id = idGeneratorDefault(),
            body = mapOf("basic" to "message: $n"),
            type = DidcommMessageTypes.BASIC_MESSAGE
        ).from(
            didcomm.getDid()
        ).to(
            listOf(recipient.communicationPeerDidService!!.did)
        ).build()

        val wrapInForwardResult = didcomm.wrapInForward(
            basicMessage,
            recipient.communicationPeerDidService!!.did
        )

        val response = sendEncryptedMessage(wrapInForwardResult.msgEncrypted.packedMessage)
        assert(Status.SUCCESS.matches(response.statusCode))
        assert(response.body.asString() == "")
        val forwardMessage = wrapInForwardResult.msg
        val initialMessage = basicMessage
    }
}
