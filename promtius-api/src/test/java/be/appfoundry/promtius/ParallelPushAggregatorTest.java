package be.appfoundry.promtius;

import be.appfoundry.promtius.exception.PushFailedException;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;

import static org.hamcrest.Matchers.anything;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
/**
 * @author Mike Seghers
 */
@RunWith(MockitoJUnitRunner.class)
public class ParallelPushAggregatorTest {
    private ParallelPushAggregator<String, String> pushAggregator;

    @Mock
    private Pusher<String, String> pusherA;

    @Mock
    private Pusher<String, String> pusherB;

    private boolean pushFinished;
    private PushPayload simpleMessagePayload;

    private PusherAggregatorTracker tracker = new PusherAggregatorTracker() {
        @Override
        public void markFinished() {
            pushFinished = true;
        }
    };

    @Before
    public void setUp() throws Exception {
        pushAggregator = new ParallelPushAggregator<>(new HashSet<>(Arrays.asList(pusherA, pusherB)));
        simpleMessagePayload = new PushPayload.Builder().withMessage("message").build();
    }

    @Test
    public void test_sendPush() throws Exception {
        pushAggregator.sendPush(simpleMessagePayload, tracker);
        waitUntilAggregatorHasFinished(simpleMessagePayload, 500);
        verify(pusherA).sendPush(simpleMessagePayload);
        verify(pusherB).sendPush(simpleMessagePayload);
    }

    @Test
    public void test_sendPushToSpecificPlatform() throws Exception {
        when(pusherA.getPlatforms()).thenReturn(Sets.newHashSet("IOS"));
        pushAggregator.sendPushToPlatforms(simpleMessagePayload, Lists.newArrayList("IOS"), tracker);
        waitUntilAggregatorHasFinished(simpleMessagePayload, 500);
        verify(pusherA).sendPush(simpleMessagePayload);
        verify(pusherB, never()).sendPush(simpleMessagePayload);
    }

    @Test
    public void test_sendPushToSpecificPlatformWithGroups() throws Exception {
        final Collection<String> groups = Arrays.asList("groupA", "groupB");
        when(pusherA.getPlatforms()).thenReturn(Sets.newHashSet("IOS"));
        pushAggregator.sendPushToPlatforms(simpleMessagePayload, Lists.newArrayList("IOS"), groups, tracker);
        waitUntilAggregatorHasFinished(simpleMessagePayload, 500);
        verify(pusherA).sendPush(simpleMessagePayload, groups);
        verify(pusherB, never()).sendPush(Mockito.any(PushPayload.class), Mockito.<Collection<String>>any());
    }

    @Test
    public void test_sendPushToGroup() throws Exception {
        final Collection<String> groups = Arrays.asList("groupA", "groupB");
        pushAggregator.sendPush(simpleMessagePayload, groups, tracker);
        waitUntilAggregatorHasFinished(simpleMessagePayload, 500);
        verify(pusherA).sendPush(simpleMessagePayload, groups);
        verify(pusherB).sendPush(simpleMessagePayload, groups);
    }

    @Test
    public void test_sendPush_failure() throws Exception {
        doThrow(new IllegalStateException()).when(pusherA).sendPush(simpleMessagePayload);
        doThrow(new IllegalStateException()).when(pusherB).sendPush(simpleMessagePayload);
        pushAggregator.sendPush(simpleMessagePayload, tracker);
        waitUntilAggregatorHasFinished(simpleMessagePayload, 500);
        verify(pusherA).sendPush(simpleMessagePayload);
        verify(pusherB).sendPush(simpleMessagePayload);
    }

    @Test(expected = PushFailedException.class)
    public void test_sendPushToUnknownPlatformsResultsInPushFailedException() throws Exception {
        pushAggregator.sendPushToPlatforms(simpleMessagePayload, Lists.newArrayList("IOS"), tracker);
    }

    @Test(expected = PushFailedException.class)
    public void test_sendPushForGroupsToUnknownPlatformsResultsInPushFailedException() throws Exception {
        final Collection<String> groups = Arrays.asList("groupA", "groupB");
        pushAggregator.sendPushToPlatforms(simpleMessagePayload, Lists.newArrayList("IOS"), groups, tracker);
    }

    private void waitUntilAggregatorHasFinished(PushPayload payload, long timeout) throws InterruptedException {
        long start = System.currentTimeMillis();
        long timePassed = 0;
        while (!pushFinished && timePassed < timeout) {
            Thread.sleep(100);
            timePassed = System.currentTimeMillis() - start;
        }

        if (!pushFinished) {
            fail("Aggregator didn't finish within timeout.");
        }
    }
}
