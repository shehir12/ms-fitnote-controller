package uk.gov.dwp.health.fitnotecontroller;

import cloud.localstack.docker.LocalstackDocker;
import cloud.localstack.docker.annotation.LocalstackDockerAnnotationProcessor;
import cloud.localstack.docker.annotation.LocalstackDockerConfiguration;
import cloud.localstack.docker.annotation.LocalstackDockerProperties;
import cucumber.api.CucumberOptions;
import cucumber.api.junit.Cucumber;
import org.junit.BeforeClass;
import uk.gov.dwp.health.fitnotecontroller.application.FitnoteControllerApplication;
import uk.gov.dwp.health.fitnotecontroller.application.FitnoteControllerConfiguration;
import io.dropwizard.testing.junit.DropwizardAppRule;
import org.junit.AfterClass;
import org.junit.ClassRule;
import org.junit.runner.RunWith;
import uk.gov.dwp.health.fitnotecontroller.redis.RedisTestClusterManager;
import java.io.IOException;

import static io.dropwizard.testing.ResourceHelpers.resourceFilePath;

@RunWith(Cucumber.class)
@SuppressWarnings({"squid:S2187", "squid:S1118", "squid:S4042"})
@LocalstackDockerProperties(services = {"sns", "sqs", "s3"}, pullNewImage = false)
@CucumberOptions(plugin = "json:target/cucumber-report.json", tags = {})
public class RunCukesTest {
    private static LocalstackDocker localstackDocker = LocalstackDocker.INSTANCE;

    static {
        LocalstackDockerConfiguration dockerConfig = new LocalstackDockerAnnotationProcessor().process(RunCukesTest.class);
        localstackDocker.startup(dockerConfig);
    }

    @BeforeClass
    public static void setup() throws IOException, InterruptedException {
        RedisTestClusterManager.startupRedisCluster();
    }

    @AfterClass
    public static void init() throws IOException, InterruptedException {
        RedisTestClusterManager.shutdownRedisCluster();
        localstackDocker.stop();
    }

    @ClassRule
    public static final DropwizardAppRule<FitnoteControllerConfiguration> RULE = new DropwizardAppRule<>(FitnoteControllerApplication.class, resourceFilePath("test.yml"));
}
