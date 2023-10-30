Feature: Report Problem Protocol

  Scenario: Sending the same message twice
    When Recipient sends the same message twice
    Then Recipient gets report problem message with reply problem code

  Scenario: Sending a message with unsupported protocol
    When Recipient sends a message with unsupported protocol
    Then Recipient gets report problem message with unsupported protocol problem code

  Scenario: Sending a message with unsupported version of protocol
    When Recipient sends a message with unsupported version of protocol
    Then Recipient gets report problem message with unsupported protocol problem code

  Scenario: Receive pickup message with not enrolled peer did
    When Recipient sends a delivery-request message with another not enrolled peer did
    Then Recipient gets report problem message with not enrolled message problem code

  Scenario: Send forward message to not enrolled peer did
    When Recipient sends forward message to a DID that is not enrolled
    Then Recipient gets signed report problem message with not enrolled message problem code

  # Fails for now, error 400 and empty body
#  Scenario: Sending tampered message
#    When Recipient sends a message with tampered payload
#    Then Recipient gets report problem message with crypto error problem code

  # Fails for now, error 400 and empty body
#  Scenario: Sending malformed message
#    When Recipient sends a plaintext message with malformed payload
#    Then Recipient gets report problem message with malformed payload problem code
