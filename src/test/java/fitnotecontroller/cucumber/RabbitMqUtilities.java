package fitnotecontroller.cucumber;

import com.rabbitmq.client.BuiltinExchangeType;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.GetResponse;
import gov.dwp.securecomms.tls.TLSGeneralException;
import org.apache.qpid.server.BrokerOptions;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeoutException;

public class RabbitMqUtilities {
    private static final String USER_CREDENTIALS_FILE = "src/test/resources/rabbitmq/rabbit.passwd";
    private static final String CONFIG_FILE = "src/test/resources/rabbitmq/rabbit-config.json";

    private static ConnectionFactory connectionFactory = new ConnectionFactory();

    private Connection getConnection(URI connectionUri) throws NoSuchAlgorithmException, KeyManagementException, URISyntaxException, TimeoutException, IOException, UnrecoverableKeyException, CertificateException, KeyStoreException, TLSGeneralException {
        connectionFactory.setUri(connectionUri.toString());
        return connectionFactory.newConnection();
    }

    private void initCatchAllTestQueue(URI serverURI, String queueName, String bindingExchange, List<String> exchangeBindings) throws IOException, UnrecoverableKeyException, NoSuchAlgorithmException, URISyntaxException, TimeoutException, TLSGeneralException, KeyStoreException, CertificateException, KeyManagementException {
        Channel channel = getConnection(serverURI).createChannel();

        channel.queueDeclare(queueName, true, false, false, null);
        for (String item : exchangeBindings) {
            channel.queueBind(queueName, bindingExchange, item);
        }
    }

    void initCatchAllTestQueue(URI serverURI, String queueName, String bindingExchange) throws IOException, UnrecoverableKeyException, NoSuchAlgorithmException, URISyntaxException, TimeoutException, TLSGeneralException, KeyStoreException, CertificateException, KeyManagementException {
        initCatchAllTestQueue(serverURI, queueName, bindingExchange, Collections.singletonList("#"));
    }

    void initStandardExchange(URI serverURI, String exchangeName) throws IOException, UnrecoverableKeyException, NoSuchAlgorithmException, URISyntaxException, TimeoutException, TLSGeneralException, KeyStoreException, CertificateException, KeyManagementException {
        Channel channel = getConnection(serverURI).createChannel();
        channel.exchangeDeclare(exchangeName, BuiltinExchangeType.TOPIC, true, false, null);
    }

    static BrokerOptions setupBrokerTestService(int port) throws Exception {
        BrokerOptions brokerOptions = new BrokerOptions();

        brokerOptions.setConfigProperty("qpid.pass_file", USER_CREDENTIALS_FILE);
        brokerOptions.setInitialConfigurationLocation(CONFIG_FILE);
        brokerOptions.setConfigProperty("qpid.amqp_port", String.valueOf(port));

        return brokerOptions;
    }

    GetResponse getQueueContents(URI serverURI, String queueName, boolean autoAck) throws IOException, UnrecoverableKeyException, NoSuchAlgorithmException, URISyntaxException, TimeoutException, TLSGeneralException, KeyStoreException, CertificateException, KeyManagementException {
        return getConnection(serverURI).createChannel().basicGet(queueName, autoAck);
    }
}
