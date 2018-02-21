package fitnotecontroller.cucumber;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.rabbitmq.client.GetResponse;
import cucumber.api.PendingException;
import cucumber.api.java.After;
import cucumber.api.java.Before;
import cucumber.api.java.en.And;
import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;
import fitnotecontroller.domain.ImagePayload;
import gherkin.deps.net.iharder.Base64;
import gov.dwp.securecomms.tls.TLSGeneralException;
import gov.dwp.utilities.logging.DwpEncodedLogger;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;
import org.apache.qpid.server.Broker;
import org.junit.Rule;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeoutException;
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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

@SuppressWarnings("squid:S2925")
public class FitnoteCucumberSteps {

    private static final Logger LOG = DwpEncodedLogger.getLogger(FitnoteCucumberSteps.class.getName());
    private static final int IMAGE_STATUS_QUERY_TIMEOUT_MILLIS = 60000;
    private static Broker rabbitMqBroker = new Broker();
    private static URI rabbitMqUri;

    private RabbitMqUtilities rabbitMqUtilities = new RabbitMqUtilities();
    private ObjectMapper mapper = new ObjectMapper();
    private Pattern regexFileExtension;
    private GetResponse queueMessage;
    private HttpClient httpClient;
    private HttpResponse response;
    private String jsonResponse;

    private final String SERVER_TRUST_STORE_PATH = "src/test/resources/tls/esaCenterLookupTrustStore.jks";
    private final String SERVER_KEY_STORE_PATH = "src/test/resources/tls/esaCenterLookupTrustStore.jks";
    private final String TRUST_STORE_PASS = "password";
    private final String KEY_STORE_PASS = "password";

    static {
        try {
            rabbitMqUri = new URI("amqp://system:manager@localhost:15671");
            rabbitMqBroker.startup(RabbitMqUtilities.setupBrokerTestService(rabbitMqUri.getPort()));

            Runtime.getRuntime().addShutdownHook(new Thread() {
                public void run() {
                    rabbitMqBroker.shutdown();
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Rule
    public WireMockRule postcodeLookupService =
            new WireMockRule(wireMockConfig().port(6666).httpsPort(8086).needClientAuth(false)
                    .trustStorePath(SERVER_TRUST_STORE_PATH).trustStorePassword(TRUST_STORE_PASS)
                    .keystorePath(SERVER_KEY_STORE_PATH).keystorePassword(KEY_STORE_PASS));

    @Before
    public void startServiceMocks() {
        regexFileExtension = Pattern.compile("\\.(\\w+)");
        postcodeLookupService.start();
        queueMessage = null;
    }

    @After
    public void stopServiceMocks() {
        postcodeLookupService.stop();
    }

    @Given("^the controller is up$")
    public void theControllerIsUp() throws Throwable {
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

        while (!HttpResponseBodyContainsEntries(expectedValues)) {
            if ((System.currentTimeMillis() - startTime) > IMAGE_STATUS_QUERY_TIMEOUT_MILLIS) {
                throw new IOException(String.format("TIMING OUT :: '%s' request after %d milliseconds with no match on response body", url, IMAGE_STATUS_QUERY_TIMEOUT_MILLIS));
            }

            Thread.sleep(1000);
            performHttpGetWithUriOf(fullUrl);
            checkHTTPResponseStatusCode(status);
        }
    }

    @And("^I hit the service url \"([^\"]*)\" with session id \"([^\"]*)\" getting return status (\\d+) and finally timing out trying to match the following body$")
    public void iHitTheServiceUrlWithSessionIdGettingReturnStatusAndFinallyContainingTheFollowingJsonBody_EXCEPTION(String url, String sessionId, int status, Map<String, String> expectedValues) throws InterruptedException {
        try {
            iHitTheServiceUrlWithSessionIdGettingReturnStatusAndFinallyContainingTheFollowingJsonBody(url, sessionId, status, expectedValues);

        } catch (IOException e) {
            LOG.error(e);
            assertThat("Must have timed out", e.getMessage(), containsString("TIMING OUT ::"));
        }
    }

    @Then("^I receive a HTTP response of (\\d+)$")
    public void checkHTTPResponseStatusCode(int expectedStatusCode) {
        int actualStatusCode = response.getStatusLine().getStatusCode();
        assertThat(actualStatusCode, is(expectedStatusCode));
    }

    @And("^I create a rabbit exchange named \"([^\"]*)\"$")
    public void iCreateARabbitExchangeNamed(String exchangeName) throws Throwable {
        rabbitMqUtilities.initStandardExchange(rabbitMqUri, exchangeName);
    }

    @And("^I create a catch all subscription for queue name \"([^\"]*)\" binding to exchange \"([^\"]*)\"$")
    public void iCreateACatchAllSubscriptionForQueueNameBindingToExchange(String queueName, String exchangeName) throws IOException, CertificateException, NoSuchAlgorithmException, UnrecoverableKeyException, TimeoutException, TLSGeneralException, KeyStoreException, URISyntaxException, KeyManagementException {
        rabbitMqUtilities.initCatchAllTestQueue(rabbitMqUri, queueName, exchangeName);
    }

    @And("^a message is successfully consumed from the queue and there are (\\d+) pending message left on queue \"([^\"]*)\"$")
    public void thereIsPendingMessageOnQueue(int pendingMessages, String queueName) throws Throwable {
        queueMessage = rabbitMqUtilities.getQueueContents(rabbitMqUri, queueName, true);
        assertNotNull(queueMessage);

        assertThat("mismatched pending messages", queueMessage.getMessageCount(), is(equalTo(pendingMessages)));
    }

    @And("^there are no pending messages on queue \"([^\"]*)\"$")
    public void thereAreNoPendingMessageOnQueue(String queueName) throws Throwable {
        queueMessage = rabbitMqUtilities.getQueueContents(rabbitMqUri, queueName, true);
        assertNull(queueMessage);
    }

    @And("^the message has a correlation id and a valid ImagePayload with the following NINO, House number, Postcode and matching SessionId$")
    public void theMessageIsTakenFromTheQueueHasACorrelationIdAndAValidImagePayloadWithTheFollowingNINOHouseNumberAndPostcode(Map<String, String> jsonValues) throws Throwable {
        assertNotNull(queueMessage);

        JsonNode payload = new ObjectMapper().readTree(queueMessage.getBody()).path("payload");

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
            }
        }
    }

    private boolean HttpResponseBodyContainsEntries(Map<String, String> expectedJsonValues) {
        boolean matchResult = true;
        try {
            JsonNode responseBody = mapper.readTree(jsonResponse);
            for (Map.Entry<String, String> expectedJsonKeyValue : expectedJsonValues.entrySet()) {
                JsonNode jsonKey = responseBody.get(expectedJsonKeyValue.getKey());
                if (jsonKey != null) {
                    String actualValue = jsonKey.textValue();
                    LOG.info(String.format("Found json element '%s' with expected value '%s'.  Actual value was '%s'", expectedJsonKeyValue.getKey(), expectedJsonKeyValue.getValue(), actualValue));
                    if (!expectedJsonKeyValue.getValue().equals(actualValue)) {
                        matchResult = false;
                    }

                } else {
                    matchResult = false;
                }
            }

        } catch (IOException e) {
            matchResult = false;
            LOG.error(e);
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
        LOG.info(String.format("RESPONSE FROM HTTP GET : %s", jsonResponse));
    }
}
