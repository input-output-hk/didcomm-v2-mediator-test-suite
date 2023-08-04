Feature: Mediator Coordination protocol

  A protocol to coordinate mediation configuration between a mediating agent and the recipient.
  Protocol description: https://didcomm.org/mediator-coordination/2.0/

  Background: Recipient successfully set up a connection with the mediator
    When Recipient sends a mediate request message to the mediator
    Then Mediator responds to Recipient with mediate grant message

#  Scenario: Recipient asks for mediation twice in a row and second request is denied
#    When Recipient sends a mediate request message to the mediator
#    Then Mediator responds to Recipient with mediate deny message

  Scenario: Recipient adds new key to keylist
    When Recipient sends a keylist update message to the mediator with a new peer did
    Then Mediator responds to Recipient with a correct keylist update add message

  Scenario: Recipient removes alias from keylist
    When Recipient sends a keylist update message to the mediator with a new peer did
    And Recipient sends a keylist update message to the mediator to remove added alias
    Then Mediator responds to Recipient with a correct keylist update remove message

  Scenario: Recipient removes not existing alias
    When Recipient sends a keylist update message to the mediator to remove not existing alias
    Then Mediator responds to Recipient with a message with no_change status

  Scenario: Recipient removes the last alias from keylist
    When Recipient sends a keylist update message to the mediator to remove the last alias
    Then Mediator responds to Recipient with a correct keylist update remove message

  Scenario: Recipient query keylist
    When Recipient sends a keylist query message to the mediator
    Then Mediator responds to Recipient with keylist message containing the current list of keys
