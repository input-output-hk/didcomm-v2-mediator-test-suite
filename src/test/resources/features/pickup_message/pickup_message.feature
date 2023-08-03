Feature: Pickup message protocol

  A protocol to facilitate an agent picking up messages held at a mediator.
  Protocol description: https://didcomm.org/pickup/3.0/

  Not tested features of the protocol:
  * Multiple recipients - not supported
  * Live mode - not supported

  Background: Recipient and Mediator successfully set up mediation
    Given Recipient sends a mediate request message to the mediator
    And Mediator responds to Recipient with mediate grant message

  Scenario: Recipient sends a status-request message with one forward message available

  Recipient asks mediator if any forward messages are available for him,
  Mediator should answer that there is a message available for delivery.

    Given Sender sent a forward message to Recipient
    When Recipient sends a status-request message
    Then Mediator responds with a status message with 1 queued messages of Recipient

  Scenario: Recipient sends a status-request message with no forward message available

  Recipient asks mediator if any forward messages are available for him,
  Mediator should answer that there are no messages available for delivery.

    When Recipient sends a status-request message
    Then Mediator responds with a status message with 0 queued messages of Recipient

  Scenario: Recipient sends a delivery-request message with one forward message available

  Recipient is trying to get one forward message from Mediator,
  Mediator should deliver the message.

    Given Sender sent a forward message to Recipient
    When Recipient sends a delivery-request message
    Then Mediator delivers message of Sender to Recipient

  Scenario: Recipient sends a delivery-request message with no available forward messages

  Recipient is trying to request delivery when no messages are available,
  Mediator should respond with a delivery response with no attachments.

    When Recipient sends a delivery-request message
    Then Mediator responds that there are no messages from Sender to Recipient

  Scenario: Recipient sends a delivery-request and confirms that messages were received

  Recipient acknowledges and confirms that they received the message
  using `messages-received` message after which mediator should clear its messages queue for Recipient
  and respond with `status` message that should not contain any messages anymore.

    Given Sender sent a forward message to Recipient
    And Recipient sends a delivery-request message
    And Mediator delivers message of Sender to Recipient
    When Recipient sends a messages-received message
    When Recipient sends a status-request message
    Then Mediator responds with a status message with 0 queued messages of Recipient
#
#  Scenario Outline: Recipient limits number of messages for delivery
#
#  Recipient has multiple messages for delivery,
#  when they request them, they limit a number of messages,
#  and mediator should return only the number of messages restricted by limit.
#
#    Given Sender sent <amount> of messages to mediator
#    When Recipient requests <limit> messages for delivery
#    Then Mediator delivers less or equal <limit> of messages of Sender to Recipient
#    Examples:
#      | amount | limit |
#      | 4      | 2     |
#      | 2      | 4     |
#      | 3      | 3     |
#      | 2      | 0     |
