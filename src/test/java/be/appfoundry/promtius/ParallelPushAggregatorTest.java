package be.appfoundry.promtius;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Arrays;
import java.util.HashSet;

import static org.hamcrest.Matchers.*;
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
        pushAggregator = new ParallelPushAggregator(new HashSet<Pusher>(Arrays.asList(pusherA, pusherB)));
    }

    @Test
    public void test_sendPush() throws Exception {
        PushPayload payload = new PushPayload("message");
        pushAggregator.sendPush(payload, tracker);
        waitUntillAgregatorHasFinishedAndVerify(payload);
    }



    @Test
    public void test_sendPush_failure() throws Exception {
        PushPayload payload = new PushPayload("message");
        doThrow(new IllegalStateException()).when(pusherA).sendPush(payload);
        doThrow(new IllegalStateException()).when(pusherB).sendPush(payload);
        pushAggregator.sendPush(payload, tracker);
        waitUntillAgregatorHasFinishedAndVerify(payload);
    }

    private void waitUntillAgregatorHasFinishedAndVerify(PushPayload payload) throws InterruptedException {
        while (!pushFinished) {
            Thread.sleep(1000);
        }

        verify(pusherA).sendPush(payload);
        verify(pusherB).sendPush(payload);
    }
}
