/**
 * Copyright 2018 by StockPile Inc. All rights reserved
 */
package com.stockpile.common.eventbus;

import com.sp.quantum.databus.DataBus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.List;


public class KafkaEventPublisher implements IEventPublisher {

    private static final Logger logger = LoggerFactory.getLogger(KafkaEventPublisher.class);
    private String topic;

    public KafkaEventPublisher() {
    }

    void setTopic(String topic) {
        this.topic = topic;
    }

    @Override
    public <Event extends BaseEvent> void publish(Event event) {
        assert (topic != null);
        String key = event.getGroup();
        try {
            event.setPublishedTime(Instant.now().getNano());
            DataBus.publish(topic, key, event);
        } catch (Throwable throwable) {
            logger.error(throwable.getLocalizedMessage() + " While sending message ", event.getId(), throwable);
        }
    }

    @Override
    public <Event extends BaseEvent> void publish(List<Event> batchOfEvents) {
        for (Event event : batchOfEvents) {
            this.publish(event);
        }
    }
}
