package de.holisticon.util.tracee.jaxrs.client;

import de.holisticon.util.tracee.TraceeBackend;
import de.holisticon.util.tracee.TraceeConstants;
import de.holisticon.util.tracee.TraceeLoggerFactory;
import de.holisticon.util.tracee.configuration.TraceeFilterConfiguration;
import de.holisticon.util.tracee.jaxrs.MockTraceeBackend;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import java.io.IOException;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;


/**
 * @author Daniel Wegener (Holisticon AG)
 */
public class TraceeClientRequestFilterTest {

	private final TraceeFilterConfiguration configuration = Mockito.mock(TraceeFilterConfiguration.class);
	private final TraceeLoggerFactory loggerFactory = Mockito.mock(TraceeLoggerFactory.class);
    private final TraceeBackend traceeBackend = new MockTraceeBackend(configuration, loggerFactory);
    private final TraceeClientRequestFilter unit = new TraceeClientRequestFilter(traceeBackend);
    private ClientRequestContext clientRequestContext = Mockito.mock(ClientRequestContext.class);



    @Before
    public void setUp() {
		when(configuration.shouldProcessContext(any(TraceeFilterConfiguration.MessageType.class))).thenReturn(true);
		when(configuration.generatedRequestIdLength()).thenReturn(32);

        final MultivaluedMap<String, Object> headers = new MultivaluedHashMap<String, Object>();
        when(clientRequestContext.getHeaders()).thenReturn(headers);

    }

    @Test
    public void testFilter() throws IOException {
        traceeBackend.put("foo", "bar");
        unit.filter(clientRequestContext);
        assertThat((String)clientRequestContext.getHeaders().getFirst(TraceeConstants.HTTP_HEADER_NAME),
                containsString("\"foo\":\"bar\""));
    }

    @Test
    public void testFilterCreatesRequestId() throws IOException {
        unit.filter(clientRequestContext);
        assertThat(traceeBackend.get(TraceeConstants.REQUEST_ID_KEY), not(isEmptyOrNullString()));
        assertThat((String)clientRequestContext.getHeaders().getFirst(TraceeConstants.HTTP_HEADER_NAME),
                containsString("\""+TraceeConstants.REQUEST_ID_KEY+"\":\""));
    }

    @Test
    public void testFilterKeepsExistingRequestId() throws IOException {
        traceeBackend.put(TraceeConstants.REQUEST_ID_KEY, "foo");
        unit.filter(clientRequestContext);
        assertThat(traceeBackend.get(TraceeConstants.REQUEST_ID_KEY), equalTo("foo"));
        assertThat((String)clientRequestContext.getHeaders().getFirst(TraceeConstants.HTTP_HEADER_NAME),
                containsString("\""+TraceeConstants.REQUEST_ID_KEY+"\":\"foo\""));
    }

}