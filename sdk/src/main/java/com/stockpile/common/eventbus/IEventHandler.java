/**
 * Copyright 2018 by StockPile Inc. All rights reserved
 */
package com.stockpile.common.eventbus;

/**
 * @param <Event>
 */
public interface IEventHandler<Event extends BaseEvent> {

    public AckStatus receive(Event event);

    public void close();


    public enum AckStatus {
        Consumed,
        TryAgain,
        Failed
    }

}
