package be.appfoundry.promtius.apple;


import com.notnoop.apns.ApnsService;
import com.notnoop.exceptions.InvalidSSLConfig;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * @author Mike Seghers
 */
public class ApnsServiceFactoryTest {

    @Test
    public void test_getServiceWhenNoCertIsGiven() throws Exception {
        ApnsService sanboxApnsService = ApnsServiceFactory.getSanboxApnsService(null, null);
        assertThat(sanboxApnsService, is(instanceOf(NoopApnsService.class)));
    }

    @Test
    public void test_getServiceWhenInvalidCertIsGiven() throws Exception {
        ApnsService sanboxApnsService = ApnsServiceFactory.getSanboxApnsService("path", "pass");
        assertThat(sanboxApnsService, is(instanceOf(NoopApnsService.class)));
    }

    @Test(expected = InvalidSSLConfig.class)
    public void test_getSandbocService_wrongP12Password() throws Exception {
        ApnsServiceFactory.getSanboxApnsService("/selfsigned.p12", "pass");

    }

    @Test
    public void test_getSanboxService() throws Exception {
        ApnsService sandboxApnsService = ApnsServiceFactory.getSanboxApnsService("/selfsigned.p12", "password");
        assertThat(sandboxApnsService, is(notNullValue()));

    }

    @Test
    public void test_getProductionService() throws Exception {
        ApnsService sandboxApnsService = ApnsServiceFactory.getProductionApnsService("/selfsigned.p12", "password");
        assertThat(sandboxApnsService, is(notNullValue()));

    }
}
