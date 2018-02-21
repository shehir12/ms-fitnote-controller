package fitnotecontroller;

import cucumber.api.CucumberOptions;
import cucumber.api.junit.Cucumber;
import fitnotecontroller.application.FitnoteControllerApplication;
import fitnotecontroller.application.FitnoteControllerConfiguration;
import io.dropwizard.testing.junit.DropwizardAppRule;
import org.junit.AfterClass;
import org.junit.ClassRule;
import org.junit.runner.RunWith;

import java.io.File;

import static io.dropwizard.testing.ResourceHelpers.resourceFilePath;

@RunWith(Cucumber.class)
@SuppressWarnings("squid:S2187")
@CucumberOptions(plugin = "json:target/cucumber-report.json", tags = {})
public class RunCukesTest {

    @AfterClass
    public static void init() {
        File dir = new File("work");

        if ((dir.exists()) && (dir.isDirectory())) {
            new File("work/default/config/default.json").deleteOnExit();
            new File("work/config.json").deleteOnExit();
        }
    }

    @ClassRule
    public static final DropwizardAppRule<FitnoteControllerConfiguration> RULE = new DropwizardAppRule<>(FitnoteControllerApplication.class, resourceFilePath("test.yml"));
}
