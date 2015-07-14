package be.appfoundry.promtius;

import com.google.common.base.Optional;
import com.google.common.collect.Maps;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
/**
 * Created by mike on 14/07/15.
 */

public class PushPayloadTest {

    private PushPayload payload;
    private PushPayload.Builder validBuilder;

    @Before
    public void setUp() throws Exception {
        validBuilder = new PushPayload.Builder().withMessage("msg");
        payload = validBuilder.build();
    }

    @Test
    public void testCanBuildSimpleMessagePayload() throws Exception {
        assertThat(payload.getMessage(), is(equalTo("msg")));
    }

    @Test
    public void testBuildingWithoutSoundGivesDefaultSound() throws Exception {
        assertThat(payload.getSound(), is(equalTo(PushPayload.DEFAULT_SOUND_VALUE)));
    }

    @Test(expected = IllegalStateException.class)
    public void testCannotBuildWithoutMessage() throws Exception {
        new PushPayload.Builder().build();
    }

    @Test
    public void testCanBuildWithTimeToLive() throws Exception {
        payload = validBuilder.withTimeToLive(90).build();
        assertThat(payload.getTimeToLive(), is(Optional.of(90)));
    }

    @Test
    public void testCanBuildWithoutTimeToLive() throws Exception {
        assertThat(payload.getTimeToLive(), is(equalTo(Optional.<Integer>absent())));
    }

    @Test
    public void testCanBuildWithCustomFields() throws Exception {
        Map<String, Object> customFields = Maps.newHashMap();
        customFields.put("key", "value");
        payload = validBuilder.withCustomFields(customFields).build();
        assertThat(payload.getCustomFields(), is(equalTo(Optional.<Map<String, ?>>of(customFields))));
    }

    @Test
    public void testCanBuildWithoutCustomFields() throws Exception {
        assertThat(payload.getCustomFields(), is(equalTo(Optional.<Map<String, ?>>absent())));
    }

    @Test
    public void testCanBuildWithDiscriminator() throws Exception {
        payload = validBuilder.withDiscriminator("discri").build();
        assertThat(payload.getDiscriminator(), is(equalTo("discri")));
    }

    @Test
    public void testCanBuildWithoutDiscriminator() throws Exception {
        assertThat(payload.getDiscriminator(), is(equalTo(PushPayload.DEFAULT_DISCRIMINATOR_VALUE)));
    }
}