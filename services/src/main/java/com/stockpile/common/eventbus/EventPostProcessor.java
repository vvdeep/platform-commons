/**
 * Copyright 2018 by StockPile Inc. All rights reserved
 */
package com.stockpile.common.eventbus;

import com.codahale.metrics.Counter;
import com.sp.quantum.core.metrics.MetricsRegistry;
import com.sp.quantum.databus.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.IOException;

public class EventPostProcessor implements IEventPostProcessor {

    private static Logger logger = LoggerFactory.getLogger(EventPostProcessor.class);
    // We would auto inject the topic name for.
    @Inject
    IEventPublisher eventPublisher;
    private Counter consumedCounter;
    private Counter RetriedCounter;
    private Counter FailedCounter;
    private boolean isDeadLetterListener;

    public EventPostProcessor() {

    }

    void setTopic(String topic) {
        ((KafkaEventPublisher) eventPublisher).setTopic(topic);
        this.consumedCounter = MetricsRegistry.counter("Consumed." + topic);
        this.RetriedCounter = MetricsRegistry.counter("Retried." + topic);
        this.FailedCounter = MetricsRegistry.counter("Failed." + topic);
    }

    @Override
    public <Event extends BaseEvent> void process(Consumer consumer, Event event,
                                                  IEventHandler.AckStatus ackStatus,
                                                  IEventHandler<Event> eventIEventHandler) {
        logger.debug(" Event processing status {} for event id {} ", ackStatus, event.getId());
        IEventHandler.AckStatus status = IEventHandler.AckStatus.TryAgain;
        try {
            if (ackStatus == IEventHandler.AckStatus.Consumed) {
                consumedCounter.inc();
                return;
            }
            int maxRetries = 10;
            int retryCount = 0;
            for (retryCount = 0; retryCount < maxRetries; retryCount++) {
                try {
                    status = eventIEventHandler.receive(event);
                } catch (Throwable throwable) {
                    status = IEventHandler.AckStatus.TryAgain;
                    // Lets not ignore this exception and try again.
                    logger.error(throwable.getLocalizedMessage(), throwable);
                }

                if (status != IEventHandler.AckStatus.TryAgain) {
                    break;
                }
            }
            RetriedCounter.inc(retryCount);
            if (status == IEventHandler.AckStatus.Consumed) {
                consumedCounter.inc();
                // consumed. so we can mark as commit.
                consumer.commit();
            } else {
                FailedCounter.inc();
                // Post this to dead letter queue now we can mark it as complete.
                postAsDeadLetter(event);
                consumer.commit();
            }
        } catch (Throwable throwable) {
            logger.error(throwable.getLocalizedMessage(), throwable);
            if (status != IEventHandler.AckStatus.Consumed) {
                try {
                    postAsDeadLetter(event);
                } catch (IOException e) {
                    logger.error(throwable.getLocalizedMessage(), throwable);
                }
            }
        } finally {

        }
    }

    /**
     * @param event
     * @param <Event>
     * @throws IOException
     */
    private <Event extends BaseEvent> void postAsDeadLetter(Event event) throws IOException {
        if (!isDeadLetterListener) {
            eventPublisher.publish(event);
        } else {
            logger.error(" Event {} is failed to consume in dead letter messages ", event.getId());
        }

    }

    public void setDeadLetterListener(boolean deadLetterListener) {
        isDeadLetterListener = deadLetterListener;
    }


}
