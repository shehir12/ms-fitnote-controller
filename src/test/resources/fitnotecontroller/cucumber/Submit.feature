Feature: Fitnote submit

  @FitnoteSubmitTest
  Scenario: Submit readable barcode
    Given the controller is up
    When I hit the service url "http://localhost:9100/qrcode" with the following json body
      | barcodeImage   | /working_barcode.jpg |
      | sessionId      | 1                    |
    Then I receive a HTTP response of 202
    And I hit the service url "http://localhost:9100/imagestatus" with session id "1" getting return status 200 and finally containing the following json body
      | barcodeStatus | SUCCEEDED |

  @FitnoteSubmitTest
  Scenario: Submit unreadable barcode
      Given the controller is up
      When I hit the service url "http://localhost:9100/qrcode" with the following json body
        |barcodeImage | /EmptyPage.jpg|
        |sessionId    | 2                 |
      Then I receive a HTTP response of 202
      And I hit the service url "http://localhost:9100/imagestatus" with session id "2" getting return status 200 and finally containing the following json body
        | barcodeStatus | FAILED_IMG_BARCODE |

  @FitnoteSubmitTest
  Scenario: Submit readable fitnote
      Given the controller is up
      When I hit the service url "http://localhost:9100/qrcode" with the following json body
        |barcodeImage | /working_barcode.jpg|
        |sessionId    | "3"                 |
      Then I receive a HTTP response of 202
      When I hit the service url "http://localhost:9100/photo" with the following json body
        | image     | /OcrTest.jpg |
        | sessionId | "3"             |
      Then I receive a HTTP response of 202
      And I hit the service url "http://localhost:9100/imagestatus" with session id "3" getting return status 200 and finally containing the following json body
        | fitnoteStatus | SUCCEEDED |

  @FitnoteSubmitTest
  Scenario: Submit partial fitnote
    Given the controller is up
    When I hit the service url "http://localhost:9100/qrcode" with the following json body
      |barcodeImage | /working_barcode.jpg|
      |sessionId    | "3"                 |
    Then I receive a HTTP response of 202
    When I hit the service url "http://localhost:9100/photo" with the following json body
      | image     | /OcrTest_LHS.jpg |
      | sessionId | "3"             |
    Then I receive a HTTP response of 202
    And I hit the service url "http://localhost:9100/imagestatus" with session id "3" getting return status 200 and finally containing the following json body
      | fitnoteStatus | FAILED_IMG_OCR_PARTIAL |

  @FitnoteSubmitTest
  Scenario: Submit unreadable fitnote
      Given the controller is up
      When I hit the service url "http://localhost:9100/qrcode" with the following json body
        |barcodeImage | /working_barcode.jpg|
        |sessionId    | "5"                   |
      Then I receive a HTTP response of 202
      When I hit the service url "http://localhost:9100/photo" with the following json body
        | image     | /OcrTest_RHS.jpg |
        | sessionId | "5"                   |
      Then I receive a HTTP response of 202
      And I hit the service url "http://localhost:9100/imagestatus" with session id "5" getting return status 200 and finally containing the following json body
        | fitnoteStatus | FAILED_IMG_OCR |

  @FitnoteSubmitTest
  Scenario: Submit readable barcode with an empty session Id
      Given the controller is up
      When I hit the service url "http://localhost:9100/qrcode" with the following json body
        | barcodeImage   | /working_barcode.jpg |
        | sessionId      |  ""                    |
      Then I receive a HTTP response of 400

  @FitnoteSubmitTest
  Scenario: Submit readable Fitnote with an empty session Id
      Given the controller is up
      When I hit the service url "http://localhost:9100/qrcode" with the following json body
        |barcodeImage | /working_barcode.jpg|
        |sessionId    | "6"                 |
      Then I receive a HTTP response of 202
      When I hit the service url "http://localhost:9100/photo" with the following json body
        | image     | /OcrTest.jpg |
        | sessionId | ""            |
      Then I receive a HTTP response of 400

  @FitnoteSubmitTest
  Scenario: Submit a pdf scanned fitnote is a SUCCESS at 300dpi colour
    Given the controller is up
    When I hit the service url "http://localhost:9100/photo" with the following json body
      | image     | /FullPage_Portrait.pdf |
      | sessionId | "46"             |
    Then I receive a HTTP response of 202
    And I hit the service url "http://localhost:9100/imagestatus" with session id "46" getting return status 200 and finally containing the following json body
      | fitnoteStatus | SUCCEEDED |

  @FitnoteSubmitTest
  Scenario: Submit small fitnote
    Given the controller is up
    When I hit the service url "http://localhost:9100/photo" with the following json body
      | image     | /Pixi3.jpg |
      | sessionId | "8"             |
    Then I receive a HTTP response of 202
    And I hit the service url "http://localhost:9100/imagestatus" with session id "8" getting return status 200 and finally containing the following json body
      | fitnoteStatus | FAILED_IMG_SIZE |

  @FitnoteSubmitTest
  Scenario: Submit invalid json as Barcode Image
      Given the controller is up
      When I hit the service url "http://localhost:9100/qrcode" with the following json body
        |||
      Then I receive a HTTP response of 400

  @FitnoteSubmitTest
  Scenario: Submit invalid json as Fitnote Image
      Given the controller is up
      When I hit the service url "http://localhost:9100/photo" with the following json body
        |||
      Then I receive a HTTP response of 400

  @FitnoteSubmitTest
  Scenario: Verify that the timeout functionality kicks in after 60 seconds
    Given the controller is up
    When I hit the service url "http://localhost:9100/photo" with the following json body
      | image     | /FullPage_Portrait.jpg |
      | sessionId | "11"             |
    Then I receive a HTTP response of 202
    And I hit the service url "http://localhost:9100/imagestatus" with session id "11" getting return status 200 and finally timing out trying to match the following body
      | fitnoteStatus | NEVER_GONNA_HAPPEN |