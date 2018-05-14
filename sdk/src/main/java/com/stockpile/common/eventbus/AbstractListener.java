package com.stockpile.common.eventbus;

import com.sp.quantum.databus.Consumer;

import javax.inject.Inject;


public abstract class AbstractListener<Event extends BaseEvent> implements IEventHandler<Event> {


    @Inject
    private IEventPostProcessor eventPostProcessor;

    /**
     * @param consumer
     * @param kafkaOffset
     * @param s
     * @param event
     */

    public final void onMessage(Consumer consumer, long kafkaOffset, String s, Event event) {
        AckStatus ackStatus = this.receive(event);
        eventPostProcessor.process(consumer, event, ackStatus, this);

    }

    /**
     * @return
     */
    public final IEventPostProcessor getEventPostProcessor() {
        return this.eventPostProcessor;
    }
}
