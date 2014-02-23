package be.appfoundry.promtius;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFutureTask;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Executors;

/**
 * @author Mike Seghers
 */
public class ParallelPushAggregator implements PushAggregator {
    private static final Logger LOGGER = LoggerFactory.getLogger(ParallelPushAggregator.class);


    private Set<Pusher> pusherRegistry;

    public ParallelPushAggregator(Set<Pusher> pusherRegistry) {
        this.pusherRegistry = new HashSet<>(pusherRegistry);
    }

    @Override
    public void sendPush(final PushPayload payload, final PusherAggregatorTracker tracker) {
        LOGGER.debug("Sending payload via parallel aggregator");
        ListeningExecutorService service = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(pusherRegistry.size()));
        createFutureTaskForEachPusherToSendPayload(payload, tracker, service);
        service.shutdown();
    }

    private void createFutureTaskForEachPusherToSendPayload(final PushPayload payload, final PusherAggregatorTracker tracker, final ListeningExecutorService service) {
        PusherAggregatorTaskCallback callback = new PusherAggregatorTaskCallback(tracker, pusherRegistry.size());
        for (final Pusher p : pusherRegistry) {
            LOGGER.debug("Setting up task for pusher {}", p);
            createListenableTaskAndExecuteForPusher(payload, service, callback, p);
        }
    }

    private void createListenableTaskAndExecuteForPusher(final PushPayload payload, final ListeningExecutorService service, final PusherAggregatorTaskCallback callback,
                                                         final Pusher p) {
        ListenableFutureTask<Boolean> task = ListenableFutureTask.create(new Runnable() {
            @Override
            public void run() {
                p.sendPush(payload);
            }
        }, Boolean.TRUE);
        Futures.addCallback(task, callback);
        service.execute(task);
    }

    private static final class PusherAggregatorTaskCallback implements FutureCallback<Boolean> {
        private static final Logger LOGGER = LoggerFactory.getLogger(PusherAggregatorTaskCallback.class);
        private int taskCount;
        private int successCount;
        private int failureCount;
        private PusherAggregatorTracker delegate;

        public PusherAggregatorTaskCallback(PusherAggregatorTracker delegate, int taskCount) {
            this.delegate = delegate;
            this.taskCount = taskCount;
        }

        @Override
        public synchronized void onSuccess(final Boolean result) {
            LOGGER.debug("Push success detected");
            successCount++;
            markDelegateWhenFinshed();
        }

        @Override
        public synchronized void onFailure(final Throwable t) {
            LOGGER.debug("Push failure received", t);
            failureCount++;
            markDelegateWhenFinshed();
        }

        private void markDelegateWhenFinshed() {
            if (isFinished()) {
                LOGGER.debug("Asking delegate to mark push aggregators as being finished.");
                delegate.markFinished();
            }
        }

        private boolean isFinished() {
            return taskCount == (successCount + failureCount);
        }


    }
}
