package fitnotecontroller.utils;


import com.fasterxml.jackson.databind.ObjectMapper;
import fitnotecontroller.domain.ContactCentre;
import gov.dwp.securecomms.tls.TLSConnectionBuilder;
import gov.dwp.securecomms.tls.TLSGeneralException;
import gov.dwp.utilities.logging.DwpEncodedLogger;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

public class EsaCenterLookup {

    private static final Logger LOG = DwpEncodedLogger.getLogger(EsaCenterLookup.class);
    private TLSConnectionBuilder sslAuthenticatedConnection;
    private final String defaultRoutingForDRS;
    private String easPostCodeLookUpUrl;

    public EsaCenterLookup(String easPostCodeLookUpUrl, TLSConnectionBuilder sslConnection,
                           String defaultRoutingForDRS) {
        this.sslAuthenticatedConnection = sslConnection;
        this.easPostCodeLookUpUrl = easPostCodeLookUpUrl;
        this.defaultRoutingForDRS = defaultRoutingForDRS;
    }

    public String getEsaOfficeRoutingCodeForDRS(String postcode) {
        String routingCodeForDRS;
        HttpResponse httpResponse;
        int statusCode;
        try (CloseableHttpClient httpsClient = sslAuthenticatedConnection.configureSSLConnection()) {
            HttpGet getRequest = new HttpGet(easPostCodeLookUpUrl + postcode);
            httpResponse = httpsClient.execute(getRequest);
            statusCode = httpResponse.getStatusLine().getStatusCode();
            if (statusCode == 200) {
                try (InputStream content = httpResponse.getEntity().getContent()) {
                    ContactCentre[] contactCentre = new ObjectMapper().readValue(IOUtils.toString(content), ContactCentre[].class);
                    routingCodeForDRS = contactCentre[0].getOfficeRoutingCodeForDRS();
                    LOG.debug("Returing ESA office routing code for DRS");
                }
            } else {
                LOG.warn("ESA office routing code for DRS lookup failed to retrieve corresponding DRS office routing code, using default office routing code for DRS");
                routingCodeForDRS = defaultRoutingForDRS;
            }
        } catch (IOException | CertificateException | UnrecoverableKeyException
                | NoSuchAlgorithmException | KeyStoreException | KeyManagementException
                | TLSGeneralException e) {
            LOG.debug("ESA office routing code for DRS lookup returning default office routing code for DRS.");
            routingCodeForDRS = defaultRoutingForDRS;
            LOG.debug(e);
        }
        return routingCodeForDRS;
    }
}