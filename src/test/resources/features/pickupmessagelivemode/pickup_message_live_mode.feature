Feature: Pickup message live mode protocol

  A protocol to facilitate an agent picking up messages held at a mediator.
  Protocol description: https://didcomm.org/pickup/3.0/

  Not tested features of the protocol:
  * Multiple recipients - not supported

  Background: Recipient and Mediator successfully set up mediation
    Given Recipient sends a mediate request message to the mediator
    And Mediator responds to Recipient with mediate grant message
    And Recipient sends a keylist update message to the mediator with a new peer did

  Scenario: Recipient sends a delivery-request message with one forward message available

  Recipient is trying to get one forward message from Mediator,
  Mediator should deliver the message.
    Given Recipient sends a live delivery change message with live_delivery to true
    And Sender sent a forward message in live mode to Recipient
    When Recipient sends a delivery-request message in live mode over websocket
    Then Mediator delivers message in live mode of Sender to Recipient

  Scenario: Recipient sends a delivery-request message with no available forward messages

  Recipient is trying to request delivery when no messages are available,
  Mediator should respond with a delivery response with no attachments.
    Given Recipient sends a live delivery change message with live_delivery to true
    When Recipient sends a delivery-request message in live mode over websocket
    Then Mediator responds with a status message with 0 queued messages in live mode of Recipient


  Scenario: Recipient sends a delivery-request and confirms that messages were received

  Recipient acknowledges and confirms that they received the message
  using `messages-received` message after which mediator should clear its messages queue for Recipient
  and respond with `status` message that should not contain any messages anymore.

    Given Recipient sends a live delivery change message with live_delivery to true
    Given Sender sent a forward message in live mode to Recipient
    And Recipient sends a delivery-request message in live mode over websocket
    And Mediator delivers message in live mode of Sender to Recipient
    When Recipient sends a messages-received message in live mode
    When Recipient sends a status-request in live mode message
    Then Mediator responds with a status message with 0 queued messages in live mode of Recipient


