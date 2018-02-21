package fitnotecontroller.utils;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class MemoryCheckerTest {
    private static final long MEGABYTE = 1024L * 1024L;

    @Mock
    private Runtime runTimeMock;

    @Test
    public void validateMemoryAvailableIsEnough() {
        when(runTimeMock.freeMemory()).thenReturn(12 * MEGABYTE);

        assertThat("should allow request", MemoryChecker.hasEnoughMemoryForRequest(runTimeMock, 10), is(equalTo(true)));
    }

    @Test
    public void validateMemoryAvailableIsNotEnough() {
        when(runTimeMock.freeMemory()).thenReturn(8 * MEGABYTE);

        assertThat("should reject request", MemoryChecker.hasEnoughMemoryForRequest(runTimeMock, 10), is(equalTo(false)));
    }

    @Test
    public void testWhenParameterIsNegative() {
        when(runTimeMock.freeMemory()).thenReturn(8 * MEGABYTE);

        assertThat("should reject request", MemoryChecker.hasEnoughMemoryForRequest(runTimeMock, -10), is(equalTo(false)));
    }
}