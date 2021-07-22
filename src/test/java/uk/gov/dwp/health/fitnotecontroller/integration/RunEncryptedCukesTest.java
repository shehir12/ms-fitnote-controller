package uk.gov.dwp.health.fitnotecontroller.integration;

import cucumber.api.CucumberOptions;
import cucumber.api.junit.Cucumber;
import org.junit.Ignore;
import org.testcontainers.containers.DockerComposeContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import uk.gov.dwp.health.fitnotecontroller.application.FitnoteControllerApplication;
import uk.gov.dwp.health.fitnotecontroller.application.FitnoteControllerConfiguration;
import io.dropwizard.testing.junit.DropwizardAppRule;
import org.junit.ClassRule;
import org.junit.runner.RunWith;

import java.io.File;
import java.util.Arrays;

import static io.dropwizard.testing.ResourceHelpers.resourceFilePath;

@RunWith(Cucumber.class)
@SuppressWarnings({"squid:S2187", "squid:S1118", "squid:S4042", "squid:S1192"})
@CucumberOptions(
    plugin = "json:target/cucumber-report.json",
    tags = {"@FitnoteDeclarationTest"}) // only run declaration tests to test encryption/decryption
public class RunEncryptedCukesTest {
  private static final String CONFIG_FILE = "test-crypto.yml";

  @ClassRule
  public static final DropwizardAppRule<FitnoteControllerConfiguration> RULE =
      new DropwizardAppRule<>(FitnoteControllerApplication.class, resourceFilePath(CONFIG_FILE));
}
