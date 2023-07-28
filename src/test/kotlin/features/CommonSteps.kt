package features

import abilities.CommunicateViaDidcomm
import abilities.ListenToHttpMessages
import common.Environments
import io.cucumber.java.After
import io.cucumber.java.Before
import io.cucumber.java.ParameterType
import net.serenitybdd.screenplay.Actor
import net.serenitybdd.screenplay.actors.Cast
import net.serenitybdd.screenplay.actors.OnStage
import net.serenitybdd.screenplay.rest.abilities.CallAnApi

class CommonSteps {

    @Before
    fun setStage() {
        val cast = Cast()

        cast.actorNamed(
            "Recipient",
            CallAnApi.at(Environments.MEDIATOR_URL),
            ListenToHttpMessages.at(Environments.RECIPIENT_LISTENER_HOST, Environments.RECIPIENT_LISTENER_PORT),
            CommunicateViaDidcomm.at("http://${Environments.RECIPIENT_LISTENER_HOST}:${Environments.RECIPIENT_LISTENER_PORT}/")
        )
        cast.actorNamed(
            "Sender",
            CallAnApi.at(Environments.MEDIATOR_URL),
            CommunicateViaDidcomm.at("http://some.test.host:1234/")
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
