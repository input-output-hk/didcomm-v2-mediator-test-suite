package features.reportproblem

import abilities.CommunicateViaDidcomm
import common.DidcommMessageTypes
import common.Ensure
import common.ReportProblemErrors
import interactions.SendDidcommMessage
import interactions.SendEncryptedDidcommMessage
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import net.serenitybdd.screenplay.Actor
import org.didcommx.didcomm.message.Message
import org.didcommx.didcomm.utils.idGeneratorDefault

class ReportProblemProtocolSteps {

    @When("{actor} sends the same message twice")
    fun recipientSendsTheSameMessageTwice(recipient: Actor) {
        val basicMessage = Message.builder(
            id = idGeneratorDefault(),
            body = mapOf("content" to "message"),
            type = DidcommMessageTypes.BASIC_MESSAGE
        ).from(recipient.recall("peerDid"))
            .to(listOf(recipient.usingAbilityTo(CommunicateViaDidcomm::class.java).mediatorPeerDid))
            .customHeader("return_route", "all")
            .build()

        val encryptedDidcommMessage = recipient.usingAbilityTo(CommunicateViaDidcomm::class.java).packMessage(basicMessage)

        recipient.attemptsTo(
            SendEncryptedDidcommMessage(encryptedDidcommMessage),
            SendEncryptedDidcommMessage(encryptedDidcommMessage)
        )
    }

    @Then("{actor} gets report problem message with reply problem code")
    fun recipientGetsReportProblemMessageWithReplyProblemCode(recipient: Actor) {
        val reportProblemMessage = recipient.usingAbilityTo(CommunicateViaDidcomm::class.java).unpackLastDidcommMessage()
        recipient.attemptsTo(
            Ensure.that(reportProblemMessage.type).isEqualTo(DidcommMessageTypes.PROBLEM_REPORT),
            Ensure.that(reportProblemMessage.body["code"] as String).isEqualTo(ReportProblemErrors.STORAGE)
        )
    }

    @When("{actor} sends a message with unsupported protocol")
    fun recipientSendsAMessageWithUnsupportedProtocol(recipient: Actor) {
        val unsupportedProtocolMessage = Message.builder(
            id = idGeneratorDefault(),
            body = mapOf("response_requested" to true),
            type = "https://didcomm.org/unsupported-protocol"
        )
        recipient.attemptsTo(
            SendDidcommMessage(unsupportedProtocolMessage)
        )
    }

    @Then("{actor} gets report problem message with unsupported protocol problem code")
    fun recipientGetsReportProblemMessageWithUnsupportedProtocolProblemCode(recipient: Actor) {
        val reportProblemMessage = recipient.usingAbilityTo(CommunicateViaDidcomm::class.java).unpackLastDidcommMessage()
        recipient.attemptsTo(
            Ensure.that(reportProblemMessage.type).isEqualTo(DidcommMessageTypes.PROBLEM_REPORT),
            Ensure.that(reportProblemMessage.body["code"] as String).isEqualTo(ReportProblemErrors.UNSUPPORTED_PROTOCOL)
        )
    }

    @When("{actor} sends a message with unsupported version of protocol")
    fun recipientSendsAMessageWithUnsupportedVersionOfProtocol(recipient: Actor) {
        val unsupportedProtocolMessage = Message.builder(
            id = idGeneratorDefault(),
            body = mapOf("content" to "message"),
            type = "https://didcomm.org/basicmessage/100.0/message"
        )
        recipient.attemptsTo(
            SendDidcommMessage(unsupportedProtocolMessage)
        )
    }

    @When("{actor} sends a delivery-request message with another not enrolled peer did")
    fun recipientSendsADeliveryRequestMessageWithAnotherNotEnrolledPeerDid(recipient: Actor) {
        val notRegisteredPeerDid = recipient.usingAbilityTo(CommunicateViaDidcomm::class.java).createNewPeerDidService()
        val deliveryRequestMessage = Message.builder(
            id = idGeneratorDefault(),
            body = mapOf(
                "recipient_did" to notRegisteredPeerDid.did,
                "limit" to 1
            ),
            type = DidcommMessageTypes.PICKUP_DELIVERY_REQUEST
        )
        recipient.attemptsTo(
            SendDidcommMessage(deliveryRequestMessage)
        )
    }

