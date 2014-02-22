package be.appfoundry.promtius.apple;

import com.google.common.collect.Lists;
import com.notnoop.apns.ApnsNotification;
import junit.framework.TestCase;
import org.junit.Before;
import org.junit.Test;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Date;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;
/**
 * @author Mike Seghers
 */
public class NoopApnsServiceTest {
    private  NoopApnsService service;

    @Before
    public void setUp() throws Exception {
        service = new NoopApnsService();
    }

    @Test
    public void test_pushUsingMessage() throws Exception {
        service.push(null);
    }

    @Test
    public void test_pushWithString() throws Exception {
        assertThat(service.push("token", "payload"), is(nullValue()));
    }

    @Test
    public void test_pushWithBytes() throws Exception {
        assertThat(service.push("token".getBytes(), "payload".getBytes()), is(nullValue()));
    }

    @Test
    public void test_pushWithBytesAndExpiry() throws Exception {
        assertThat(service.push("token".getBytes(), "payload".getBytes(), 10), is(nullValue()));
    }

    @Test
    public void test_pushWithStringAndExpiry() throws Exception {
        assertThat(service.push("token", "payload", new Date()), is(nullValue()));
    }

    @Test
    public void test_pushWithCollectionOfStrings() throws Exception {
        assertThat(service.push(Lists.newArrayList("token"), "payload"), is(empty()));
    }

    @Test
    public void test_pushWithCollectionOfStringsWithExpiry() throws Exception {
        assertThat(service.push(Lists.newArrayList("token"), "payload", new Date()), is(empty()));
    }

    @Test
    public void test_pushWithCollectionOfBytes() throws Exception {
        assertThat(service.push(Lists.newArrayList("token".getBytes()), "payload".getBytes()), is(empty()));
    }

    @Test
    public void test_pushWithCollectionOfBytesWithExpiry() throws Exception {
        assertThat(service.push(Lists.newArrayList("token".getBytes()), "payload".getBytes(), 10), is(empty()));
    }

    @Test
    public void test_start() throws Exception {
        service.start();
    }

    @Test
    public void test_stop() throws Exception {
        service.stop();
    }

    @Test
    public void test_getInactiveDevices() throws Exception {
        assertThat(service.getInactiveDevices().size(), is(0));
    }

    @Test
    public void test_testConnection() throws Exception {
        service.testConnection();
    }
}
