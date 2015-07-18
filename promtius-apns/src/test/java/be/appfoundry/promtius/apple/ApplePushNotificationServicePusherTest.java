package be.appfoundry.promtius.apple;


import be.appfoundry.promtius.ClientToken;
import be.appfoundry.promtius.ClientTokenFactory;
import be.appfoundry.promtius.ClientTokenService;
import be.appfoundry.promtius.PushPayload;
import com.google.common.collect.ImmutableSet;
import com.notnoop.apns.ApnsService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.exparity.hamcrest.date.DateMatchers.within;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Mike Seghers
 */
@RunWith(MockitoJUnitRunner.class)
public class ApplePushNotificationServicePusherTest {
    @Mock
    private ApnsService apnsService;

    @Mock
    private ClientTokenService<TestClientToken, String, String, String> clientTokenService;
    @Mock
    private ClientTokenFactory<TestClientToken, String, String> clientTokenFactory;

    @Captor
    ArgumentCaptor<TestClientToken> pushTokenCaptor;

    private static final String TEST_PLATFORM = "iOS";
    private TestClientToken tokenA;
    private TestClientToken tokenB;

    private ApplePushNotificationServicePusher<TestClientToken, String, String> pusher;

    @Before
    public void setUp() throws Exception {
        pusher = new ApplePushNotificationServicePusher<>(apnsService, clientTokenService, clientTokenFactory, TEST_PLATFORM);
        tokenA = new TestClientToken("token1");
        tokenB = new TestClientToken("token2");
    }

    @Test
    public void test_sendPush() throws Exception {
        PushPayload payload = new PushPayload.Builder().withMessage("message").withSound("sound").build();
        List<TestClientToken> tokens = Arrays.asList(tokenA, tokenB);
        when(clientTokenService.findClientTokensForOperatingSystem(TEST_PLATFORM)).thenReturn(tokens);

        pusher.sendPush(payload);

        verify(apnsService).push(eq(Arrays.asList("token1", "token2")), argThat(allOf(containsString("\"message\""), containsString("\"sound\""))));
    }

    @Test
    public void test_sendPush_setsCustomFieldsMap() throws Exception {
        final Map<String, Object> map = new HashMap<>();
        final Map<String, String> innerMap = new HashMap<>();
        map.put("custom", innerMap);
        PushPayload payload = new PushPayload.Builder().withMessage("message").withCustomFields(map).build();
        List<TestClientToken> tokens = Arrays.asList(tokenA, tokenB);
        when(clientTokenService.findClientTokensForOperatingSystem(TEST_PLATFORM)).thenReturn(tokens);

        pusher.sendPush(payload);

        verify(apnsService).push(eq(Arrays.asList("token1", "token2")), argThat(allOf(containsString("\"custom\":{}"))));
    }

    @Test
    public void test_sendPush_considersTTL() throws Exception {
        PushPayload payload = new PushPayload.Builder().withMessage("message").withTimeToLive(10).build();
        List<TestClientToken> tokens = Arrays.asList(tokenA, tokenB);
        when(clientTokenService.findClientTokensForOperatingSystem(TEST_PLATFORM)).thenReturn(tokens);

        pusher.sendPush(payload);

        Date ttlDate = new Date(System.currentTimeMillis() + 600000);
        verify(apnsService).push(eq(Arrays.asList("token1", "token2")), any(String.class), argThat(within(1, TimeUnit.SECONDS, ttlDate)));
    }

    @Test
    public void test_sendPushToGroup() throws Exception {
        PushPayload payload = new PushPayload.Builder().withMessage("message").build();
        List<TestClientToken> tokens = Arrays.asList(tokenA, tokenB);
        final Collection<String> groups = Arrays.asList("groupA", "groupB");
        when(clientTokenService.findClientTokensForOperatingSystem(TEST_PLATFORM, groups)).thenReturn(tokens);

        pusher.sendPush(payload, groups);

        verify(apnsService).push(eq(Arrays.asList("token1", "token2")), argThat(allOf(containsString("message"), containsString(PushPayload.DEFAULT_SOUND_VALUE))));
    }

    @Test
    public void test_removeInactiveDevices() throws Exception {
        Map<String, Date> inactive = new HashMap<String, Date>();
        inactive.put("token1", new Date());
        inactive.put("token2", new Date());
        when(apnsService.getInactiveDevices()).thenReturn(inactive);
        when(clientTokenFactory.createClientToken("token1", TEST_PLATFORM)).thenReturn(tokenA);
        when(clientTokenFactory.createClientToken("token2", TEST_PLATFORM)).thenReturn(tokenB);

        PushPayload payload = new PushPayload.Builder().withMessage("message").build();
        pusher.sendPush(payload);

        verify(clientTokenService, times(2)).unregisterClientToken(pushTokenCaptor.capture());
        List<TestClientToken> allValues = pushTokenCaptor.getAllValues();
        assertThat(allValues.get(0), is(tokenA));
        assertThat(allValues.get(1), is(tokenB));
    }

    @Test
    public void test_getPlatform() throws Exception {
        Set<String> singletonSet = ImmutableSet.of(TEST_PLATFORM);
        assertThat(pusher.getPlatforms(), is(singletonSet));
    }

    @Test
    public void test_whenUnregistrationFails_pushStillContinues() throws Exception {
        List<TestClientToken> tokens = Arrays.asList(tokenA, tokenB);
        when(clientTokenService.findClientTokensForOperatingSystem(TEST_PLATFORM)).thenReturn(tokens);
        when(apnsService.getInactiveDevices()).thenThrow(new RuntimeException());
        PushPayload payload = new PushPayload.Builder().withMessage("message").build();
        pusher.sendPush(payload);
        verify(apnsService).push(eq(Arrays.asList("token1", "token2")), argThat(allOf(containsString("message"), containsString(PushPayload.DEFAULT_SOUND_VALUE))));
    }

    private static class TestClientToken implements ClientToken<String, String> {

        private final String token;

        private TestClientToken(String token) {
            this.token = token;
        }

        @Override
        public String getToken() {
            return token;
        }

        @Override
        public String getPlatform() {
            return TEST_PLATFORM;
        }
    }
}
