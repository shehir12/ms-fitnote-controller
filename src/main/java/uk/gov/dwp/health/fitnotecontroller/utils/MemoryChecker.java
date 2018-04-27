package uk.gov.dwp.health.fitnotecontroller.utils;

import org.slf4j.Logger;
import uk.gov.dwp.logging.DwpEncodedLogger;

public class MemoryChecker {
    private static final Logger LOG = DwpEncodedLogger.getLogger(MemoryChecker.class.getName());
    private static final long MEGABYTE = 1024L * 1024L;

    private MemoryChecker() {
    }

    public static boolean hasEnoughMemoryForRequest(Runtime javaRunTime, int requiredMemoryMb) {
        long freeMemoryMb = returnCurrentAvailableMemoryInMb(javaRunTime);
        int absoluteMemory = Math.abs(requiredMemoryMb);
        boolean isEnoughMemory = freeMemoryMb > absoluteMemory;

        LOG.info("Current available memory is {}mb, abs required memory is {}mb, request allowed = {}", freeMemoryMb, absoluteMemory, isEnoughMemory);
        return freeMemoryMb >= absoluteMemory;
    }

    public static long returnCurrentAvailableMemoryInMb(Runtime javaRunTime) {
        return javaRunTime.freeMemory() / MEGABYTE;
    }
}
