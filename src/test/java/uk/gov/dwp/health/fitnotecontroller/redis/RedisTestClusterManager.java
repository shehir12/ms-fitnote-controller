package uk.gov.dwp.health.fitnotecontroller.redis;

import com.googlecode.junittoolbox.PollingWait;
import com.googlecode.junittoolbox.RunnableAssert;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.Assert.assertTrue;

public class RedisTestClusterManager {
    private static final String DOCKER_IMAGE_NAME = "nexus.service.health-dev.dwpcloud.uk:5000/grokzen/redis-cluster";
    private static final Logger LOG = LoggerFactory.getLogger(RedisTestClusterManager.class.getName());

    private static final String[] DOCKER_RUN_COMMAND = {"docker", "run", "--rm", "--name", "redis_test_integ", "-p", "7000-7005:7000-7005", "--env", "CLUSTER_ONLY=true", "--env", "IP=0.0.0.0", DOCKER_IMAGE_NAME};
    private static final String[] DOCKER_STOP_COMMAND = {"docker", "stop", "redis_test_integ"};
    private static File outputFile = new File("src/test/resources/redisProcess.log");

    private RedisTestClusterManager() {
        // prevent instantiation
    }

    public static void startupRedisCluster() throws IOException {
        ProcessBuilder runProcess = new ProcessBuilder(commandBuilder(DOCKER_RUN_COMMAND));
        LOG.info("running docker start for redis-cluster");

        runProcess.redirectOutput(ProcessBuilder.Redirect.to(outputFile));
        runProcess.redirectErrorStream(true);
        runProcess.start();

        checkRedisUp(outputFile);
    }

    public static void shutdownRedisCluster() throws IOException, InterruptedException {
        ProcessBuilder stop = new ProcessBuilder(commandBuilder(DOCKER_STOP_COMMAND));
        LOG.info("shutting down redis-cluster 'redis_test_integ'");
        Process terminate = stop.start();
        terminate.waitFor();
    }

    private static void checkRedisUp(File output) throws IOException {
        PollingWait wait = new PollingWait().timeoutAfter(30, SECONDS).pollEvery(1, SECONDS);

        if (FileUtils.readFileToString(output, StandardCharsets.UTF_8.toString()).contains("The container name \"/redis_test_integ\" is already in use")) {
            throw new IOException("The container name \"/redis_test_integ\" is already in use, docker will shutdown the container and exit, ready for re-run");
        }

        wait.until(new RunnableAssert("waiting for redis cluster to complete startup & initialise") {
            @Override
            public void run() throws Exception {
                LOG.info("checking redis cluster status...");
                assertTrue("checking redis cluster status", FileUtils.readFileToString(output, StandardCharsets.UTF_8.toString()).contains("/var/log/supervisor/redis-6.log"));

                LOG.info("redis cluster is 'up'.  pausing 3 seconds for clustered nodes to accept connections");
                TimeUnit.SECONDS.sleep(3); // to ensure all ports are open for connections
                LOG.info("started");
            }
        });
    }

    private static String[] commandBuilder(String[] input) {
        String[] fullCommand;

        if (System.getProperty("os.name").toLowerCase().startsWith("xxwindows")) {
            fullCommand = new String[input.length + 2];
            System.arraycopy(input, 0, fullCommand, 2, input.length);

            fullCommand[0] = "cmd.exe";
            fullCommand[1] = "/c";

        } else {
            fullCommand = input;
        }

        return fullCommand;
    }
}
