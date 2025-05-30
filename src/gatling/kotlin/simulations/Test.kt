package simulations

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import models.DIDDocResolverPeerDID
import models.Mediator
import models.Recipient
import models.Sender

fun main() = runBlocking {
    val numberOfAgents = 50

    println("Starting $numberOfAgents concurrent DIDComm agent cycles...")

    val jobs = List(numberOfAgents) { agentIndex ->
        launch(Dispatchers.IO) {
            runDidCommCycleForAgent(agentIndex + 1) // Pass a unique ID to each agent's cycle
        }
    }
    jobs.forEach { it.join() }

    println("All $numberOfAgents DIDComm agent cycles have completed.")
}

suspend fun runDidCommCycleForAgent(agentId: Int) {
    println("[Agent $agentId] Starting communication cycle...")
    try {
        val mediator = Mediator()

        val didResolver = DIDDocResolverPeerDID()
        val mediatorDidDocResult = didResolver.resolve(mediator.did)
        if (mediatorDidDocResult.isEmpty) {
            println("[Agent $agentId] Failed to resolve mediator DID: ${mediator.did}")
            return
        }
        val mediatorDidDoc = mediatorDidDocResult.get()
        val mediatorUrl = mediatorDidDoc.didCommServices.firstOrNull()?.serviceEndpoint
        if (mediatorUrl == null) {
            println("[Agent $agentId] No DIDComm service endpoint found for mediator: ${mediator.did}")
            return
        }

        val recipient = Recipient(mediator.did, mediatorUrl)

        recipient.recipientSendsAMediateRequestMessageToTheMediator()
        recipient.recipientSendsAKeylistUpdateMessageToTheMediatorWithANewPeerDid()
//        recipient.recipientSendsALiveDeliveryChangeMessage()
        println("[Agent $agentId] Recipient setup complete.")

        val sender = Sender(mediator.did, mediatorUrl)
        println("[Agent $agentId] Sender configured for mediator ${mediator.did} at $mediatorUrl")

        var totalMessagesSentByAgent = 0
        for (i in 1..10) { // Outer loop for batches
            println("[Agent $agentId] Starting batch $i of 10...")
            for (j in 1..50) { // Inner loop for messages within a batch
                val messageContent = "Agent $agentId - Batch $i - Message $j"
                sender.senderSentAForwardMessageToRecipient(messageContent, recipient)
                totalMessagesSentByAgent++
            }
            println("[Agent $agentId] Batch $i: 50 messages sent. Total for agent: $totalMessagesSentByAgent")

            recipient.recipientSendsAStatusRequestMessage()
            val receivedMessages = recipient.recipientSendsADeliveryRequestMessage()
            recipient.recipientSendsAMessagesReceivedMessage(receivedMessages.thid, receivedMessages.idList ?: emptyList())
            println("[Agent $agentId] Batch $i: Messages picked up (${receivedMessages.idList.count()} received).")
        }
        println("[Agent $agentId] Cycle finished. Total messages sent: $totalMessagesSentByAgent.")

    } catch (e: Exception) {
        println("[Agent $agentId] !!! Error during DIDComm cycle: ${e.message} !!!")
        e.printStackTrace()
    }
}
