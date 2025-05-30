package simulations

import io.gatling.javaapi.core.*
import io.gatling.javaapi.core.CoreDsl.*
import io.gatling.javaapi.http.HttpDsl.*
import models.Mediator
import models.Recipient
import java.time.Duration

class TestSimulation : Simulation() {

    val httpProtocol = http
        .baseUrl("http://localhost:8080")
        .acceptHeader("application/json")

    val mediator = Mediator()
    val recipient = Recipient(mediator.did, mediator.serviceUrl)

    val scenario = scenario("Bombard mediator with messages")
        .exec(http("send message").get("/computers"))

    init {
        setUp(
            scenario.injectOpen(
                nothingFor(Duration.ofSeconds(4)), // Pause for a few seconds before starting
                atOnceUsers(10), // Inject 10 users at once
                rampUsers(10).during(Duration.ofSeconds(5)), // Inject 10 users over 5 seconds
                constantUsersPerSec(20.0).during(Duration.ofSeconds(15)), // Inject 20 users/sec for 15 sec
                constantUsersPerSec(20.0).during(Duration.ofSeconds(15)).randomized(), // Same, but randomized
                rampUsersPerSec(10.0).to(20.0).during(Duration.ofSeconds(10)), // Ramp from 10 to 20 users/sec
                rampUsersPerSec(10.0).to(20.0).during(Duration.ofSeconds(10)).randomized(),
                stressPeakUsers(1000).during(Duration.ofSeconds(20)) // Stress test with a peak
            )
        )
    }
}
