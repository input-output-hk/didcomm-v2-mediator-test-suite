package interactions

import abilities.CommunicateViaDidcomm
import common.TestConstants
import io.restassured.RestAssured
import io.restassured.builder.RequestSpecBuilder
import io.restassured.config.EncoderConfig
import net.serenitybdd.core.Serenity
import net.serenitybdd.screenplay.Actor
import net.serenitybdd.screenplay.Interaction
import net.serenitybdd.screenplay.rest.interactions.Post
import org.didcommx.didcomm.message.Message

open class SendDidcommMessage(
    private val message: Message,
    private val contentType: String = TestConstants.DIDCOMM_V2_CONTENT_TYPE_ENCRYPTED
): Interaction {
    override fun <T : Actor> performAs(actor: T) {
        Serenity.recordReportData().withTitle("DIDComm Message").andContents(message.toString())
        // We have to rewrite spec to remove all unnecessary hardcoded headers
        // from standard serenity rest interaction
        val spec = RequestSpecBuilder().noContentType()
            .setContentType(contentType)
            .setConfig(
                RestAssured.config()
                .encoderConfig(
                    EncoderConfig
                        .encoderConfig()
                        .appendDefaultContentCharsetToContentTypeIfUndefined(false))
            )
            .setBody(actor.usingAbilityTo(CommunicateViaDidcomm::class.java).packMessage(message))
            .build()
        Post.to("/").with {
            it.spec(spec)
        }.performAs(actor)
    }
}
