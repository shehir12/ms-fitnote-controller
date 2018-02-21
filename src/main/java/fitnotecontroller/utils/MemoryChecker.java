package fitnotecontroller.utils;

import gov.dwp.utilities.logging.DwpEncodedLogger;
import org.apache.log4j.Logger;

public class MemoryChecker {
    private static final Logger LOG = DwpEncodedLogger.getLogger(MemoryChecker.class.getName());
    private static final long MEGABYTE = 1024L * 1024L;

    private MemoryChecker() {
    }

    public static boolean hasEnoughMemoryForRequest(Runtime javaRunTime, int requiredMemoryMb) {
        long freeMemoryMb = returnCurrentAvailableMemoryInMb(javaRunTime);
        int absoluteMemory = Math.abs(requiredMemoryMb);

        LOG.info(String.format("Current available memory is %dmb, abs required memory is %dmb, request %s", freeMemoryMb, absoluteMemory, freeMemoryMb > absoluteMemory ? "ALLOWED" : "REJECTED"));
        return freeMemoryMb >= absoluteMemory;
    }

    public static long returnCurrentAvailableMemoryInMb(Runtime javaRunTime) {
        return javaRunTime.freeMemory() / MEGABYTE;
    }
}
