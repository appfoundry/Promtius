package be.appfoundry.promtius.google;

import be.appfoundry.custom.google.android.gcm.server.Constants;
import be.appfoundry.custom.google.android.gcm.server.Message;
import be.appfoundry.custom.google.android.gcm.server.MulticastResult;
import be.appfoundry.custom.google.android.gcm.server.Result;
import be.appfoundry.promtius.ClientToken;
import be.appfoundry.promtius.ClientTokenFactory;
import be.appfoundry.promtius.ClientTokenService;
import be.appfoundry.promtius.PushPayload;
import be.appfoundry.promtius.exception.PushFailedException;
import com.google.common.collect.ImmutableSet;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static be.appfoundry.custom.google.android.gcm.server.MulticastResultFactory.getMulticastResultBuilder;
import static be.appfoundry.custom.google.android.gcm.server.MulticastResultFactory.getResult;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyListOf;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Mike Seghers
 */
@RunWith(MockitoJUnitRunner.class)
public class GoogleCloudMessagingPusherTest {
    private static final String TEST_PLATFORM = "Android";

    private GoogleCloudMessagingPusher<ClientToken<String, String>, String, String> pusher;
    @Mock
    private ClientTokenService<ClientToken<String, String>, String, String, String> clientTokenService;
    @Mock
    private GoogleSenderWrapper wrapper;
    @Mock
    private ClientToken<String, String> tokenA;
    @Mock
    private ClientToken<String, String> tokenB;
    @Mock
    private ClientTokenFactory<ClientToken<String, String>, String, String> clientTokenFactory;

    @Captor
    private ArgumentCaptor<List<String>> deviceIdCaptor;
    @Captor
    private ArgumentCaptor<ClientToken<String, String>> tokenCaptor;

    private PushPayload payload;

    @Before
    public void setUp() throws Exception {
        pusher = new GoogleCloudMessagingPusher<>(wrapper, clientTokenService, clientTokenFactory, TEST_PLATFORM);
        payload = new PushPayload.Builder().withMessage("message").build();
    }

    @Test
    public void test_sendPush() throws Exception {
        PushPayload payload = new PushPayload.Builder().withMessage("message").withSound("sound").build();
        List<ClientToken<String, String>> tokens = Arrays.asList(tokenA, tokenB);
        when(clientTokenService.findClientTokensForOperatingSystem(TEST_PLATFORM)).thenReturn(tokens);
        when(tokenA.getToken()).thenReturn("token1");
        when(tokenB.getToken()).thenReturn("token2");
        pusher.sendPush(payload);

        ArgumentCaptor<Message> messageCaptor = ArgumentCaptor.forClass(Message.class);
        verify(wrapper).send(messageCaptor.capture(), deviceIdCaptor.capture(), eq(5));

        Message message = messageCaptor.getValue();
        assertThat(message.getData().get("message"), is((Object)"message"));
        assertThat(message.getData().get("sound"), is((Object)"sound"));
        assertThat(message.getTimeToLive(), is(nullValue()));

        List<String> deviceIds = deviceIdCaptor.getValue();
        assertThat(deviceIds, hasSize(2));
        assertThat(deviceIds, hasItems("token1", "token2"));
    }

    @Test
    public void test_sendPushToGroup() throws Exception {
        List<ClientToken<String, String>> tokens = Arrays.asList(tokenA, tokenB);
        when(clientTokenService.findClientTokensForOperatingSystem(TEST_PLATFORM)).thenReturn(tokens);
        when(tokenA.getToken()).thenReturn("token1");
        when(tokenB.getToken()).thenReturn("token2");
        final Collection<String> groups = Arrays.asList("groupA", "groupB");
        when(clientTokenService.findClientTokensForOperatingSystem(TEST_PLATFORM, groups)).thenReturn(tokens);

        pusher.sendPush(payload, groups);

        ArgumentCaptor<Message> messageCaptor = ArgumentCaptor.forClass(Message.class);
        verify(wrapper).send(messageCaptor.capture(), deviceIdCaptor.capture(), eq(5));

        Message message = messageCaptor.getValue();
        assertThat(message.getData().get("message"), is((Object)"message"));
        assertThat(message.getData().get("sound"), is((Object)PushPayload.DEFAULT_SOUND_VALUE));
        assertThat(message.getTimeToLive(), is(nullValue()));

        List<String> deviceIds = deviceIdCaptor.getValue();
        assertThat(deviceIds, hasSize(2));
        assertThat(deviceIds, hasItems("token1", "token2"));
    }

    @Test
    public void test_sendPush_setsCustomFieldsMap() throws Exception {
        final List<ClientToken<String, String>> tokens = Arrays.asList(tokenA, tokenB);
        final Map<String, Object> map = new HashMap<>();
        final Map<String, String> innerMap = new HashMap<>();
        map.put("custom", innerMap);
        PushPayload payload = new PushPayload.Builder().withMessage("message").withCustomFields(map).build();

        when(clientTokenService.findClientTokensForOperatingSystem(TEST_PLATFORM)).thenReturn(tokens);
        pusher.sendPush(payload);

        ArgumentCaptor<Message> messageCaptor = ArgumentCaptor.forClass(Message.class);
        verify(wrapper).send(messageCaptor.capture(), anyListOf(String.class), any(Integer.class));

        Message message = messageCaptor.getValue();
        assertThat(message.getData().get("data"), is(equalTo((Object) map)));
    }

