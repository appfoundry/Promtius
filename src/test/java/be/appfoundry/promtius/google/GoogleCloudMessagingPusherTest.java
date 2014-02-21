package be.appfoundry.promtius.google;

import be.appfoundry.promtius.ClientToken;
import be.appfoundry.promtius.ClientTokenService;
import be.appfoundry.promtius.ClientTokenType;
import be.appfoundry.promtius.PushPayload;
import be.appfoundry.promtius.exception.PushFailedException;
import com.google.android.gcm.server.Message;
import org.junit.Before;
import org.junit.Ignore;
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
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

//import static com.google.android.gcm.server.MulticastResultFactory.getMulticastResultBuilder;
//import static com.google.android.gcm.server.MulticastResultFactory.getResult;

/**
 * @author Mike Seghers
 */
@RunWith(MockitoJUnitRunner.class)
public class GoogleCloudMessagingPusherTest {
    private GoogleCloudMessagingPusher pusher;

    @Mock
    private ClientTokenService clientTokenService;

    @Mock
    private GoogleSenderWrapper wrapper;
    @Mock
    private ClientTokenType clientTokenType;
    @Mock
    private ClientToken<String> tokenA;
    @Mock
    private ClientToken<String> tokenB;

    @Before
    public void setUp() throws Exception {
        pusher = new GoogleCloudMessagingPusher(wrapper, clientTokenService, clientTokenType);
    }

    @Test
    public void test_sendPush() throws Exception {
        PushPayload payload = new PushPayload("message");
        List<ClientToken<String>> tokens = Arrays.asList(tokenA, tokenB);
        when(clientTokenService.findClientTokensForOperatingSystem(clientTokenType)).thenReturn(tokens);
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
        List<ClientToken<String>> tokens = Arrays.asList(tokenA, tokenB);
        when(clientTokenService.findClientTokensForOperatingSystem(clientTokenType)).thenReturn(tokens);
        when(wrapper.send(Mockito.any(Message.class), anyList(), anyInt())).thenThrow(new IOException());

        pusher.sendPush(payload);
    }

    @Test
    public void test_multicastSend() throws Exception {
        List<ClientToken<String>> tokens = new ArrayList<ClientToken<String>>(2500);
        for (int i = 0; i < 2500; i++) {
            tokens.add(tokenA);
        }
        when(clientTokenService.findClientTokensForOperatingSystem(clientTokenType)).thenReturn(tokens);

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
    @Ignore
    public void test_multicastReturnEvaluated_cannonicalReplacement() throws Exception {
        /*List<ClientToken<String>> tokens = Arrays.asList(tokenA);
        List<Result> results = Arrays.asList(getResult("newToken", "err", "1"));
        MulticastResult expected = getMulticastResultBuilder(50, 50, 20, 1, results);

        when(clientTokenService.findPushTokensForOperatingSystem(PushToken.OperatingSystem.ANDROID)).thenReturn(tokens);
        when(wrapper.send(Mockito.any(Message.class), anyList(), anyInt())).thenReturn(expected);

        pusher.sendPush(new PushPayload("message"));

        ArgumentCaptor<ClientToken> oldTokenCaptor = ArgumentCaptor.forClass(ClientToken.class);
        verify(clientTokenService).unregisterClientToken(oldTokenCaptor.capture());
        assertThat(oldTokenCaptor.getValue(), is(tokenA));
        ArgumentCaptor<ClientToken> tokenCaptor = ArgumentCaptor.forClass(ClientToken.class);
        verify(clientTokenService).registerClientToken(tokenCaptor.capture());
        ClientToken savedToken = tokenCaptor.getValue();
        assertThat((String) savedToken.getToken(), is("newToken"));
        assertThat(savedToken.getClientTokenType(), is(clientTokenType));*/
    }

    @Test
    @Ignore
    public void test_multicastReturnEvaluated_removal() throws Exception {
        /*ClientToken<String> oldToken = tokenA;
        List<ClientToken<String>> tokens = Arrays.asList(tokenA);
        List<Result> results = Arrays.asList(getResult(null, Constants.ERROR_NOT_REGISTERED, null));
        MulticastResult expected = getMulticastResultBuilder(50, 50, 20, 1, results);

        when(clientTokenService.findPushTokensForOperatingSystem(PushToken.OperatingSystem.ANDROID)).thenReturn(tokens);
        when(wrapper.send(Mockito.any(Message.class), anyList(), anyInt())).thenReturn(expected);

        pusher.sendPush(new PushPayload("message"));

        ArgumentCaptor<ClientToken> oldTokenCaptor = ArgumentCaptor.forClass(ClientToken.class);
        verify(clientTokenService).unregisterClientToken(oldTokenCaptor.capture());
        assertThat((String) oldTokenCaptor.getValue().getToken(), is("token1"));

        verify(clientTokenService, never()).registerClientToken(Mockito.any(ClientToken.class));*/
    }
}
