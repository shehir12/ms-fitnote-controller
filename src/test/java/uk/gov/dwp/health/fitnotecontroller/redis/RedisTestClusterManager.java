package uk.gov.dwp.health.fitnotecontroller.redis;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class RedisTestClusterManager {
    private static final String DOCKER_IMAGE_NAME = "nexus.mgmt.health-dev.dwpcloud.uk:5000/grokzen/redis-cluster";
    private static final Logger LOG = LoggerFactory.getLogger(RedisTestClusterManager.class.getName());

    private static final String[] DOCKER_RUN_COMMAND = {"docker", "run", "--rm", "--name", "redis_test_integ", "-p", "7000-7005:7000-7005", "--env", "CLUSTER_ONLY=true", "--env", "IP=0.0.0.0", DOCKER_IMAGE_NAME};
    private static final String[] DOCKER_STOP_COMMAND = {"docker", "stop",  "redis_test_integ"};
    private static File outputFile = new File("src/test/resources/redisProcess.log");

    private RedisTestClusterManager() {
        // prevent instantiation
    }

    public static void startupRedisCluster() throws IOException, InterruptedException {
        ProcessBuilder runProcess = new ProcessBuilder(commandBuilder(DOCKER_RUN_COMMAND));
        LOG.info("running docker start for redis-cluster");

        runProcess.redirectOutput(ProcessBuilder.Redirect.to(outputFile));
        runProcess.redirectErrorStream(true);
        Process start = runProcess.start();

        start.waitFor(5, TimeUnit.SECONDS);
        checkRedisUp(outputFile);
    }

    public static void shutdownRedisCluster() throws IOException, InterruptedException {
        ProcessBuilder stop = new ProcessBuilder(commandBuilder(DOCKER_STOP_COMMAND));
        LOG.info("shutting down redis-cluster 'redis_test_integ'");
        Process terminate = stop.start();
        terminate.waitFor();
    }

    private static void checkRedisUp(File output) throws IOException, InterruptedException {
        long startTime = System.currentTimeMillis();
        boolean processStarted = false;

        if (FileUtils.readFileToString(output, "UTF-8").contains("The container name \"/redis_test_integ\" is already in use")) {
            throw new IOException("The container name \"/redis_test_integ\" is already in use, docker will shutdown the container and exit, ready for re-run");
        }

        while ((System.currentTimeMillis() - startTime) < 30000) {
            LOG.info("redis-cluster up and initialising...  checking output {} for 30 seconds; {} seconds elapsed", output.getName(), (System.currentTimeMillis() - startTime)/1000);
            if (FileUtils.readFileToString(output, "UTF-8").contains("/var/log/supervisor/redis-6.log")) {
                Thread.sleep(3000);
                processStarted = true;
                LOG.info("started");
                break;
            }

            Thread.sleep(1000);
        }

        if (!processStarted) {
            throw new IOException(FileUtils.readFileToString(output, "UTF-8"));
        }
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
