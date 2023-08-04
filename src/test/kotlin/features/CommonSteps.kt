package features

import abilities.CommunicateViaDidcomm
import abilities.ListenToHttpMessages
import com.sksamuel.hoplite.ConfigLoader
import config.Env
import io.cucumber.java.After
import io.cucumber.java.Before
import io.cucumber.java.ParameterType
import net.serenitybdd.rest.SerenityRest
import net.serenitybdd.screenplay.Actor
import net.serenitybdd.screenplay.actors.Cast
import net.serenitybdd.screenplay.actors.OnStage
import net.serenitybdd.screenplay.rest.abilities.CallAnApi

class CommonSteps {

    @Before
    fun setStage() {
        val env = ConfigLoader().loadConfigOrThrow<Env>("/mediator.conf")

        if (env.mediator.did == null) {
            SerenityRest.rest().get("${env.mediator.url}/invitation")
            env.mediator.did = SerenityRest.lastResponse().jsonPath().getString("from")
        }

        val cast = Cast()
        cast.actorNamed(
            "Recipient",
            CallAnApi.at(env.mediator.url),
            ListenToHttpMessages.at(env.recipient.host, env.recipient.port),
            CommunicateViaDidcomm.at(
                env.mediator.did!!,
                "http://${env.recipient.host}:${env.recipient.port}/"
            )
        )
        cast.actorNamed(
            "Sender",
            CallAnApi.at(env.mediator.url),
            CommunicateViaDidcomm.at(env.mediator.did!!)
        )
        cast.actors.forEach { actor ->
            actor.remember("peerDid", actor.usingAbilityTo(CommunicateViaDidcomm::class.java).getDid())
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
