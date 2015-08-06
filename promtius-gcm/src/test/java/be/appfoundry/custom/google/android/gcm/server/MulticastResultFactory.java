package be.appfoundry.custom.google.android.gcm.server;

import java.util.List;

/**
 * This class is provided so we can use the {@link be.appfoundry.custom.google.android.gcm.server.Result.Builder}, which is package private.
 *
 * @author Mike Seghers
 */
public class MulticastResultFactory {
    public static MulticastResult getMulticastResultBuilder(int success, int failure, int canonicalIds, long multicastId, List<Result> results) {
        MulticastResult.Builder builder = new MulticastResult.Builder(success, failure, canonicalIds, multicastId);
        for (Result result : results) {
            builder.addResult(result);
        }
        return builder.build();
    }

    public static Result getResult(String canonicalId, String errorCode, String messageId) {
        return new Result.Builder().canonicalRegistrationId(canonicalId).errorCode(errorCode).messageId(messageId).build();
    }
}
