/**
 * Copyright 2018 by StockPile Inc. All rights reserved
 */
package com.stockpile.common.eventbus;

import java.io.IOException;
import java.util.List;

public interface IEventPublisher {

    /**
     * @param event
     */
    <Event extends BaseEvent> void publish(Event event) throws IOException;

    /**
     * @param batchOfEvents
     */
    <Event extends BaseEvent> void publish(List<Event> batchOfEvents) throws IOException;

}
