package features.mediatorcoordination

import abilities.CommunicateViaDidcomm
import common.*
import interactions.SendDidcommMessage
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import kotlinx.serialization.json.Json
import models.*
import net.serenitybdd.screenplay.Actor
import org.didcommx.didcomm.message.Message
import org.didcommx.didcomm.utils.idGeneratorDefault
import org.didcommx.didcomm.utils.toJSONString
import java.util.*

class MediationCoordinationSteps {

    @When("{actor} sends a mediate request message to the mediator")
    fun recipientSendsAMediateRequestMessageToTheMediator(recipient: Actor) {
        val messageMediationRequest = Message.builder(
            id = idGeneratorDefault(),
            body = mapOf(),
            type = DidcommMessageTypes.MEDIATE_REQUEST
        )
        recipient.attemptsTo(
            SendDidcommMessage(messageMediationRequest)
        )
    }

    @Then("Mediator responds to {actor} with mediate grant message")
    fun mediatorRespondsToHimWithMediateGrantMessage(recipient: Actor) {
        val didcommResponse = recipient.usingAbilityTo(CommunicateViaDidcomm::class.java).unpackLastDidcommMessage()
        val mediationResponse = Json.decodeFromString<MediationResponse>(didcommResponse.body.toJSONString())
        recipient.attemptsTo(
            Ensure.that(didcommResponse.type).isEqualTo(DidcommMessageTypes.MEDIATE_GRANT),
            Ensure.that(mediationResponse.routing_did).isNotEmpty()
        )
        recipient.remember("routingDid", mediationResponse.routing_did)
    }

    @Then("Mediator responds to {actor} with mediate deny message")
    fun mediatorRespondsToHimWithMediateDenyMessage(recipient: Actor) {
        val didcommResponse = recipient.usingAbilityTo(CommunicateViaDidcomm::class.java).unpackLastDidcommMessage()
        val mediationResponse = Json.decodeFromString<MediationResponse>(didcommResponse.body.toJSONString())
        recipient.attemptsTo(
            Ensure.that(didcommResponse.type).isEqualTo(DidcommMessageTypes.MEDIATE_DENY),
            Ensure.that(mediationResponse.routing_did).isNotEmpty()
        )
    }

    @When("{actor} sends a keylist update message to the mediator with a new peer did")
    fun recipientSendsAKeylistUpdateMessageToTheMediatorWithANewPeerDid(recipient: Actor) {
        val communicationPeerDidService = recipient.usingAbilityTo(CommunicateViaDidcomm::class.java).createNewPeerDidService(
            recipient.recall("routingDid")
        )
        recipient.attemptsTo(
            Ensure.that(communicationPeerDidService.did).isNotEqualTo(recipient.recall("peerDid"))
        )
        recipient.remember("communicationPeerDidService", communicationPeerDidService)
        val messageKeylistUpdate = Message.builder(
            id = idGeneratorDefault(),
            body = mapOf(
                "updates" to listOf(
                    mapOf(
                        "action" to TestConstants.MEDIATOR_COORDINATION_ACTION_ADD,
                        "recipient_did" to communicationPeerDidService.did
                    )
                )
            ),
            type = DidcommMessageTypes.MEDIATE_KEYLIST_UPDATE
        )
        recipient.attemptsTo(
            SendDidcommMessage(messageKeylistUpdate)
        )
    }

    @Then("Mediator responds to {actor} with a correct keylist update add message")
    fun mediatorRespondsToHimWithACorrectKeylistUpdateAddMessage(recipient: Actor) {
        val didcommResponse = recipient.usingAbilityTo(CommunicateViaDidcomm::class.java).unpackLastDidcommMessage()
        val mediationKeylistUpdateResponse = Json.decodeFromString<MediationKeylistUpdateResponse>(didcommResponse.body.toJSONString())
        recipient.attemptsTo(
            Ensure.that(didcommResponse.type).isEqualTo(DidcommMessageTypes.MEDIATE_KEYLIST_UPDATE_RESPONSE),
            Ensure.that(mediationKeylistUpdateResponse.updated.size).isEqualTo(1),
            Ensure.that(mediationKeylistUpdateResponse.updated[0].result).isEqualTo(TestConstants.MEDIATOR_COORDINATION_ACTION_RESULT_SUCCESS),
            Ensure.that(mediationKeylistUpdateResponse.updated[0].action).isEqualTo(TestConstants.MEDIATOR_COORDINATION_ACTION_ADD),
            Ensure.that(mediationKeylistUpdateResponse.updated[0].recipient_did).isEqualTo(recipient.recall<PeerDID>("communicationPeerDidService").did)
        )
    }

    @When("{actor} sends a keylist query message to the mediator")
    fun recipientSendsAKeylistQueryMessageToTheMediator(recipient: Actor) {
        val messageKeylistQuery = Message.builder(
            id = idGeneratorDefault(),
            body = mapOf(
                "paginate" to
                    mapOf(
                        "limit" to 2,
                        "offset" to 0
                    )
            ),
            type = DidcommMessageTypes.MEDIATE_KEYLIST_QUERY
        )
        recipient.attemptsTo(
            SendDidcommMessage(messageKeylistQuery)
        )
    }

