Feature: Fitnote declaration

    @FitnoteDeclarationTest
    Scenario: Submit an accepted declaration with all mandatory session data present
      Given the http client is up
      And I create a rabbit exchange named "rabbit.fitnote.ex"
      And I create a NON-DURABLE catch all subscription for queue name "test.queue.1" binding to exchange "rabbit.fitnote.ex"
      When I hit the service url "http://localhost:9101/photo" with the following json body
        | sessionId         | "11"                 |
        | image             | /OcrTest.jpg        |
      Then I receive a HTTP response of 202
      And I hit the service url "http://localhost:9101/imagestatus" with session id "11" getting return status 200 and finally containing the following json body
        | fitnoteStatus | SUCCEEDED |
      When I hit the service url "http://localhost:9101/nino" with the following json body
        | sessionId         | "11"                 |
        | nino              | "AA370773"          |
      Then I receive a HTTP response of 200
      When I hit the service url "http://localhost:9101/address" with the following json body
        | sessionId         | "11"              |
        | houseNameOrNumber | "254"             |
        | postcode          | "NE12 9PG"        |
      Then I receive a HTTP response of 200
      When I hit the service url "http://localhost:9101/declaration" with the following json body
      | sessionId      | "11" |
      | accepted       | true |
      Then I receive a HTTP response of 200
      And a message is successfully consumed from the queue and there are 0 pending message left on queue "test.queue.1"
      And the message has a correlation id and a valid ImagePayload with the following NINO, House number, Postcode and matching SessionId
        | nino              | "AA370773"          |
        | houseNameOrNumber | "254"                |
        | postcode          | "NE12 9PG"           |
        | sessionId         | "11"                 |

  @FitnoteDeclarationTest
  Scenario: Submit a declined declaration with all mandatory session data present
    Given the http client is up
    And I create a rabbit exchange named "rabbit.fitnote.ex"
    And I create a NON-DURABLE catch all subscription for queue name "test.queue.2" binding to exchange "rabbit.fitnote.ex"
    When I hit the service url "http://localhost:9101/photo" with the following json body
      | sessionId         | "12"                 |
      | image             | /OcrTest.jpg        |
    Then I receive a HTTP response of 202
    When I hit the service url "http://localhost:9101/nino" with the following json body
      | sessionId         | "12"                 |
      | nino              | "AA370773"          |
    Then I receive a HTTP response of 200
    When I hit the service url "http://localhost:9101/declaration" with the following json body
      | sessionId      | "12"   |
      | accepted       | false  |
    Then I receive a HTTP response of 400
    And there are no pending messages on queue "test.queue.2"

  @FitnoteDeclarationTest
  Scenario: Submit an accepted declaration with session missing fitnote image
    Given the http client is up
    And I create a rabbit exchange named "rabbit.fitnote.ex"
    And I create a NON-DURABLE catch all subscription for queue name "test.queue.3" binding to exchange "rabbit.fitnote.ex"
    When I hit the service url "http://localhost:9101/nino" with the following json body
      | sessionId         | "13"                 |
      | nino              | "AA370773"          |
    Then I receive a HTTP response of 200
    When I hit the service url "http://localhost:9101/declaration" with the following json body
      | sessionId      | "13"   |
      | accepted       | true   |
    Then I receive a HTTP response of 400
    And there are no pending messages on queue "test.queue.3"

  @FitnoteDeclarationTest
  Scenario: Submit an accepted declaration with session missing an address
    Given the http client is up
    And I create a rabbit exchange named "rabbit.fitnote.ex"
    And I create a NON-DURABLE catch all subscription for queue name "test.queue.4" binding to exchange "rabbit.fitnote.ex"
    When I hit the service url "http://localhost:9101/photo" with the following json body
      | sessionId         | "14"                 |
      | image             | /OcrTest.jpg        |
    Then I receive a HTTP response of 202
    When I hit the service url "http://localhost:9101/nino" with the following json body
      | sessionId         | "14"                 |
      | nino              | "AA370773"          |
    Then I receive a HTTP response of 200
    When I hit the service url "http://localhost:9101/declaration" with the following json body
      | sessionId      | "14"   |
      | accepted       | true   |
    Then I receive a HTTP response of 400
    And there are no pending messages on queue "test.queue.4"

  @FitnoteDeclarationTest
  Scenario: Submit an accepted declaration with session missing NINO
    Given the http client is up
    And I create a rabbit exchange named "rabbit.fitnote.ex"
    And I create a NON-DURABLE catch all subscription for queue name "test.queue.5" binding to exchange "rabbit.fitnote.ex"
    When I hit the service url "http://localhost:9101/photo" with the following json body
      | sessionId         | "16"                 |
      | image             | /OcrTest.jpg        |
    Then I receive a HTTP response of 202
    When I hit the service url "http://localhost:9101/declaration" with the following json body
      | sessionId      | "16"   |
      | accepted       | true  |
    Then I receive a HTTP response of 400
    And there are no pending messages on queue "test.queue.5"

  @FitnoteDeclarationTest
  Scenario: Submit invalid json as declaration
    Given the http client is up
    And I create a rabbit exchange named "rabbit.fitnote.ex"
    And I create a NON-DURABLE catch all subscription for queue name "test.queue.6" binding to exchange "rabbit.fitnote.ex"
    When I hit the service url "http://localhost:9101/photo" with the following json body
      | sessionId         | "17"                 |
      | image             | /OcrTest.jpg        |
    Then I receive a HTTP response of 202
    When I hit the service url "http://localhost:9101/nino" with the following json body
      | sessionId         | "17"                 |
      | nino              | "AA370773"          |
    Then I receive a HTTP response of 200
    When I hit the service url "http://localhost:9101/declaration" with the following json body
      |||
    Then I receive a HTTP response of 400
    And there are no pending messages on queue "test.queue.6"
    And I hit the service url "http://localhost:9101/imagestatus" with session id "17" getting return status 200 and finally containing the following json body
      | fitnoteStatus | SUCCEEDED |

  @FitnoteDeclarationTest
  Scenario: Submit multiple accepted declarations with all mandatory session data present
    Given the http client is up
    And I create a rabbit exchange named "rabbit.fitnote.ex"
    And I create a NON-DURABLE catch all subscription for queue name "test.queue.7" binding to exchange "rabbit.fitnote.ex"
    When I hit the service url "http://localhost:9101/photo" with the following json body
      | sessionId         | "22"                 |
      | image             | /OcrTest.jpg        |
    Then I receive a HTTP response of 202
    When I hit the service url "http://localhost:9101/photo" with the following json body
      | sessionId         | "23"                 |
      | image             | /OcrTest.jpg        |
    Then I receive a HTTP response of 202
    When I hit the service url "http://localhost:9101/nino" with the following json body
      | sessionId         | "22"                 |
      | nino              | "AA370773"          |
    Then I receive a HTTP response of 200
    When I hit the service url "http://localhost:9101/nino" with the following json body
      | sessionId         | "23"                 |
      | nino              | "AA370773"          |
    Then I receive a HTTP response of 200
    When I hit the service url "http://localhost:9101/address" with the following json body
      | sessionId         | "22"              |
      | houseNameOrNumber | "254"             |
      | postcode          | "NE12 9PG"        |
    Then I receive a HTTP response of 200
    When I hit the service url "http://localhost:9101/address" with the following json body
      | sessionId         | "23"              |
      | houseNameOrNumber | "88"             |
      | postcode          | "LS6 9PG"        |
    Then I receive a HTTP response of 200
    And I hit the service url "http://localhost:9101/imagestatus" with session id "23" getting return status 200 and finally containing the following json body
      | fitnoteStatus | SUCCEEDED |
    When I hit the service url "http://localhost:9101/declaration" with the following json body
      | sessionId      | "23" |
      | accepted       | true |
    Then I receive a HTTP response of 200
    And I hit the service url "http://localhost:9101/imagestatus" with session id "22" getting return status 200 and finally containing the following json body
      | fitnoteStatus | SUCCEEDED |
    When I hit the service url "http://localhost:9101/declaration" with the following json body
      | sessionId      | "22" |
      | accepted       | true |
    Then I receive a HTTP response of 200
    And a message is successfully consumed from the queue and there are 1 pending message left on queue "test.queue.7"
    And the message has a correlation id and a valid ImagePayload with the following NINO, House number, Postcode and matching SessionId
      | nino              | "AA370773"          |
      | houseNameOrNumber | "88"                |
      | postcode          | "LS6 9PG"           |
      | sessionId         | "23"                 |
    And a message is successfully consumed from the queue and there are 0 pending message left on queue "test.queue.7"
    And the message has a correlation id and a valid ImagePayload with the following NINO, House number, Postcode and matching SessionId
      | nino              | "AA370773"          |
      | houseNameOrNumber | "254"                |
      | postcode          | "NE12 9PG"           |
      | sessionId         | "22"                 |
    And there are no pending messages on queue "test.queue.7"