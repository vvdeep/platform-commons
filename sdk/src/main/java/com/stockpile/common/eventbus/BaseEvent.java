/**
 * Copyright 2018 by StockPile Inc. All rights reserved
 */
package com.stockpile.common.eventbus;

import java.time.Instant;
import java.util.UUID;

public abstract class BaseEvent {

    private long createdTime;
    private long publishedTime;
    private String id;

    public BaseEvent() {
        createdTime = Instant.now().getNano();
        id = UUID.randomUUID().toString();
    }

    /**
     * Is used for watermarking etc. Any typical use of event time
     *
     * @return
     */
    public final long getCreatedTime() {
        return this.createdTime;
    }

    /**
     * Time when an event is published. There should be very less difference between create and publish
     *
     * @return
     */
    public final long getPublishedTime() {
        return this.publishedTime;
    }


    /**
     * @param timestamp
     * @return
     */
    public final void setPublishedTime(long timestamp) {
        this.publishedTime = timestamp;
    }

    /**
     * Is used for distributing the messages. Example of such is a STOCK symbol. When multiple consumers are running,
     * events are serialized and send to one consumer based on this.
     *
     * @return
     */
    public abstract String getGroup();

    /**
     * A random UUID is generated, this can be used for all tracing and debugging between publisher and consumer.
     *
     * @return
     */
    public final String getId() {
        return this.id;
    }

}
