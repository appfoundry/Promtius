package be.appfoundry.promtius.apple;


import be.appfoundry.promtius.ClientToken;
import be.appfoundry.promtius.ClientTokenFactory;
import be.appfoundry.promtius.ClientTokenService;
import be.appfoundry.promtius.PushPayload;
import com.notnoop.apns.ApnsService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
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
    private ClientTokenService<String, String> clientTokenService;
    @Mock
    private ClientTokenFactory<String, String> clientTokenFactory;

    private static final String TEST_PLATFORM = "iOS";
    @Mock
    private ClientToken<String, String> tokenA;
    @Mock
    private ClientToken<String, String> tokenB;

    private ApplePushNotificationServicePusher<String> pusher;

    @Before
    public void setUp() throws Exception {
        pusher = new ApplePushNotificationServicePusher<String>(apnsService, clientTokenService, clientTokenFactory, TEST_PLATFORM);
    }

    @Test
    public void test_sendPush() throws Exception {
        PushPayload payload = new PushPayload("message");
        List<ClientToken<String, String>> tokens = Arrays.asList(tokenA, tokenB);
        when(clientTokenService.findClientTokensForOperatingSystem(TEST_PLATFORM)).thenReturn(tokens);
        when(tokenA.getToken()).thenReturn("token1");
        when(tokenB.getToken()).thenReturn("token2");

        pusher.sendPush(payload);

        ArgumentCaptor<String> stringCaptor = ArgumentCaptor.forClass(String.class);
        verify(apnsService).push(eq(Arrays.asList("token1", "token2")), stringCaptor.capture());
        System.out.println(stringCaptor.getValue());

    }

    @Test
    public void test_removeInactiveDevices() throws Exception {
        Map<String, Date> inactive = new HashMap<String, Date>();
        inactive.put("token1", new Date());
        inactive.put("token2", new Date());
        when(apnsService.getInactiveDevices()).thenReturn(inactive);
        when(clientTokenFactory.createClientToken("token1", TEST_PLATFORM)).thenReturn(tokenA);
        when(clientTokenFactory.createClientToken("token2", TEST_PLATFORM)).thenReturn(tokenB);

        PushPayload payload = new PushPayload("message");
        pusher.sendPush(payload);

        ArgumentCaptor<ClientToken> pushTokenCaptor = ArgumentCaptor.forClass(ClientToken.class);
        verify(clientTokenService, times(2)).unregisterClientToken(pushTokenCaptor.capture());
        List<ClientToken> allValues = pushTokenCaptor.getAllValues();
        assertThat((ClientToken<String, String>) allValues.get(0), is(tokenA));
        assertThat((ClientToken<String, String>) allValues.get(1), is(tokenB));
    }

    @Test
    public void test_getPlatform() throws Exception {
        assertThat(pusher.getPlatform(), is(TEST_PLATFORM));
    }
}
