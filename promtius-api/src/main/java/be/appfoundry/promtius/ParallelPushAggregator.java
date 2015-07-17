package be.appfoundry.promtius;

import be.appfoundry.promtius.exception.PushFailedException;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFutureTask;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Executors;

/**
 * {@link PushAggregator} using {@link java.util.concurrent.FutureTask}s to push messages concurrently via injected {@link Pusher}s. Each given Pusher will get his own task. In
 * this way each pusher is run concurrently.
 *
 * @param <P> The platform identifier type, identifying the platform to which the pusher pushes its messages.
 * @param <G> The type of the group identifier. A group identifier is used to put client tokens in a collection of groups, so that a push can be done to specific groups.
 * @author Mike Seghers
 */
public class ParallelPushAggregator<P, G> implements PushAggregator<P, G> {
    private static final Logger LOGGER = LoggerFactory.getLogger(ParallelPushAggregator.class);

    private Set<Pusher<P, G>> pusherRegistry;

    public ParallelPushAggregator(final Set<Pusher<P, G>> pusherRegistry) {
        this.pusherRegistry = new HashSet<>(pusherRegistry);
    }

    @Override
    public void sendPush(final PushPayload payload, final PusherAggregatorTracker tracker) {
        LOGGER.debug("Sending payload via parallel aggregator");
        sendPushUsingStrategy(tracker, new AllPushStrategy<P, G>(payload), pusherRegistry);
    }

    @Override
    public void sendPush(final PushPayload payload, final Collection<G> groups, final PusherAggregatorTracker tracker) {
        LOGGER.debug("Sending payload via parallel aggregator for groups");
        sendPushUsingStrategy(tracker, new GroupPushStrategy<P, G>(payload, groups), pusherRegistry);
    }

    @Override
    public void sendPushToPlatforms(final PushPayload payload, final Collection<P> platforms, final PusherAggregatorTracker tracker) {
        LOGGER.debug("Sending payload via parallel aggregator to specific platforms");
        Set<Pusher<P, G>> pushers = getPushersForPlatformsOrFailTrying(platforms);
        sendPushUsingStrategy(tracker, new AllPushStrategy<P, G>(payload), pushers);
    }

    @Override
    public void sendPushToPlatforms(final PushPayload payload, final Collection<P> platforms, final Collection<G> groups, final PusherAggregatorTracker tracker) {
        LOGGER.debug("Sending payload via parallel aggregator to specific platforms for groups");
        Set<Pusher<P, G>> pushers = getPushersForPlatformsOrFailTrying(platforms);
        sendPushUsingStrategy(tracker, new GroupPushStrategy<P, G>(payload, groups), pushers);
    }

    private Set<Pusher<P, G>> getPushersForPlatformsOrFailTrying(final Collection<P> platforms) {
        Set<Pusher<P, G>> pushers = Sets.filter(pusherRegistry, new PlatformFilter(platforms));
        if (pushers.isEmpty()) {
            throw new PushFailedException("Could not find any pusher in the configured pusher registry for the given platforms (" + platforms + ")");
        }
        return pushers;
    }

    private void sendPushUsingStrategy(final PusherAggregatorTracker tracker, final PushStrategy<P, G> pushStrategy, final Set<Pusher<P, G>> pusherRegistry) {
        ListeningExecutorService service = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(pusherRegistry.size()));
        PusherAggregatorTaskCallback callback = new PusherAggregatorTaskCallback(tracker, pusherRegistry.size());
        for (final Pusher<P, G> p : pusherRegistry) {
            LOGGER.debug("Setting up task for pusher {}", p);
            createListenableTaskAndExecuteForPusher(service, callback, pushStrategy.newRunnableForPusher(p));
        }
        service.shutdown();
    }

    private void createListenableTaskAndExecuteForPusher(final ListeningExecutorService service, final PusherAggregatorTaskCallback callback, final Runnable runnable) {
        ListenableFutureTask<Boolean> task = ListenableFutureTask.create(runnable, Boolean.TRUE);
        Futures.addCallback(task, callback);
        service.execute(task);
    }

    private static final class PusherAggregatorTaskCallback implements FutureCallback<Boolean> {
        private static final Logger LOGGER = LoggerFactory.getLogger(PusherAggregatorTaskCallback.class);
        private int taskCount;
        private int successCount;
        private int failureCount;
        private PusherAggregatorTracker delegate;

        public PusherAggregatorTaskCallback(final PusherAggregatorTracker delegate, final int taskCount) {
            this.delegate = delegate;
            this.taskCount = taskCount;
        }

        @Override
        public synchronized void onSuccess(final Boolean result) {
            LOGGER.debug("Push success detected");
            successCount++;
            markDelegateWhenFinished();
        }

        @Override
        public synchronized void onFailure(final Throwable t) {
            LOGGER.error("Push failure received", t);
            failureCount++;
            markDelegateWhenFinished();
        }

        private void markDelegateWhenFinished() {
            if (isFinished()) {
                LOGGER.debug("Asking delegate to mark push aggregators as being finished.");
                delegate.markFinished();
            }
        }

        private boolean isFinished() {
            return taskCount == (successCount + failureCount);
        }
    }

    private interface PushStrategy<P, G> {
        Runnable newRunnableForPusher(Pusher<P, G> pusher);
    }

    private static class GroupPushStrategy<P, G> extends AllPushStrategy<P, G> {

        private Collection<G> groups;

        private GroupPushStrategy(final PushPayload payload, final Collection<G> groups) {
            super(payload);
            this.groups = groups;
        }

        @Override
        public Runnable newRunnableForPusher(final Pusher<P, G> pusher) {
            return new Runnable() {
                @Override
                public void run() {
                    pusher.sendPush(payload, groups);
                }
            };
        }
    }

    private static class AllPushStrategy<P, G> implements PushStrategy<P, G> {

        protected PushPayload payload;

        private AllPushStrategy(final PushPayload payload) {
            this.payload = payload;
        }

        @Override
        public Runnable newRunnableForPusher(final Pusher<P, G> pusher) {
            return new Runnable() {
                @Override
                public void run() {
                    pusher.sendPush(payload);
                }
            };
        }
    }

    private class PlatformFilter implements Predicate<Pusher<P, G>> {
        private Collection<P> platformsToKeep;

        public PlatformFilter(final Collection<P> platformsToKeep) {
            this.platformsToKeep = platformsToKeep;
        }

        @Override
        public boolean apply(final Pusher<P, G> input) {
            for (P platform : input.getPlatforms()) {
                if (platformsToKeep.contains(platform)) {
                    return true;
                }
            }
            return false;
        }
    }
}
