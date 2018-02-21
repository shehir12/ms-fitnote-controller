# Memory Considerations

As part of the project review process a couple of issues were raised as potential weaknesses to the service through the injection of large images through the api endpoints and the internal image processing being memory intensive.

- persistent injection of large images could cause a java **OutOfMemory** situation
- the internal image processing and ocr could cause a java **OutOfMemroy** situation

We should guard against these situations to prevent the application from crashing (and taking with it all of the session information for all threads) and to prevent a scenario where the images checks are aborted or never completed.  The user journey should be protected at all time.

## Inject large image

The **dropwizard** framework prevents the application from accepted payload that cause a memory exception.  Test scenario:-

- 'Edit Configurations' on FitnoteControllerApplication and add the following to the VM Options:-
    - `-Xms10m -Xmx10m` (to give the JVM 10m to play with a no extensions)
    - Apply and start the application
    - run the script `src/test/resources/scripts/01_submitFullFitnote.sh 1234`
    
The application rejects...

    0:0:0:0:0:0:0:1 - - [06/Jul/2017:11:33:31 +0000] "POST /photo HTTP/1.1" 500 249 "-" "curl/7.54.0" 77
    WARN  [2017-07-06 11:33:31,822] org.eclipse.jetty.server.HttpChannel: /photo
    ! java.lang.OutOfMemoryError: Java heap space
    ...
    ...
    ...
    
and the request is rejected

    <html>
    <head>
    <meta http-equiv="Content-Type" content="text/html;charset=utf-8"/>
    <title>Error 500 Request failed.</title>
    </head>
    <body><h2>HTTP ERROR 500</h2>
    <p>Problem accessing /photo. Reason:
    <pre>    Request failed.</pre></p>
    </body>
    </html>

but the application remains active

## Reject potential OutOfMemory for processing

The OCR and image manipulation takes quite a bit of memory and although it may be possible to accept the image payload there may not be enough memory to actually process it.  `fitnotecontroller.utils.MemoryChecker` class uses the yml entry `estimatedRequestMemoryMb` (defaulting to 25mb) to check the available Runtime memory.  If the available runtime memory is less than the `estimatedRequestMemoryMb` the request will be rejected with a 503 (service unavailable).

- 'Edit Configurations' on FitnoteControllerApplication and add the following to the VM Options:-
    - `-Xms35m -Xmx35m` (to give the JVM 35m to play with a no extensions)
    - Apply and start the application
    - run the script `src/test/resources/scripts/01_submitFullFitnote.sh 1234`
    
The applications rejects...

    INFO  [2017-07-06 11:59:28,622] fitnotecontroller.utils.MemoryChecker (dwp encoded): Current available memory is 13mb, abs required memory is 25mb, request REJECTED
    DEBUG [2017-07-06 11:59:28,622] fitnotecontroller.FitnoteSubmitResource (dwp encoded): Completed /photo, send back status 503
    0:0:0:0:0:0:0:1 - - [06/Jul/2017:11:59:28 +0000] "POST /photo HTTP/1.1" 503 25 "-" "curl/7.54.0" 49

## Image processing runs out of memory

- 'Edit Configurations' on FitnoteControllerApplication and add the following to the VM Options:-
    - `-Xms35m -Xmx35m` (to give the JVM 35m to play with a no extensions)
    - update the **dev.yml** `estimatedRequestMemoryMb` value to **5**
    - Apply and start the application (using the dev.yml)
    - run the script `src/test/resources/scripts/01_submitFullFitnote.sh 1234`
    
The application rejects...

    INFO  [2017-07-06 12:29:06,241] fitnotecontroller.utils.MemoryChecker (dwp encoded): Current available memory is 14mb, abs required memory is 5mb, request ALLOWED
    DEBUG [2017-07-06 12:29:06,392] fitnotecontroller.ImageStorage (dwp encoded): Session id does not exist, created entry for 1234
    DEBUG [2017-07-06 12:29:06,576] fitnotecontroller.FitnoteSubmitResource (dwp encoded): Json Validated correctly
    DEBUG [2017-07-06 12:29:06,577] fitnotecontroller.FitnoteSubmitResource (dwp encoded): Completed /photo, send back status 202
    0:0:0:0:0:0:0:1 - - [06/Jul/2017:12:29:06 +0000] "POST /photo HTTP/1.1" 202 20 "-" "curl/7.54.0" 455
    ERROR [2017-07-06 12:29:06,820] fitnotecontroller.FitnoteSubmitResource (dwp encoded): java.lang.OutOfMemoryError: Java heap space

When the thread reports the **OutOfMemory** error it will set the status for the image before it exits to preserve the user journey.  When the front-end checks back on the status of the request (after being given the initial 202) it will get:-

`$ curl -m 10 -XGET --noproxy '*' http://localhost:9100/imagestatus?sessionId=1234`

    {"fitnoteStatus":"FAILED_ERROR", "barcodeStatus" : "CREATED"}

