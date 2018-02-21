package fitnotecontroller.utils;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import gov.dwp.securecomms.tls.TLSConnectionBuilder;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class EsaCenterLookupTest {

    private final String INVALID_POSTCODE = "GL529999";
    private final String VALID_POSTCODE = "LS27UA";
    private final String DEFAULT_DRS_OFFICE_ROUTING_CODE = "ES33 9QX";

    public static final String CLIENT_TRUST_STORE_PATH = "src/test/resources/tls/esaCenterLookupTrustStore.jks";
    public static final String TRUST_STORE_PASSWORD = "password";

    public final String ESA_LOOKUP_URL = "https://localhost:8086/esa/findCenter/";

    TLSConnectionBuilder sslConnection = null;
    private EsaCenterLookup esaCenterLookup = null;

    @Rule
    public WireMockRule postcodeLookupService = new WireMockRule(wireMockConfig().port(6666)
            .httpsPort(8086).needClientAuth(false).trustStorePath(CLIENT_TRUST_STORE_PATH)
            .trustStorePassword(TRUST_STORE_PASSWORD).keystorePath(CLIENT_TRUST_STORE_PATH)
            .keystorePassword(TRUST_STORE_PASSWORD));

    @Before
    public void startup() {
        sslConnection = new TLSConnectionBuilder(CLIENT_TRUST_STORE_PATH, TRUST_STORE_PASSWORD);
        esaCenterLookup = new EsaCenterLookup(ESA_LOOKUP_URL, sslConnection, DEFAULT_DRS_OFFICE_ROUTING_CODE);
        postcodeLookupService.start();
    }

    @After
    public void stopWireMock() {
        postcodeLookupService.stop();
    }

    @Test
    public void esaServiceUnavailable() {
        String response = esaCenterLookup.getEsaOfficeRoutingCodeForDRS(VALID_POSTCODE);
        assertThat(response, is(DEFAULT_DRS_OFFICE_ROUTING_CODE));
    }

    @Test
    public void badTrustStorePassword() {
        sslConnection = new TLSConnectionBuilder(CLIENT_TRUST_STORE_PATH, "bad");
        esaCenterLookup = new EsaCenterLookup(ESA_LOOKUP_URL, sslConnection, DEFAULT_DRS_OFFICE_ROUTING_CODE);
        String response = esaCenterLookup.getEsaOfficeRoutingCodeForDRS(VALID_POSTCODE);
        assertThat(response, is(DEFAULT_DRS_OFFICE_ROUTING_CODE));
    }

    @Test
    public void testVaildPostCodeReturnsCorrectDRSRoutingCode() {
        postcodeLookupService.stubFor(get(urlEqualTo("/esa/findCenter/" + VALID_POSTCODE)).willReturn(aResponse().withBody("[{\"office\":\"ESA Hull\",\"mailsite\":\"Mail Handling Site A\",\"postcode\":\"WV98 2AG\",\"benefitCentre\":\"HULL BC\",\"freepostAddress\":\"Freepost DWP ESA 17\",\"officeRoutingCodeForDRS\":\"ES1 9QX\"}]").withStatus(200)));
        String response = esaCenterLookup.getEsaOfficeRoutingCodeForDRS(VALID_POSTCODE);
        String expectedOfficeRoutingCodeForDRS = "ES1 9QX";
        assertThat(response, is(expectedOfficeRoutingCodeForDRS));
    }

    @Test
    public void invalidPostCode() {
        postcodeLookupService.stubFor(get(urlEqualTo("/esa/findCenter/" + INVALID_POSTCODE))
                .willReturn(aResponse().withStatus(500)));
        String response = esaCenterLookup.getEsaOfficeRoutingCodeForDRS(INVALID_POSTCODE);
        assertThat(response, is(DEFAULT_DRS_OFFICE_ROUTING_CODE));
    }

    @Test
    public void testEmptyPostcodeReturnsDefaultDRSRoutingCode() {
        String response = esaCenterLookup.getEsaOfficeRoutingCodeForDRS("");
        assertThat(response, is(DEFAULT_DRS_OFFICE_ROUTING_CODE));
    }

    @Test
    public void testNullPostcodeReturnsDefaultDRSRoutingCode() {
        String response = esaCenterLookup.getEsaOfficeRoutingCodeForDRS(null);
        assertThat(response, is(DEFAULT_DRS_OFFICE_ROUTING_CODE));
    }
}
