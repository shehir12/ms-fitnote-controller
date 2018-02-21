package fitnotecontroller.application;

import fitnotecontroller.FitnoteAddressResource;
import fitnotecontroller.FitnoteConfirmationResource;
import fitnotecontroller.FitnoteDeclarationResource;
import fitnotecontroller.FitnoteSubmitResource;
import fitnotecontroller.ImageStorage;
import io.dropwizard.Application;
import io.dropwizard.setup.Environment;
import uk.gov.dwp.health.utilities.rabbitmq.PublishSubscribe;

import java.util.Timer;
import java.util.TimerTask;

public class FitnoteControllerApplication extends Application<FitnoteControllerConfiguration> {
    @Override
    public void run(FitnoteControllerConfiguration fitnoteControllerConfiguration, Environment environment) throws Exception {

        final ImageStorage imageStorage = new ImageStorage(fitnoteControllerConfiguration);
        final PublishSubscribe rabbitMqPublisher = new PublishSubscribe(fitnoteControllerConfiguration.getRabbitMqURI());

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
                imageStorage.sessionExpired();
            }
        };
        expiryTime.scheduleAtFixedRate(runExpiryTime, 5000, fitnoteControllerConfiguration.getFrequencyOfExpiryTimeInMilliSeconds());
    }

    public static void main(String[] args) throws Exception {
        new FitnoteControllerApplication().run(args);
    }
}
