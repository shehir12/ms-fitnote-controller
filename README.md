# Fitnote Controller
[![Build Status](https://travis-ci.org/dwp/fitnote-controller.svg?branch=master)](https://travis-ci.org/dwp/fitnote-controller) [![Known Vulnerabilities](https://snyk.io/test/github/dwp/fitnote-controller/badge.svg)](https://snyk.io/test/github/dwp/fitnote-controller)

Main service functionality for submitting, checking and adding additional claimant details to a submitted fit note.      

## Development

Clone repository and run `mvn clean package`

Starting the service -
 
    cd target; java -jar fitnote-controller-<version>.jar server path/to/config.yml

## Endpoints

each endpoint can be called in any order with the exception of `declaration` which will check all of the data for the session has been set.  If any entries are missing or invalid at `declaration` the software will return an error.

The first call to any of the endpoints for a new sessionId will implicitly create the entry in the ImageStorage class.  Each further call with the session id will update the current ImagePayload.

### `/photo`

**POST** request expecting JSON object of base 64 encoded image of the fitnote and a session id.

`{
  "image":"base64-encoded-string",
  "sessionId":"session1"
}`

Returns:-

* **202** :: Success.  Returns json session id `{"sessionId":"session1"}`
* **400** :: Bad or Malformed content.  Returns "Unable to process request" (error is logged)
* **500** :: Internal error occurred.  Returns "Unable to process request" (error is logged)
* **503** :: Service Unavailable.  Returns "Unable to process request" when not enough memory is available on controller to OCR the image (errors are logged)

## `/imagestatus`

**GET** request, passing the session id (as json) to check the status of the image processing

`{
  "sessionId":"session1"
}`

Returns:-

* **200** :: Success.  Returns json status `{"fitnoteStatus":"<status>", "barcodeStatus":"<status>"}`
* **400** :: Missing session-id in json (error is logged)

currently the list of statuses are

* `CREATED`
* `UPLOADED`
* `CHECKING`
* `FAILED_IMG_SIZE`
* `PASS_IMG_SIZE`
* `FAILED_IMG_OCR`
* `FAILED_IMG_OCR_PARTIAL`
* `PASS_IMG_OCR`
* `FAILED_IMG_BARCODE`
* `PASS_IMG_BARCODE`
* `SUCCEEDED`
* `FAILED_ERROR`

## `/address`

**POST** request with the claimant's address

`{
  "sessionId":"session1",
  "houseNameOrNumber":"Shangri La",
  "street":"The Street",
  "city":"Leeds",
  "postcode":"LS1 1AB"
}`

Returns:-

* **200** :: Success (no return body)
* **400** :: Bad or Malformed content; `houseNameOrNumber` & `postcode` are mandatory.  Returns "Unable to process request" (error is logged)
* **500** :: Internal error occurred.  Returns "Unable to process request" (error is logged)

## `/nino`

**POST** request to specify the mandatory NINO

`{
  "sessionId":"123456",
  "nino":"AA370773"
}`

Returns:-

* **200** :: Success.  Returns json session id `{"sessionId":"session1"}`
* **400** :: Bad or Malformed content.  Returns "Unable to process request" (error is logged)
* **500** :: Internal error occurred.  Returns "Unable to process request" (error is logged)

## `/mobile`

**POST** request to specify the optional MOBILE PHONE NUMBER

`{
  "sessionId":"123456",
  "mobileNumber":"07877654321"
}`

Returns:-

* **200** :: Success.  Returns json session id `{"sessionId":"session1"}`
* **400** :: Bad or Malformed content.  Returns "Unable to process request" (error is logged)
* **500** :: Internal error occurred.  Returns "Unable to process request" (error is logged)

## `/declaration`

**POST** request to complete the user journey with an accept.

`{
  "sessionId":"session1",
  "accepted":true
}`

* _true_ : declare ok, package, send to RabbitMQ and clear session
* _false_ : declare not ok, log, clear the session and return

Returns:-

* **200** :: Success.
* **400** :: Bad or Malformed content.  Returns what is missing (detailed error is logged)
* **500** :: Internal error occurred.  Returns "Unable to process request" (error is logged)


## Configuration Elements

* `sessionExpiryTimeInSeconds` : the length of time before (in seconds) before an inactive session is cleared from the cache
* `imageReplayExpirySeconds` : the length of time before the replay cache is cleared for a submitted image
* `maxAllowedImageReplay` : the threshold for the same image to be replayed to the service before an error is thrown and the submission rejected
* `imageHashSalt` : the 'salt' to use when creating the image hash for the replay cache
* `redisStoreURI`: the URI of the Redis instance (eg. redis://localhost:6379)
* `redisEncryptMessages`: whether to encrypt the ImagePayload before persisting to Redis
* `redisKmsCryptoConfiguration`: The `CryptoConfig` object for KMS interaction for redis activities (when `redisEncryptMessages is true)
* `rabbitMqURI` : The URI of the RabbitMq instance (eg. amqp://guest:guest@localhost:9105)
* `rabbitEventRoutingKey` : the routing key to use for RabbitMq published messages
* `rabbitExchangeName` : the RabbitMq exchange name
* `rabbitEncryptMessages`: whether to encrypt the RabbitMq messages
* `rabbitKmsCryptoConfiguration`: The `CryptoConfig` object for KMS interaction for rabbit activities
* `rabbitMqTruststoreFile` : The RabbitMq truststore for SSL connections
* `rabbitMqKeystoreFile` : The certificate keystore for SSL RabbitMq connections
* `rabbitMqTruststorePass` : the truststore password
* `rabbitMqKeystorePass` : the keystore password
* `ocrChecksEnabled` : is OCR enabled, defaults to **true**
* `forceLandscapeImageSubmission` : force the submissions of landscape images, defaults to **true**
* `tesseractFolderPath` : the tesseract configuration file path
* `easPostCodeLookUpUrl` : the full url of the postcode service lookup
* `sslTruststoreFilenameESA` : the trust store for the postcode service cert
* `sslTruststorePasswordESA` : the postcode service trust store password
* `esaDefaultRoutingForDRS` : the default routing code if esa postcode lookup fails
* `rejectingOversizeImages` : works in conjunction with `targetImageSizeKB` and will force a reject if it is not possible to compress the image to the target size.  Defaults to **true**
* `pdfScanDPI` : the DPI to use when the incoming document has been detected as PDF.  300 is the optimum (and default) value for this to generate a BufferedImage that is detailed enough to be read but small enough to be compressed to the target size.  >300 DPI will fail image compression.
* `targetImageSizeKB` : the target compressed image size in KB defaults to **500**
* `greyScale` : grey scale the image during compression, defaults to **true**
* `maxLogChars` : the maximum number of OCR characters to output to the log (in debug mode), defaults to **50**
* `targetBrightness` : the brightness to use to optimise the image for OCR, defaults to **179**
* `borderLossPercentage` : the amount of border to clip, defaults to **10**
* `scanTargetImageSize` : the compression image size to optimise for OCR scan, defaults to **1000**
* `highTarget` : The maximum percentage threshold for accepted image OCR
* `diagonalTarget` : the minimum success percentage on a diagonal corner of a 100% match, defaults to **20**
* `contrastCutOff` : the contrast to apply against the image for optimal OCR, defaults to **105**
* `topLeftText` (List\<String\>) : the text to look for in the top left corner
* `topRightText` (List\<String\>) : the text to look for in the top right corner
* `baseLeftText` (List\<String\>) : the text to look for in the bottom left corner
* `baseRightText` (List\<String\>) : the text to look for in the bottom right corner
* `estimatedRequestMemoryMb` : the expected request size (in megabytes) of a photo submissions, defaults to **3**.  if there is not enough free memory available to service the request it will be rejected
