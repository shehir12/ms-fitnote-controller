package uk.gov.dwp.health.fitnotecontroller.application;

import uk.gov.dwp.health.crypto.CryptoDataManager;
import uk.gov.dwp.health.crypto.exception.CryptoException;
import uk.gov.dwp.health.crypto.rabbitmq.MessageEncoder;
import uk.gov.dwp.health.fitnotecontroller.FitnoteAddressResource;
import uk.gov.dwp.health.fitnotecontroller.FitnoteConfirmationResource;
import uk.gov.dwp.health.fitnotecontroller.FitnoteDeclarationResource;
import uk.gov.dwp.health.fitnotecontroller.FitnoteSubmitResource;
import uk.gov.dwp.health.fitnotecontroller.ImageStorage;
import io.dropwizard.Application;
import io.dropwizard.setup.Environment;
import uk.gov.dwp.health.rabbitmq.PublishSubscribe;

import java.util.Timer;
import java.util.TimerTask;

public class FitnoteControllerApplication extends Application<FitnoteControllerConfiguration> {

    @Override
    public void run(FitnoteControllerConfiguration fitnoteControllerConfiguration, Environment environment) throws Exception {

        final ImageStorage imageStorage = new ImageStorage(fitnoteControllerConfiguration);

        CryptoDataManager kmsCrypto = null;
        if ((fitnoteControllerConfiguration.isRabbitEncryptMessages()) && (null == fitnoteControllerConfiguration.getKmsCryptoConfig())) {
            throw new CryptoException("RabbitEncryptMessages is TRUE.  Cannot encrypt without a valid 'kmsCryptoConfiguration' configuration item");

        } else if (fitnoteControllerConfiguration.isRabbitEncryptMessages()) {
            kmsCrypto = new CryptoDataManager(fitnoteControllerConfiguration.getKmsCryptoConfig());
        }

        final MessageEncoder cryptoMessageEncoder = new MessageEncoder(kmsCrypto);

        final PublishSubscribe rabbitMqPublisher = new PublishSubscribe(kmsCrypto, cryptoMessageEncoder, fitnoteControllerConfiguration.getRabbitMqURI());
        rabbitMqPublisher.setTruststoreCredentials(fitnoteControllerConfiguration.getRabbitMqTruststoreFile(), fitnoteControllerConfiguration.getRabbitMqTruststorePass());
        rabbitMqPublisher.setKeystoreCredentials(fitnoteControllerConfiguration.getRabbitMqKeystoreFile(), fitnoteControllerConfiguration.getRabbitMqKeystorePass());

        final FitnoteSubmitResource resource = new FitnoteSubmitResource(fitnoteControllerConfiguration, imageStorage);
        final FitnoteConfirmationResource confirmationResource = new FitnoteConfirmationResource(imageStorage);
        final FitnoteAddressResource addressResource = new FitnoteAddressResource(imageStorage);
        final FitnoteDeclarationResource declarationResource = new FitnoteDeclarationResource(imageStorage, rabbitMqPublisher, fitnoteControllerConfiguration);

        environment.jersey().register(resource);
        environment.jersey().register(confirmationResource);
        environment.jersey().register(declarationResource);
        environment.jersey().register(addressResource);

        Timer expiryTime = new Timer("Session Expiry Timer");
        TimerTask runExpiryTime = new TimerTask() {
            @Override
            public void run() {
                imageStorage.clearExpiredObjects();
            }
        };
        expiryTime.scheduleAtFixedRate(runExpiryTime, 5000, fitnoteControllerConfiguration.getFrequencyOfExpiryTimeInMilliSeconds());
    }

    public static void main(String[] args) throws Exception {
        new FitnoteControllerApplication().run(args);
    }
}
