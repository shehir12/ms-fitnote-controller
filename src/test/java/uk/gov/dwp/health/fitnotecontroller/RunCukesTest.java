package uk.gov.dwp.health.fitnotecontroller;

import cucumber.api.CucumberOptions;
import cucumber.api.junit.Cucumber;
import uk.gov.dwp.health.fitnotecontroller.application.FitnoteControllerApplication;
import uk.gov.dwp.health.fitnotecontroller.application.FitnoteControllerConfiguration;
import io.dropwizard.testing.junit.DropwizardAppRule;
import org.slf4j.Logger;
import org.junit.AfterClass;
import org.junit.ClassRule;
import org.junit.runner.RunWith;
import uk.gov.dwp.logging.DwpEncodedLogger;

import java.io.File;

import static io.dropwizard.testing.ResourceHelpers.resourceFilePath;

@RunWith(Cucumber.class)
@SuppressWarnings("squid:S2187")
@CucumberOptions(plugin = "json:target/cucumber-report.json", tags = {"@CH"})
public class RunCukesTest {
    private final static Logger LOGGER = DwpEncodedLogger.getLogger(RunCukesTest.class.getName());

    @AfterClass
    public static void init() {
        File dir = new File("work");

        if ((dir.exists()) && (dir.isDirectory())) {
            LOGGER.error("*** delete rabbit configuration file 'work/default/config/default.json' result {} ***", new File("work/default/config/default.json").delete());
            LOGGER.error("*** delete rabbit configuration file 'work/config.json' result {} ***", new File("work/config.json").delete());
        }
    }

    @ClassRule
    public static final DropwizardAppRule<FitnoteControllerConfiguration> RULE = new DropwizardAppRule<>(FitnoteControllerApplication.class, resourceFilePath("test.yml"));
}
