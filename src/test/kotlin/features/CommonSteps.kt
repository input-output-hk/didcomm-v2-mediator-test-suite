package features

import abilities.CommunicateViaDidcomm
import abilities.ListenToHttpMessages
import com.sksamuel.hoplite.ConfigLoader
import config.Env
import io.cucumber.java.After
import io.cucumber.java.Before
import io.cucumber.java.ParameterType
import models.DIDDocResolverPeerDID
import net.serenitybdd.core.Serenity
import net.serenitybdd.screenplay.Actor
import net.serenitybdd.screenplay.actors.Cast
import net.serenitybdd.screenplay.actors.OnStage
import net.serenitybdd.screenplay.rest.abilities.CallAnApi

class CommonSteps {

    @Before
    fun setStage() {
        val env = ConfigLoader().loadConfigOrThrow<Env>("/mediator.conf")
        val didResolver = DIDDocResolverPeerDID()
        val mediatorDidDoc = didResolver.resolve(env.mediator.did).get()
        val mediatorUrl = mediatorDidDoc.didCommServices.first().serviceEndpoint

        val cast = Cast()
        cast.actorNamed(
            "Recipient",
            CallAnApi.at(mediatorUrl),
            ListenToHttpMessages.at(env.recipient.host, env.recipient.port),
            CommunicateViaDidcomm.at(
                env.mediator.did,
                "http://${env.recipient.host}:${env.recipient.port}/"
            )
        )
        cast.actorNamed(
            "Sender",
            CallAnApi.at(mediatorUrl),
            CommunicateViaDidcomm.at(env.mediator.did)
        )
        cast.actors.forEach { actor ->
            val peerDid = actor.usingAbilityTo(CommunicateViaDidcomm::class.java).getDid()
            val secrets = actor.usingAbilityTo(CommunicateViaDidcomm::class.java).getSecretsJson()
            actor.remember("peerDid", peerDid)
            Serenity.recordReportData().withTitle("${actor.name}-did-and-secret-keys").andContents(secrets)
        }
        OnStage.setTheStage(cast)
    }

    @After
    fun clearStage() {
        OnStage.drawTheCurtain()
    }

    @ParameterType(".*")
    fun actor(actorName: String): Actor {
        return OnStage.theActorCalled(actorName)
    }
}