    @Then("{actor} gets report problem message with not enrolled message problem code")
    fun recipientGetsReportProblemMessageWithNotEnrolledMessageProblemCode(recipient: Actor) {
        val reportProblemMessage = recipient.usingAbilityTo(CommunicateViaDidcomm::class.java).unpackLastDidcommMessage()
        recipient.attemptsTo(
            Ensure.that(reportProblemMessage.type).isEqualTo(DidcommMessageTypes.PROBLEM_REPORT),
            Ensure.that(reportProblemMessage.body["code"] as String).isEqualTo(ReportProblemErrors.NOT_ENROLL)
        )
    }

    @When("{actor} sends forward message to a DID that is not enrolled")
    fun recipientSendsForwardMessageToADIDThatIsNotEnrolled(recipient: Actor) {
        val notRegisteredPeerDid = recipient.usingAbilityTo(CommunicateViaDidcomm::class.java).createNewPeerDidService()
        val basicMessage = Message.builder(
            id = idGeneratorDefault(),
            body = mapOf("basic" to "message"),
            type = DidcommMessageTypes.BASIC_MESSAGE
        ).from(recipient.recall("peerDid"))
            .to(listOf(notRegisteredPeerDid.did)).build()

        val wrapInForwardResult = recipient.usingAbilityTo(CommunicateViaDidcomm::class.java).wrapInForward(
            basicMessage,
            notRegisteredPeerDid.did
        )
        recipient.attemptsTo(
            SendEncryptedDidcommMessage(wrapInForwardResult.msgEncrypted.packedMessage)
        )
    }

    @Then("{actor} gets signed report problem message with not enrolled message problem code")
    fun recipientGetsSignedReportProblemMessageWithNotEnrolledMessageProblemCode(recipient: Actor) {
        val reportProblemMessage = recipient.usingAbilityTo(CommunicateViaDidcomm::class.java).unpackLastSignedDidcommMessagePayload()
        recipient.attemptsTo(
            Ensure.that(reportProblemMessage.type).isEqualTo(DidcommMessageTypes.PROBLEM_REPORT),
            Ensure.that(reportProblemMessage.body["code"] as String).isEqualTo(ReportProblemErrors.NOT_ENROLL)
        )
    }

    @When("{actor} sends a message with tampered payload")
    fun recipientSendsAMessageWithTamperedPayload(recipient: Actor) {
        val basicMessage = Message.builder(
            id = idGeneratorDefault(),
            body = mapOf("content" to "message"),
            type = DidcommMessageTypes.BASIC_MESSAGE
        ).from(recipient.recall("peerDid"))
            .to(listOf(recipient.usingAbilityTo(CommunicateViaDidcomm::class.java).mediatorPeerDid))
            .customHeader("return_route", "all")
            .build()

        val encryptedDidcommMessage = recipient.usingAbilityTo(CommunicateViaDidcomm::class.java)
            .packMessage(basicMessage).replace("\"iv\":\"", "\"iv\":\"tampered")

        recipient.attemptsTo(
            SendEncryptedDidcommMessage(encryptedDidcommMessage)
        )
    }

    @Then("{actor} gets report problem message with crypto error problem code")
    fun recipientGetsReportProblemMessageWithCryptoErrorProblemCode(recipient: Actor) {
        val reportProblemMessage = recipient.usingAbilityTo(CommunicateViaDidcomm::class.java).unpackLastDidcommMessage()
        recipient.attemptsTo(
            Ensure.that(reportProblemMessage.type).isEqualTo(DidcommMessageTypes.PROBLEM_REPORT),
            Ensure.that(reportProblemMessage.body["code"] as String).isEqualTo(ReportProblemErrors.CRYPTO)
        )
    }

    @When("{actor} sends a plaintext message with malformed payload")
    fun recipientSendsAPlaintextMessageWithMalformedPayload(recipient: Actor) {
        val basicMessage = Message.builder(
            id = idGeneratorDefault(),
            body = mapOf("incorrect" to "message"),
            type = DidcommMessageTypes.BASIC_MESSAGE
        )
        recipient.attemptsTo(
            SendDidcommMessage(basicMessage)
        )
    }

    @Then("{actor} gets report problem message with malformed payload problem code")
    fun recipientGetsReportProblemMessageWithMalformedPayloadProblemCode(recipient: Actor) {
        val reportProblemMessage = recipient.usingAbilityTo(CommunicateViaDidcomm::class.java).unpackLastDidcommMessage()
        recipient.attemptsTo(
            Ensure.that(reportProblemMessage.type).isEqualTo(DidcommMessageTypes.PROBLEM_REPORT),
            Ensure.that(reportProblemMessage.body["code"] as String).isEqualTo(ReportProblemErrors.MESSAGE)
        )
    }
}
