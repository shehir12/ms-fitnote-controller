package uk.gov.dwp.health.fitnotecontroller.cucumber;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.sqs.model.Message;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import cucumber.api.java.After;
import cucumber.api.java.Before;
import cucumber.api.java.en.And;
import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;
import gherkin.deps.net.iharder.Base64;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.junit.Assert;
import org.slf4j.Logger;
import org.junit.Rule;
import org.slf4j.LoggerFactory;
import uk.gov.dwp.health.crypto.CryptoConfig;
import uk.gov.dwp.health.crypto.CryptoDataManager;
import uk.gov.dwp.health.crypto.CryptoMessage;
import uk.gov.dwp.health.crypto.exception.CryptoException;
import uk.gov.dwp.health.messageq.EventConstants;
import uk.gov.dwp.health.messageq.amazon.items.AmazonConfigBase;
import uk.gov.dwp.health.messageq.amazon.items.messages.SnsMessageClassItem;
import uk.gov.dwp.health.messageq.amazon.utils.AmazonQueueUtilities;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class FitnoteCucumberSteps {
    private static final Logger LOG = LoggerFactory.getLogger(FitnoteCucumberSteps.class.getName());
    private static final int IMAGE_STATUS_QUERY_TIMEOUT_MILLIS = 120000;

    private static final String SPOOF_AWS_RESPONSE =
            "{\n" +
                    "  \"CiphertextBlob\": \"AQEDAHjRYf5WytIc0C857tFSnBaPn2F8DgfmThbJlGfR8P3WlwAAAH4wfAYJKoZIhvcNAQcGoG8wbQIBADBoBgkqhkiG9w0BBwEwHgYJYIZIAWUDBAEuMBEEDEFogLqPWZconQhwHAIBEIA7d9AC7GeJJM34njQvg4Wf1d5sw0NIo1MrBqZa+YdhV8MrkBQPeac0ReRVNDt9qleAt+SHgIRF8P0H+7U=\",\n" +
                    "  \"KeyId\": \"arn:aws:kms:us-east-2:111122223333:key/1234abcd-12ab-34cd-56ef-1234567890ab\",\n" +
                    "  \"Plaintext\": \"VdzKNHGzUAzJeRBVY+uUmofUGGiDzyB3+i9fVkh3piw=\"\n" +
                    "}";

    private ObjectMapper mapper = new ObjectMapper();
    private AmazonQueueUtilities queueUtilities;
    private CryptoDataManager awsKmsCryptoClass;
    private List<Message> queueMessages;
    private Pattern regexFileExtension;
    private HttpClient httpClient;
    private HttpResponse response;
    private String jsonResponse;

    @Rule
    public WireMockRule awsKmsServiceMock = new WireMockRule(wireMockConfig().port(5678));

    @Before
    public void startServiceMocks() throws CryptoException {
        regexFileExtension = Pattern.compile("\\.(\\w+)");

        awsKmsServiceMock.stubFor(post(urlEqualTo("/"))
                .willReturn(aResponse()
                        .withBody(SPOOF_AWS_RESPONSE)
                        .withStatus(200)
                        .withHeader("x-amzn-RequestId", UUID.randomUUID().toString())
                ));

        // create local properties to negate KMS from needing to access Metadata Service for IAM role privs
        System.setProperty("aws.accessKeyId", "this_is_my_system_property_key");
        System.setProperty("aws.secretKey", "abcd123456789");

        AmazonConfigBase snsConfig = new AmazonConfigBase();
        snsConfig.setEndpointOverride("http://localhost:4575");
        snsConfig.setS3EndpointOverride("http://localhost:4572");
        snsConfig.setLargePayloadSupportEnabled(false);
        snsConfig.setPathStyleAccessEnabled(true);
        snsConfig.setS3BucketName("sns-bucket");
        snsConfig.setRegion(Regions.US_EAST_1);

        AmazonConfigBase sqsConfig = new AmazonConfigBase();
        sqsConfig.setEndpointOverride("http://localhost:4576");
        sqsConfig.setS3EndpointOverride("http://localhost:4572");
        sqsConfig.setLargePayloadSupportEnabled(false);
        sqsConfig.setPathStyleAccessEnabled(true);
        sqsConfig.setS3BucketName("sqs-bucket");
        sqsConfig.setRegion(Regions.US_EAST_1);

        queueUtilities = new AmazonQueueUtilities(sqsConfig, snsConfig);

        CryptoConfig cryptoConfig = new CryptoConfig("test_request_id");
        cryptoConfig.setKmsEndpointOverride("http://localhost:5678");
        awsKmsCryptoClass = new CryptoDataManager(cryptoConfig);

        awsKmsServiceMock.start();
    }

    @After
    public void stopServiceMocks() {
        awsKmsServiceMock.stop();
    }

    @Given("^the http client is up$")
    public void theControllerIsUp() {
        httpClient = HttpClientBuilder.create().build();
    }

    @When("^I hit the service url \"([^\"]*)\" with the following json body$")
    public void hitServiceUrlWithSpecifiedJson(String url, Map<String, String> jsonValues) throws IOException {
        String jsonRequestBody = buildJsonBody(jsonValues);
        performHttpPostWithUriOf(url, jsonRequestBody);
    }

    @And("^I hit the service url \"([^\"]*)\" with session id \"([^\"]*)\" getting return status (\\d+) and finally containing the following json body$")
    public void iHitTheServiceUrlWithSessionIdGettingReturnStatusAndFinallyContainingTheFollowingJsonBody(String url, String sessionId, int status, Map<String, String> expectedValues) throws IOException, InterruptedException {
        String fullUrl = String.format("%s?sessionId=%s", url, sessionId);
        long startTime = System.currentTimeMillis();

        do {
            if ((System.currentTimeMillis() - startTime) > IMAGE_STATUS_QUERY_TIMEOUT_MILLIS) {
                throw new IOException(String.format("TIMING OUT :: '%s' request after %d milliseconds with no match on response body", url, IMAGE_STATUS_QUERY_TIMEOUT_MILLIS));
            }

            TimeUnit.SECONDS.sleep(1);
            performHttpGetWithUriOf(fullUrl);
            checkHTTPResponseStatusCode(status);

        } while (!httpResponseBodyContainsEntries(expectedValues));
    }

    @And("^I hit the service url \"([^\"]*)\" with a POST and session id \"([^\"]*)\" getting return status (\\d+) the following json body$")
    public void iHitTheServiceUrlWithAPOSTAndSessionIdGettingReturnStatusTheFollowingJsonBody(String url, String sessionId, int statusCode, Map<String, String> expectedValues) throws IOException {
        performHttpPostWithUriOf(url, String.format("{\"sessionId\":\"%s\"}", sessionId));
        checkHTTPResponseStatusCode(statusCode);

        assertTrue(httpResponseBodyContainsEntries(expectedValues));
    }

    @And("^I hit the service url \"([^\"]*)\" with session id \"([^\"]*)\" getting return status (\\d+) and finally timing out trying to match the following body$")
    public void iHitTheServiceUrlWithSessionIdGettingReturnStatusAndFinallyContainingTheFollowingJsonBodyException(String url, String sessionId, int status, Map<String, String> expectedValues) throws InterruptedException {
        try {
            iHitTheServiceUrlWithSessionIdGettingReturnStatusAndFinallyContainingTheFollowingJsonBody(url, sessionId, status, expectedValues);

        } catch (IOException e) {
            LOG.error(e.getClass().getName(), e);
            assertThat("Must have timed out", e.getMessage(), containsString("TIMING OUT ::"));
        }
    }

    @Then("^I receive a HTTP response of (\\d+)$")
    public void checkHTTPResponseStatusCode(int expectedStatusCode) {
        int actualStatusCode = response.getStatusLine().getStatusCode();
        assertThat(actualStatusCode, is(expectedStatusCode));
    }

    @And("^I create a sns topic named \"([^\"]*)\"$")
    public void iCreateAnSnsExchangeNamed(String topicName) {
        queueUtilities.createTopic(topicName);
    }

    @And("^I create a catch all subscription for queue name \"([^\"]*)\" binding to topic \"([^\"]*)\" with msg visibility timeout of (\\d+) seconds$")
    public void iCreateACatchAllSubscriptionForQueueNameBindingToExchange(String queueName, String topicName, int timeout) {
        queueUtilities.createQueue(queueName, timeout);
        queueUtilities.subscribeQueueToTopic(queueName, topicName);
    }

    @And("^I wait (\\d+) seconds for the visibility timeout to expire$")
    public void iWaitSecondsForVisibiliyTimout(long timeout) throws InterruptedException {
        TimeUnit.SECONDS.sleep(timeout);
    }

    @And("^a message is successfully removed from the queue, there were a total of (\\d+) messages on queue \"([^\"]*)\"$")
    public void thereIsPendingMessageOnQueue(int totalMessages, String queueName) throws IOException {
        queueMessages = queueUtilities.receiveMessages(queueName, queueUtilities.getS3Sqs());

        Assert.assertThat("mismatched messages", queueMessages.size(), is(equalTo(totalMessages)));

        assertNotNull("queue contents are null", queueMessages);
        queueUtilities.deleteMessageFromQueue(queueName, queueMessages.get(0).getReceiptHandle());
    }

    @And("^there are no pending messages on queue \"([^\"]*)\"$")
    public void thereAreNoPendingMessageOnQueue(String queueName) throws IOException {
        queueMessages = queueUtilities.receiveMessages(queueName, queueUtilities.getS3Sqs());
        assertNotNull(queueMessages);
        assertTrue(queueMessages.isEmpty());
    }

    @And("^the message has a correlation id, delivery mode (PERSISTENT|NON-PERSISTENT)? and a valid ImagePayload with the following NINO, House number, Postcode and matching SessionId$")
    public void theMessageIsTakenFromTheQueueHasACorrelationIdAndAValidImagePayloadWithTheFollowingNINOHouseNumberAndPostcode(String msgPersistence, Map<String, String> jsonValues) throws IOException, CryptoException {
        assertNotNull(queueMessages);
        assertFalse(queueMessages.isEmpty());

        SnsMessageClassItem snsMessageClass = new SnsMessageClassItem().buildMessageClassItem(queueMessages.get(0).getBody());
        String msgContents = snsMessageClass.getMessage();

        if (snsMessageClass.getMessageAttributes().get(EventConstants.KMS_DATA_KEY_MARKER) != null) {
            CryptoMessage cryptoMessage = new CryptoMessage();
            cryptoMessage.setKey(snsMessageClass.getMessageAttributes().get(EventConstants.KMS_DATA_KEY_MARKER).getStringValue());
            cryptoMessage.setMessage(msgContents);

            msgContents = awsKmsCryptoClass.decrypt(cryptoMessage);
        }

        JsonNode payload = new ObjectMapper().readTree(msgContents).path("payload");

        for (Map.Entry<String, String> field : jsonValues.entrySet()) {
            switch (field.getKey()) {
                case "nino":
                    assertThat("Mismatched NINO", payload.path(field.getKey()).toString(), is(equalTo(field.getValue())));
                    break;
                case "houseNameOrNumber":
                    assertThat("Mismatched House Name/Number", payload.path("claimantAddress").path(field.getKey()).toString(), is(equalTo(field.getValue())));
                    break;
                case "postcode":
                    assertThat("Mismatched Postcode", payload.path("claimantAddress").path(field.getKey()).toString(), is(equalTo(field.getValue())));
                    break;
                case "sessionId":
                    assertThat("Wrong sessionId", payload.path("sessionId").toString(), is(equalTo(field.getValue())));
                    break;
                default:
                    throw new IOException(String.format("unknown field '%s'", field.getKey()));
            }
        }
    }

    private boolean httpResponseBodyContainsEntries(Map<String, String> expectedJsonValues) {
        boolean matchResult = true;
        try {
            JsonNode responseBody = mapper.readTree(jsonResponse);
            for (Map.Entry<String, String> expectedJsonKeyValue : expectedJsonValues.entrySet()) {
                JsonNode jsonKey = responseBody.get(expectedJsonKeyValue.getKey());
                if (jsonKey != null) {
                    String actualValue = jsonKey.isObject()? jsonKey.toString() : jsonKey.textValue();
                    LOG.info("Found json element '{}' with expected value '{}'.  Actual value was '{}'", expectedJsonKeyValue.getKey(), expectedJsonKeyValue.getValue(), actualValue);
                    if (!expectedJsonKeyValue.getValue().equals(actualValue)) {
                        matchResult = false;
                    }

                } else {
                    matchResult = false;
                }
            }

        } catch (IOException e) {
            matchResult = false;
            LOG.error(e.getClass().getName(), e);
        }

        return matchResult;
    }

    private String buildJsonBody(Map<String, String> jsonValues) throws IOException {
        StringBuilder builder = new StringBuilder();
        builder.append("{");
        String delimiter = "";
        for (Map.Entry<String, String> jsonKeyValue : jsonValues.entrySet()) {
            if (!jsonKeyValue.getKey().isEmpty()) {
                builder.append(delimiter);
                builder.append("\"" + jsonKeyValue.getKey() + "\":");
                String value = jsonKeyValue.getValue();
                Matcher matcher = regexFileExtension.matcher(value);
                if (matcher.find()) {
                    builder.append("\"" + getEncodedImage(value) + "\"");
                } else {
                    builder.append(value);
                }
                delimiter = ",";
            }
        }
        builder.append("}");
        return builder.toString();
    }

    private String getEncodedImage(String imageFileName) throws IOException {
        return Base64.encodeFromFile(this.getClass().getResource(imageFileName).getPath());
    }

    private void performHttpPostWithUriOf(String uri, String body) throws IOException {
        HttpPost httpUriRequest = new HttpPost(uri);
        HttpEntity entity = new StringEntity(body);
        httpUriRequest.setEntity(entity);
        response = httpClient.execute(httpUriRequest);
        HttpEntity responseEntity = response.getEntity();
        jsonResponse = EntityUtils.toString(responseEntity);
    }

    private void performHttpGetWithUriOf(String uri) throws IOException {
        HttpGet httpUriRequest = new HttpGet(uri);
        response = httpClient.execute(httpUriRequest);
        HttpEntity responseEntity = response.getEntity();
        jsonResponse = EntityUtils.toString(responseEntity);
        LOG.info("RESPONSE FROM HTTP GET : {}", jsonResponse);
    }
}
