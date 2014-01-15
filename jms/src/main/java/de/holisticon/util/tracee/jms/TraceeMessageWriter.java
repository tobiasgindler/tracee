package de.holisticon.util.tracee.jms;

import de.holisticon.util.tracee.jms.out.TraceeMessageProducer;
import de.holisticon.util.tracee.jms.out.TraceeQueueSender;
import de.holisticon.util.tracee.jms.out.TraceeTopicPublisher;

import javax.jms.MessageProducer;
import javax.jms.QueueSender;
import javax.jms.TopicPublisher;

/**
 * @author Daniel Wegener (Holisticon AG)
 */
public final class TraceeMessageWriter {

    private TraceeMessageWriter() {
    }

    public static MessageProducer wrap(MessageProducer messageProducer) {
        return new TraceeMessageProducer(messageProducer);
    }

    public static QueueSender wrap(QueueSender queueSender) {
        return new TraceeQueueSender(new TraceeMessageProducer(queueSender), queueSender);
    }

    public static TopicPublisher wrap(TopicPublisher topicPublisher) {
        return new TraceeTopicPublisher(new TraceeMessageProducer(topicPublisher), topicPublisher);
    }

}