    @Then("Mediator responds to {actor} with keylist message containing the current list of keys")
    fun mediatorRespondsToRecipientWithKeylistMessageContainingTheCurrentListOfKeys(recipient: Actor) {
        val didcommResponse = recipient.usingAbilityTo(CommunicateViaDidcomm::class.java).unpackLastDidcommMessage()
        val mediationKeylistResponse = Json.decodeFromString<MediationKeylistResponse>(didcommResponse.body.toJSONString())
        recipient.attemptsTo(
            Ensure.that(didcommResponse.type).isEqualTo(DidcommMessageTypes.MEDIATE_KEYLIST),
            Ensure.that(mediationKeylistResponse.keys.size).isGreaterThan(0),
            Ensure.that(mediationKeylistResponse.keys).contains(MediationKeylistKey(recipient.recall<PeerDID>("communicationPeerDidService").did))
        )
    }

    @When("{actor} sends a keylist update message to the mediator to remove the last alias")
    fun recipientSendsAKeylistUpdateMessageToTheMediatorToRemoveAKey(recipient: Actor) {
        val messageKeylistUpdate = Message.builder(
            id = idGeneratorDefault(),
            body = mapOf(
                "updates" to listOf(
                    mapOf(
                        "action" to TestConstants.MEDIATOR_COORDINATION_ACTION_REMOVE,
                        "recipient_did" to recipient.recall("peerDid")
                    )
                )
            ),
            type = DidcommMessageTypes.MEDIATE_KEYLIST_UPDATE
        )
        recipient.attemptsTo(
            SendDidcommMessage(messageKeylistUpdate)
        )
    }

    @When("{actor} sends a keylist update message to the mediator to remove added alias")
    fun recipientSendsAKeylistUpdateMessageToTheMediatorToRemoveAddedAlias(recipient: Actor) {
        val messageKeylistUpdate = Message.builder(
            id = idGeneratorDefault(),
            body = mapOf(
                "updates" to listOf(
                    mapOf(
                        "action" to TestConstants.MEDIATOR_COORDINATION_ACTION_REMOVE,
                        "recipient_did" to recipient.recall<PeerDID>("communicationPeerDidService").did
                    )
                )
            ),
            type = DidcommMessageTypes.MEDIATE_KEYLIST_UPDATE
        )
        recipient.attemptsTo(
            SendDidcommMessage(messageKeylistUpdate)
        )
    }

    @Then("Mediator responds to {actor} with a correct keylist update remove message")
    fun mediatorRespondsToRecipientWithACorrectKeylistUpdateRemoveMessage(recipient: Actor) {
        val didcommResponse = recipient.usingAbilityTo(CommunicateViaDidcomm::class.java).unpackLastDidcommMessage()
        val mediationKeylistUpdateResponse = Json.decodeFromString<MediationKeylistUpdateResponse>(didcommResponse.body.toJSONString())
        recipient.attemptsTo(
            Ensure.that(didcommResponse.type).isEqualTo(DidcommMessageTypes.MEDIATE_KEYLIST_UPDATE_RESPONSE),
            Ensure.that(mediationKeylistUpdateResponse.updated.size).isGreaterThan(0),
            Ensure.that(mediationKeylistUpdateResponse.updated[0].result).isEqualTo(TestConstants.MEDIATOR_COORDINATION_ACTION_RESULT_SUCCESS),
            Ensure.that(mediationKeylistUpdateResponse.updated[0].action).isEqualTo(TestConstants.MEDIATOR_COORDINATION_ACTION_REMOVE)
        )
    }

    @When("{actor} sends a keylist update message to the mediator to remove not existing alias")
    fun recipientSendsAKeylistUpdateMessageToTheMediatorToRemoveNotExistingAlias(recipient: Actor) {
        val messageKeylistUpdate = Message.builder(
            id = idGeneratorDefault(),
            body = mapOf(
                "updates" to listOf(
                    mapOf(
                        "action" to TestConstants.MEDIATOR_COORDINATION_ACTION_REMOVE,
                        "recipient_did" to TestConstants.EXAMPLE_DID
                    )
                )
            ),
            type = DidcommMessageTypes.MEDIATE_KEYLIST_UPDATE
        )
        recipient.attemptsTo(
            SendDidcommMessage(messageKeylistUpdate)
        )
    }

    @Then("Mediator responds to {actor} with a message with no_change status")
    fun mediatorRespondsToRecipientWithAMessageWithNo_changeStatus(recipient: Actor) {
        val didcommResponse = recipient.usingAbilityTo(CommunicateViaDidcomm::class.java).unpackLastDidcommMessage()
        val mediationKeylistUpdateResponse = Json.decodeFromString<MediationKeylistUpdateResponse>(didcommResponse.body.toJSONString())
        recipient.attemptsTo(
            Ensure.that(didcommResponse.type).isEqualTo(DidcommMessageTypes.MEDIATE_KEYLIST_UPDATE_RESPONSE),
            Ensure.that(mediationKeylistUpdateResponse.updated.size).isGreaterThan(0),
            Ensure.that(mediationKeylistUpdateResponse.updated[0].result).isEqualTo(TestConstants.MEDIATOR_COORDINATION_ACTION_RESULT_NO_CHANGE),
            Ensure.that(mediationKeylistUpdateResponse.updated[0].action).isEqualTo(TestConstants.MEDIATOR_COORDINATION_ACTION_REMOVE)
        )
    }
}
