package uk.gov.dwp.health.fitnotecontroller.cucumber;

import com.rabbitmq.client.BuiltinExchangeType;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.GetResponse;
import org.apache.qpid.server.BrokerOptions;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeoutException;

public class RabbitMqUtilities {
    private static final String CONFIG_FILE = "src/test/resources/rabbitmq/rabbit-config.json";

    private static ConnectionFactory connectionFactory = new ConnectionFactory();

    private Connection getConnection(URI connectionUri) throws NoSuchAlgorithmException, KeyManagementException, URISyntaxException, TimeoutException, IOException {
        connectionFactory.setUri(connectionUri.toString());
        return connectionFactory.newConnection();
    }

    private void initCatchAllTestQueue(URI serverURI, String queueName, String bindingExchange, List<String> exchangeBindings, boolean isDurable) throws IOException, NoSuchAlgorithmException, URISyntaxException, TimeoutException, KeyManagementException {
        Channel channel = getConnection(serverURI).createChannel();

        channel.queueDeclare(queueName, isDurable, false, false, null);
        for (String item : exchangeBindings) {
            channel.queueBind(queueName, bindingExchange, item);
        }

        channel.queuePurge(queueName);
    }

    public void initCatchAllTestQueue(URI serverURI, String queueName, String bindingExchange, boolean isDurable) throws IOException, UnrecoverableKeyException, NoSuchAlgorithmException, URISyntaxException, TimeoutException, KeyManagementException {
        initCatchAllTestQueue(serverURI, queueName, bindingExchange, Collections.singletonList("#"), isDurable);
    }

    public void initStandardExchange(URI serverURI, String exchangeName) throws IOException, UnrecoverableKeyException, NoSuchAlgorithmException, URISyntaxException, TimeoutException, KeyManagementException {
        Channel channel = getConnection(serverURI).createChannel();
        channel.exchangeDeclare(exchangeName, BuiltinExchangeType.TOPIC, false, false, null);
    }

    static BrokerOptions setupBrokerTestService(int port) throws Exception {
        BrokerOptions brokerOptions = new BrokerOptions();

        brokerOptions.setInitialConfigurationLocation(CONFIG_FILE);
        brokerOptions.setConfigProperty("qpid.amqp_port", String.valueOf(port));

        return brokerOptions;
    }

    GetResponse getQueueContents(URI serverURI, String queueName, boolean autoAck) throws IOException, UnrecoverableKeyException, NoSuchAlgorithmException, URISyntaxException, TimeoutException, KeyManagementException {
        return getConnection(serverURI).createChannel().basicGet(queueName, autoAck);
    }
}
