Feature: Trust Ping Protocol

  The trust-ping protocol defined in the DIDComm Messaging Spec.
  This enables the sender and recipient to engage in an exchange of trust pings.
  Protocol description: https://didcomm.org/trust-ping/2.0/

  Scenario: Recipient sends and receives trusted ping message with return_route "all"
    When Recipient sends trusted ping message to mediator with return_route "all"
    Then Recipient receives trusted ping message back synchronously

  Scenario: Recipient sends and receives trusted ping message with return_route "none"
    When Recipient sends trusted ping message to mediator with return_route "none"
    Then Recipient receives trusted ping message back asynchronously
