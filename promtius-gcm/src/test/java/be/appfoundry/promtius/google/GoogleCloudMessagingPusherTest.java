package be.appfoundry.promtius.google;

import be.appfoundry.promtius.ClientToken;
import be.appfoundry.promtius.ClientTokenFactory;
import be.appfoundry.promtius.ClientTokenService;
import be.appfoundry.promtius.PushPayload;
import be.appfoundry.promtius.exception.PushFailedException;
import com.google.android.gcm.server.Constants;
import com.google.android.gcm.server.Message;
import com.google.android.gcm.server.MulticastResult;
import com.google.android.gcm.server.Result;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyList;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import static com.google.android.gcm.server.MulticastResultFactory.getMulticastResultBuilder;
import static com.google.android.gcm.server.MulticastResultFactory.getResult;

/**
 * @author Mike Seghers
 */
@RunWith(MockitoJUnitRunner.class)
public class GoogleCloudMessagingPusherTest {
    private GoogleCloudMessagingPusher<String> pusher;

    @Mock
    private ClientTokenService<String, String> clientTokenService;

    @Mock
    private GoogleSenderWrapper wrapper;
    private static final String TEST_PLATFORM = "Android";
    @Mock
    private ClientToken<String, String> tokenA;
    @Mock
    private ClientToken<String, String> tokenB;
    @Mock
    private ClientTokenFactory<String, String> clientTokenFactory;

    @Before
    public void setUp() throws Exception {
        pusher = new GoogleCloudMessagingPusher<String>(wrapper, clientTokenService, clientTokenFactory, TEST_PLATFORM);
    }

    @Test
    public void test_sendPush() throws Exception {
        PushPayload payload = new PushPayload("message");
        List<ClientToken<String, String>> tokens = Arrays.asList(tokenA, tokenB);
        when(clientTokenService.findClientTokensForOperatingSystem(TEST_PLATFORM)).thenReturn(tokens);
        when(tokenA.getToken()).thenReturn("token1");
        when(tokenB.getToken()).thenReturn("token2");
        pusher.sendPush(payload);

        ArgumentCaptor<Message> messageCaptor = ArgumentCaptor.forClass(Message.class);
        ArgumentCaptor<List> deviceIdCaptor = ArgumentCaptor.<List>forClass(List.class);
        verify(wrapper).send(messageCaptor.capture(), deviceIdCaptor.capture(), eq(5));

        Message message = messageCaptor.getValue();
        assertThat(message.getData().get("message"), is("message"));
        assertThat(message.getCollapseKey(), is("kolepski"));
        assertThat(message.getTimeToLive(), is(nullValue()));

        List<String> deviceIds = deviceIdCaptor.getValue();
        assertThat(deviceIds, hasSize(2));
        assertThat(deviceIds, hasItems("token1", "token2"));
    }

    @Test(expected = PushFailedException.class)
    public void test_sendPush_onIOException() throws Exception {
        PushPayload payload = new PushPayload("message");
        List<ClientToken<String, String>> tokens = Arrays.asList(tokenA, tokenB);
        when(clientTokenService.findClientTokensForOperatingSystem(TEST_PLATFORM)).thenReturn(tokens);
        when(wrapper.send(Mockito.any(Message.class), anyList(), anyInt())).thenThrow(new IOException());

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

        pusher.sendPush(new PushPayload("message"));

        ArgumentCaptor<List> deviceIdCaptor = ArgumentCaptor.forClass(List.class);
        verify(wrapper, times(3)).send(Mockito.any(Message.class), deviceIdCaptor.capture(), anyInt());
        List<List> values = deviceIdCaptor.getAllValues();
        assertThat(values, hasSize(3));
        assertThat(values.get(0).size(), is(1000));
        assertThat(values.get(1).size(), is(1000));
        assertThat(values.get(2).size(), is(500));
    }

    @Test
    public void test_multicastReturnEvaluated_cannonicalReplacement() throws Exception {
        List<ClientToken<String, String>> tokens = Arrays.asList(tokenA);
        List<Result> results = Arrays.asList(getResult("newToken", "err", "1"));
        MulticastResult expected = getMulticastResultBuilder(50, 50, 20, 1, results);

        when(tokenA.getToken()).thenReturn("oldToken");
        when(clientTokenService.findClientTokensForOperatingSystem(TEST_PLATFORM)).thenReturn(tokens);
        when(wrapper.send(Mockito.any(Message.class), anyList(), anyInt())).thenReturn(expected);
        when(clientTokenFactory.createClientToken("oldToken", TEST_PLATFORM)).thenReturn(tokenB);
        when(clientTokenFactory.createClientToken("newToken", TEST_PLATFORM)).thenReturn(tokenA);

        pusher.sendPush(new PushPayload("message"));

        ArgumentCaptor<ClientToken> oldTokenCaptor = ArgumentCaptor.forClass(ClientToken.class);
        verify(clientTokenService).unregisterClientToken(oldTokenCaptor.capture());
        assertThat((ClientToken<String, String>)oldTokenCaptor.getValue(), is(tokenB));
        ArgumentCaptor<ClientToken> tokenCaptor = ArgumentCaptor.forClass(ClientToken.class);
        verify(clientTokenService).registerClientToken(tokenCaptor.capture());
        ClientToken<String, String> savedToken = tokenCaptor.getValue();
        assertThat(savedToken, is(tokenA));
    }

    @Test
    public void test_multicastReturnEvaluated_removal() throws Exception {
        List<ClientToken<String, String>> tokens = Arrays.asList(tokenA);
        List<Result> results = Arrays.asList(getResult(null, Constants.ERROR_NOT_REGISTERED, null));
        MulticastResult expected = getMulticastResultBuilder(50, 50, 20, 1, results);

        when(tokenA.getToken()).thenReturn("oldToken");
        when(clientTokenService.findClientTokensForOperatingSystem(TEST_PLATFORM)).thenReturn(tokens);
        when(wrapper.send(Mockito.any(Message.class), anyList(), anyInt())).thenReturn(expected);
        when(clientTokenFactory.createClientToken("oldToken", TEST_PLATFORM)).thenReturn(tokenB);

        pusher.sendPush(new PushPayload("message"));

        ArgumentCaptor<ClientToken> oldTokenCaptor = ArgumentCaptor.forClass(ClientToken.class);
        verify(clientTokenService).unregisterClientToken(oldTokenCaptor.capture());
        assertThat((ClientToken<String, String>)oldTokenCaptor.getValue(), is(tokenB));

        verify(clientTokenService, never()).registerClientToken(Mockito.any(ClientToken.class));
    }

    @Test
    public void test_getPlatform() throws Exception {
        assertThat(pusher.getPlatform(), is(TEST_PLATFORM));
    }
}
