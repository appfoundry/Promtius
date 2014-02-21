package be.appfoundry.promtius;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFutureTask;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Executors;

/**
 * @author Mike Seghers
 */
public class ParallelPushAggregator implements PushAggregator {
    private Set<Pusher> pusherRegistry;

    public ParallelPushAggregator(Set<Pusher> pusherRegistry) {
        this.pusherRegistry = new HashSet<Pusher>(pusherRegistry);
    }

    @Override
    public void sendPush(final PushPayload payload, final PusherAggregatorTracker tracker) {
        ListeningExecutorService service = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(pusherRegistry.size()));
        createFutureTaskForEachPusherToSendPayload(payload, tracker, service);
        service.shutdown();
    }

    private void createFutureTaskForEachPusherToSendPayload(final PushPayload payload, final PusherAggregatorTracker tracker, final ListeningExecutorService service) {
        PusherAggregatorTaskCallback callback = new PusherAggregatorTaskCallback(tracker, pusherRegistry.size());
        for (final Pusher p : pusherRegistry) {
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
            successCount++;
            markDelegateWhenFinshed();
        }


        @Override
        public synchronized void onFailure(final Throwable t) {
            failureCount++;
            markDelegateWhenFinshed();
        }

        private void markDelegateWhenFinshed() {
            if (isFinished()) {
                delegate.markFinished();
            }
        }

        private boolean isFinished() {
            return taskCount == (successCount + failureCount);
        }


    }
}
