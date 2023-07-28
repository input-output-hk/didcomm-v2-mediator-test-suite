package abilities

import common.Environments.MEDIATOR_PEER_DID
import models.AgentPeerService
import models.DIDDocResolverPeerDID
import net.serenitybdd.core.Serenity
import net.serenitybdd.rest.SerenityRest
import net.serenitybdd.screenplay.Ability
import net.serenitybdd.screenplay.Actor
import org.didcommx.didcomm.DIDComm
import org.didcommx.didcomm.message.Message
import org.didcommx.didcomm.model.PackEncryptedParams
import org.didcommx.didcomm.model.UnpackParams
import org.didcommx.didcomm.protocols.routing.Routing
import org.didcommx.didcomm.protocols.routing.WrapInForwardResult
import org.didcommx.didcomm.utils.fromJsonToMap

open class CommunicateViaDidcomm(serviceEndpoint: String): Ability {

    private val didDocResolver = DIDDocResolverPeerDID()
    private val peerDidService = AgentPeerService.makeAgent(serviceEndpoint = serviceEndpoint)
    private val didComm = DIDComm(didDocResolver, peerDidService.getSecretResolverInMemory())
    val routing = Routing(didDocResolver, peerDidService.getSecretResolverInMemory())

    companion object {
        fun at(serviceEndpoint: String = ""): CommunicateViaDidcomm {
            return CommunicateViaDidcomm(serviceEndpoint)
        }

        fun `as`(actor: Actor): CommunicateViaDidcomm {
            return actor.abilityTo(CommunicateViaDidcomm::class.java)
        }
    }

    fun getDid(): String = peerDidService.did

    fun createNewPeerDid(): String = AgentPeerService.makePeerDid().did

    fun packMessage(message: Message, to: String = MEDIATOR_PEER_DID): String =
        didComm.packEncrypted(
            PackEncryptedParams.builder(message, to)
                .from(message.from!!)
                .forward(false).protectSenderId(false)
                .build()
        ).packedMessage

    fun wrapInForward(message: Message, to: String, routingKeys: List<String>): WrapInForwardResult {
        val packedMessage = packMessage(message, to)
        return routing.wrapInForward(fromJsonToMap(packedMessage), to, routingKeys = routingKeys)!!
    }
    fun unpackMessage(message: String): Message = didComm.unpack(
        UnpackParams(
            packedMessage = message,
            didDocResolver = didDocResolver,
            secretResolver = peerDidService.getSecretResolverInMemory(),
            expectDecryptByAllKeys = false,
            unwrapReWrappingForward = false
        )
    ).message

    fun unpackLastDidcommMessage(): Message {
        val didcommMessage = unpackMessage(SerenityRest.lastResponse().asString())
        Serenity.recordReportData().withTitle("DIDComm Response").andContents(didcommMessage.toString())
        return didcommMessage
    }

}
