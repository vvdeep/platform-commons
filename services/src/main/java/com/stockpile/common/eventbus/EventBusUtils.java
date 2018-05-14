/**
 * Copyright 2018 by StockPile Inc. All rights reserved
 */
package com.stockpile.common.eventbus;


public class EventBusUtils {


    public static String getDeadLetterTopic(String topicName) {
        return topicName + "-deadmsgs";
    }

    public static boolean isDeadLetterQueue(String topicName) {
        return topicName.endsWith("-deadmsgs");
    }
}
