Feature: Trust Ping Protocol

  The trust-ping protocol defined in the DIDComm Messaging Spec.
  This enables the sender and recipient to engage in an exchange of trust pings.
  Protocol description: https://didcomm.org/trust-ping/2.0/

  Scenario: Recipient sends and receives trusted ping message
    When Recipient sends trusted ping message to mediator
    Then Recipient receives trusted ping message back
