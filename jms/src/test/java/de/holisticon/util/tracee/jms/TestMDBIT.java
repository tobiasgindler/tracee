package de.holisticon.util.tracee.jms;

import de.holisticon.util.tracee.Tracee;
import de.holisticon.util.tracee.TraceeConstants;
import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.annotation.Resource;
import javax.ejb.embeddable.EJBContainer;
import javax.jms.*;
import java.util.TreeMap;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

/**
 * @author Daniel Wegener (Holisticon AG)
 */
public class TestMDBIT {

    @Resource
    private ConnectionFactory connectionFactory;

    @Resource(name = "TestMDB")
    private Queue mdb;

    @Resource(name = "Response")
    private Queue responses;


    @Before
    public void initContainer() throws Exception {
        container = EJBContainer.createEJBContainer();
        container.getContext().bind("inject", this);
    }

    private EJBContainer container;

    @After
    public void clearTraceeCtx() {
        Tracee.getBackend().clear();
        container.close();
    }

    @Test
    public void testContextIsPropagatedForthAndBack() throws JMSException {

        Tracee.getBackend().put("foo", "bar");

        final Connection connection = connectionFactory.createConnection();
        connection.start();
        final Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
        final MessageConsumer consumer = session.createConsumer(responses);

        final TextMessage textMessage = session.createTextMessage("foo");
        final MessageProducer producer = TraceeMessageWriter.wrap(session.createProducer(mdb));
        producer.setDeliveryMode(DeliveryMode.PERSISTENT);
        producer.send(textMessage);

        final TextMessage response = (TextMessage) consumer.receive(1000);

        assertThat("response within 1 second", response, notNullValue());
        assertThat(response.getText(), equalTo("foo"));
        final TreeMap<String, String> traceeContext = (TreeMap<String, String>) response.getObjectProperty(TraceeConstants.JMS_HEADER_NAME);
        assertThat(traceeContext, Matchers.hasEntry("foo", "bar"));

        session.close();
        connection.close();

    }


}