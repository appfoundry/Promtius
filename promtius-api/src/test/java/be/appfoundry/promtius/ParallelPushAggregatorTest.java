package be.appfoundry.promtius;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Arrays;
import java.util.HashSet;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
/**
 * @author Mike Seghers
 */
@RunWith(MockitoJUnitRunner.class)
public class ParallelPushAggregatorTest {
    private ParallelPushAggregator pushAggregator;

    @Mock
    private Pusher pusherA;

    @Mock
    private Pusher pusherB;

    private boolean pushFinished;

    private PusherAggregatorTracker tracker = new PusherAggregatorTracker() {
        @Override
        public void markFinished() {
            pushFinished = true;
        }
    };

    @Before
    public void setUp() throws Exception {
        pushAggregator = new ParallelPushAggregator(new HashSet<>(Arrays.asList(pusherA, pusherB)));
    }

    @Test
    public void test_sendPush() throws Exception {
        PushPayload payload = new PushPayload("message");
        pushAggregator.sendPush(payload, tracker);
        waitUntilAggregatorHasFinishedAndVerify(payload, 500);
    }

    @Test
    public void test_sendPush_failure() throws Exception {
        PushPayload payload = new PushPayload("message");
        doThrow(new IllegalStateException()).when(pusherA).sendPush(payload);
        doThrow(new IllegalStateException()).when(pusherB).sendPush(payload);
        pushAggregator.sendPush(payload, tracker);
        waitUntilAggregatorHasFinishedAndVerify(payload, 500);
    }

    private void waitUntilAggregatorHasFinishedAndVerify(PushPayload payload, long timeout) throws InterruptedException {
        long start = System.currentTimeMillis();
        long timePassed = 0;
        while (!pushFinished && timePassed < timeout) {
            Thread.sleep(100);
            timePassed = System.currentTimeMillis() - start;
        }

        if (!pushFinished) {
            fail("Aggregator didn't finish within timeout.");
        }

        verify(pusherA).sendPush(payload);
        verify(pusherB).sendPush(payload);
    }
}
