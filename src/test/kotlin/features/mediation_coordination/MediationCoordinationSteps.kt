package features.mediation_coordination

import abilities.CommunicateViaDidcomm
import common.*
import common.Environments.MEDIATOR_PEER_DID
import interactions.SendDidcommMessage
import io.cucumber.java.en.When
import net.serenitybdd.screenplay.Actor
import io.cucumber.java.en.Then
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import models.*
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
        ).from(recipient.recall("peerDid"))
            .to(listOf(MEDIATOR_PEER_DID)).build()

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
            Ensure.that(didcommResponse.from!!).isEqualTo(MEDIATOR_PEER_DID),
            Ensure.that(didcommResponse.to!!.first()).isEqualTo(recipient.recall("peerDid")),
            Ensure.that(mediationResponse.routing_did).isEqualTo(MEDIATOR_PEER_DID)
        )
    }

    @Then("Mediator responds to {actor} with mediate deny message")
    fun mediatorRespondsToHimWithMediateDenyMessage(recipient: Actor) {
        val didcommResponse = recipient.usingAbilityTo(CommunicateViaDidcomm::class.java).unpackLastDidcommMessage()
        val mediationResponse = Json.decodeFromString<MediationResponse>(didcommResponse.body.toJSONString())
        recipient.attemptsTo(
            Ensure.that(didcommResponse.type).isEqualTo(DidcommMessageTypes.MEDIATE_DENY),
            Ensure.that(didcommResponse.from!!).isEqualTo(MEDIATOR_PEER_DID),
            Ensure.that(didcommResponse.to!!.first()).isEqualTo(recipient.recall("peerDid")),
            Ensure.that(mediationResponse.routing_did).isEqualTo(MEDIATOR_PEER_DID)
        )
    }

    @When("{actor} sends a keylist update message to the mediator with a new peer did")
    fun recipientSendsAKeylistUpdateMessageToTheMediatorWithANewPeerDid(recipient: Actor) {
        val newPeerDid =  recipient.usingAbilityTo(CommunicateViaDidcomm::class.java).createNewPeerDid()
        recipient.attemptsTo(
            Ensure.that(newPeerDid).isNotEqualTo(recipient.recall("peerDid"))
        )
        recipient.remember("newPeerDid", newPeerDid)
        val messageKeylistUpdate = Message.builder(
            id = idGeneratorDefault(),
            body = mapOf(
                "updates" to listOf(
                    mapOf(
                        "action" to TestConstants.MEDIATOR_COORDINATION_ACTION_ADD,
                        "recipient_did" to newPeerDid,
                    )
                )
            ),
            type = DidcommMessageTypes.MEDIATE_KEYLIST_UPDATE
        ).from(recipient.recall("peerDid"))
            .to(listOf(MEDIATOR_PEER_DID)).build()
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
            Ensure.that(mediationKeylistUpdateResponse.updated[0].recipient_did).isEqualTo(recipient.recall("newPeerDid")),
        )
    }

    @When("{actor} sends a keylist query message to the mediator")
    fun recipientSendsAKeylistQueryMessageToTheMediator(recipient: Actor) {
        val messageKeylistQuery = Message.builder(
            id = idGeneratorDefault(),
            body = mapOf(
                "paginate" to
                    mapOf(
                        "limit" to 1,
                        "offset" to 0,
                    )
            ),
            type = DidcommMessageTypes.MEDIATE_KEYLIST_QUERY
        ).from(recipient.recall("peerDid"))
            .to(listOf(MEDIATOR_PEER_DID)).build()
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
            Ensure.that(mediationKeylistResponse.keys.last().recipient_did).isEqualTo(recipient.recall("peerDid"))
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
                        "recipient_did" to recipient.recall("peerDid"),
                    )
                )
            ),
            type = DidcommMessageTypes.MEDIATE_KEYLIST_UPDATE
        ).from(recipient.recall("peerDid"))
            .to(listOf(MEDIATOR_PEER_DID)).build()
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
                        "recipient_did" to recipient.recall("newPeerDid"),
                    )
                )
            ),
            type = DidcommMessageTypes.MEDIATE_KEYLIST_UPDATE
        ).from(recipient.recall("peerDid"))
            .to(listOf(MEDIATOR_PEER_DID)).build()
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
            Ensure.that(mediationKeylistUpdateResponse.updated[0].action).isEqualTo(TestConstants.MEDIATOR_COORDINATION_ACTION_REMOVE),
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
                        "recipient_did" to TestConstants.EXAMPLE_DID,
                    )
                )
            ),
            type = DidcommMessageTypes.MEDIATE_KEYLIST_UPDATE
        ).from(recipient.recall("peerDid"))
            .to(listOf(MEDIATOR_PEER_DID)).build()
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
            Ensure.that(mediationKeylistUpdateResponse.updated[0].action).isEqualTo(TestConstants.MEDIATOR_COORDINATION_ACTION_REMOVE),
        )
    }
}
