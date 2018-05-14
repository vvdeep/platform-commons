package com.stockpile.common.eventbus;

import com.sp.quantum.databus.Consumer;

/**
 * BaseEvent post processing.
 */
public interface IEventPostProcessor {

    /**
     * @param event
     * @param eventIEventHandler
     * @param <Event>
     * @return
     */
    public <Event extends BaseEvent> void process(Consumer consumer,
                                                  Event event,
                                                  IEventHandler.AckStatus ackStatus,
                                                  IEventHandler<Event> eventIEventHandler);
}