    @Test
    public void test_sendPush_considersTTL() throws Exception {
        final List<ClientToken<String, String>> tokens = Arrays.asList(tokenA, tokenB);
        when(clientTokenService.findClientTokensForOperatingSystem(TEST_PLATFORM)).thenReturn(tokens);
        PushPayload payload = new PushPayload.Builder().withMessage("message").withTimeToLive(10).build();
        pusher.sendPush(payload);


        ArgumentCaptor<Message> messageCaptor = ArgumentCaptor.forClass(Message.class);
        verify(wrapper).send(messageCaptor.capture(), anyListOf(String.class), any(Integer.class));
        Message message = messageCaptor.getValue();
        assertThat(message.getTimeToLive(), is(equalTo(600)));
    }

    @Test
    public void test_sendPush_setDiscrimimatorAsCollapseKey() throws Exception {
        final List<ClientToken<String, String>> tokens = Arrays.asList(tokenA, tokenB);
        when(clientTokenService.findClientTokensForOperatingSystem(TEST_PLATFORM)).thenReturn(tokens);
        pusher.sendPush(payload);


        ArgumentCaptor<Message> messageCaptor = ArgumentCaptor.forClass(Message.class);
        verify(wrapper).send(messageCaptor.capture(), anyListOf(String.class), any(Integer.class));
        Message message = messageCaptor.getValue();
        assertThat(message.getCollapseKey(), is(equalTo(PushPayload.DEFAULT_DISCRIMINATOR_VALUE)));
    }

    @Test(expected = PushFailedException.class)
    public void test_sendPush_onIOException() throws Exception {
        PushPayload payload = new PushPayload.Builder().withMessage("message").build();
        List<ClientToken<String, String>> tokens = Arrays.asList(tokenA, tokenB);
        when(clientTokenService.findClientTokensForOperatingSystem(TEST_PLATFORM)).thenReturn(tokens);
        when(wrapper.send(Mockito.any(Message.class), anyListOf(String.class), anyInt())).thenThrow(new IOException());

        pusher.sendPush(payload);
    }

    @Test
    public void test_multicastSend() throws Exception {
        List<ClientToken<String, String>> tokens = new ArrayList<>(2500);
        for (int i = 0; i < 2500; i++) {
            tokens.add(tokenA);
        }
        when(tokenA.getToken()).thenReturn("token");
        when(clientTokenService.findClientTokensForOperatingSystem(TEST_PLATFORM)).thenReturn(tokens);

        pusher.sendPush(payload);


        verify(wrapper, times(3)).send(Mockito.any(Message.class), deviceIdCaptor.capture(), anyInt());
        List<List<String>> values = deviceIdCaptor.getAllValues();
        assertThat(values, hasSize(3));
        assertThat(values.get(0), hasSize(1000));
        assertThat(values.get(1), hasSize(1000));
        assertThat(values.get(2), hasSize(500));
    }

    @Test
    public void test_multicastReturnEvaluated_cannonicalReplacement() throws Exception {
        List<ClientToken<String, String>> tokens = Collections.singletonList(tokenA);
        List<Result> results = Collections.singletonList(getResult("newToken", "err", "1"));
        MulticastResult expected = getMulticastResultBuilder(50, 50, 20, 1, results);

        when(tokenA.getToken()).thenReturn("oldToken");
        when(clientTokenService.findClientTokensForOperatingSystem(TEST_PLATFORM)).thenReturn(tokens);
        when(wrapper.send(Mockito.any(Message.class), anyListOf(String.class), anyInt())).thenReturn(expected);
        when(clientTokenFactory.createClientToken("oldToken", TEST_PLATFORM)).thenReturn(tokenB);
        when(clientTokenFactory.createClientToken("newToken", TEST_PLATFORM)).thenReturn(tokenA);

        pusher.sendPush(payload);

        verify(clientTokenService).unregisterClientToken(tokenCaptor.capture());
        assertThat(tokenCaptor.getValue(), is(tokenB));
        verify(clientTokenService).registerClientToken(tokenCaptor.capture());
        assertThat(tokenCaptor.getValue(), is(tokenA));
    }

    @Test
    public void test_multicastReturnEvaluated_removal() throws Exception {
        List<ClientToken<String, String>> tokens = Collections.singletonList(tokenA);
        List<Result> results = Collections.singletonList(getResult(null, Constants.ERROR_NOT_REGISTERED, null));
        MulticastResult expected = getMulticastResultBuilder(50, 50, 20, 1, results);

        when(tokenA.getToken()).thenReturn("oldToken");
        when(clientTokenService.findClientTokensForOperatingSystem(TEST_PLATFORM)).thenReturn(tokens);
        when(wrapper.send(Mockito.any(Message.class), anyListOf(String.class), anyInt())).thenReturn(expected);
        when(clientTokenFactory.createClientToken("oldToken", TEST_PLATFORM)).thenReturn(tokenB);

        pusher.sendPush(payload);

        verify(clientTokenService).unregisterClientToken(tokenCaptor.capture());
        assertThat(tokenCaptor.getValue(), is(tokenB));
        verify(clientTokenService, never()).registerClientToken(Mockito.<ClientToken<String, String>>any());
    }

    @Test
    public void test_getPlatform() throws Exception {
        Set<String> singletonSet = ImmutableSet.of(TEST_PLATFORM);
        assertThat(pusher.getPlatforms(), is(singletonSet));
    }
}
