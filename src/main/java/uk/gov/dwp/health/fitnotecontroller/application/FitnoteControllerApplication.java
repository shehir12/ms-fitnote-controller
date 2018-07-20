package uk.gov.dwp.health.fitnotecontroller.application;

import io.lettuce.core.cluster.RedisClusterClient;
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

public class FitnoteControllerApplication extends Application<FitnoteControllerConfiguration> {

    @Override
    protected void bootstrapLogging() {
        // to prevent dropwizard using its own standard logger
    }

    @Override
    public void run(FitnoteControllerConfiguration fitnoteControllerConfiguration, Environment environment) throws Exception {

        CryptoDataManager rabbitMqKmsCrypto = null;
        if ((fitnoteControllerConfiguration.isRabbitEncryptMessages()) && (null == fitnoteControllerConfiguration.getRabbitKmsCryptoConfiguration())) {
            throw new CryptoException("RabbitEncryptMessages is TRUE.  Cannot encrypt without a valid 'rabbitKmsCryptoConfiguration' configuration item");

        } else if (fitnoteControllerConfiguration.isRabbitEncryptMessages()) {
            rabbitMqKmsCrypto = new CryptoDataManager(fitnoteControllerConfiguration.getRabbitKmsCryptoConfiguration());
        }

        CryptoDataManager redisMqKmsCrypto = null;
        if ((fitnoteControllerConfiguration.isRedisEncryptMessages()) && (null == fitnoteControllerConfiguration.getRedisKmsCryptoConfiguration())) {
            throw new CryptoException("RedisEncryptMessages is TRUE.  Cannot encrypt without a valid 'redisKmsCryptoConfiguration' configuration item");

        } else if (fitnoteControllerConfiguration.isRedisEncryptMessages()) {
            redisMqKmsCrypto = new CryptoDataManager(fitnoteControllerConfiguration.getRedisKmsCryptoConfiguration());
        }

        final RedisClusterClient redisClient = RedisClusterClient.create(fitnoteControllerConfiguration.getRedisStoreURI());
        final ImageStorage imageStorage = new ImageStorage(fitnoteControllerConfiguration, redisClient, redisMqKmsCrypto);

        final PublishSubscribe rabbitMqPublisher = new PublishSubscribe(new MessageEncoder(rabbitMqKmsCrypto), fitnoteControllerConfiguration.getRabbitMqURI());
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
    }

    public static void main(String[] args) throws Exception {
        new FitnoteControllerApplication().run(args);
    }
}
