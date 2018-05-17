/**
 * Copyright 2018 by StockPile Inc. All rights reserved
 */
package com.stockpile.common.eventbus;


import com.google.inject.Binder;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.sp.quantum.core.logging.LoggingService;
import com.sp.quantum.core.servicediscovery.TopologyService;
import com.sp.quantum.databus.DataBus;
import com.sp.quantum.databus.Listener;
import com.sp.quantum.databus.MessageCallback;
import org.apache.commons.lang.RandomStringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import javax.inject.Inject;
import java.io.IOException;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;


public class KafkaEventPublisherTest {
    static Lock rentrantLock = new ReentrantLock();
    static Condition receiveMessage = rentrantLock.newCondition();
    static AtomicBoolean isDeadLetterTest = new AtomicBoolean(false);
    private static Logger testLogger = LoggerFactory.getLogger(KafkaEventPublisherTest.class);
    @Inject
    public TradeEventProducer tradeEventProducer;
    private DataBus databus = null;

    private static TradeEvent buildRandomTradeEvent() {
        TradeEvent tradeEvent = new TradeEvent();
        tradeEvent.group = RandomStringUtils.randomAscii(20);
        return tradeEvent;
    }

    @BeforeClass
    public void setup() throws Exception {
        LoggingService.initialize();
        TopologyService.init();
        databus = new DataBus();
        databus.init(new Properties());
        databus.start();
        Injector injector = Guice.createInjector(new TestModule(), new EventBusModule());
        injector.injectMembers(this);
    }

    @AfterClass
    public void teardown() throws Exception {
        databus.onStop();
    }

    @Test
    public void testPublishSimple() throws IOException {
        isDeadLetterTest.set(false);
        tradeEventProducer.publish();
        testLogger.info(" Message published  {}");
        rentrantLock.lock();
        try {
            receiveMessage.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            rentrantLock.unlock();
        }
    }

    @Test
    public void testPublishDeadLetter() throws IOException {
        isDeadLetterTest.set(true);
        tradeEventProducer.publish();
        rentrantLock.lock();
        try {
            receiveMessage.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            rentrantLock.unlock();
        }
    }

    public static class TradeEventProducer {

        @Inject
        @Publisher(topic = "PublishSimple-Test")
        private IEventPublisher eventPublisher;

        public void publish() {
            try {
                TradeEvent tradeEvent = buildRandomTradeEvent();
                eventPublisher.publish(tradeEvent);
                testLogger.info(" Message published  {}", tradeEvent.getId());
            } catch (Throwable throwable) {
                throwable.printStackTrace();
            } finally {
            }
        }
    }

    @Listener(topics = {"PublishSimple-Test"}, group = "SomeGroup")
    public static class TradeListener extends AbstractListener<TradeEvent> implements MessageCallback<TradeEvent> {

        @Override
        public AckStatus receive(TradeEvent event) {
            testLogger.info(" TradeListener receive {}", event.getId());
            rentrantLock.lock();
            try {
                receiveMessage.signal();
            } finally {
                rentrantLock.unlock();
            }
            if (!isDeadLetterTest.get()) {
                return AckStatus.Consumed;
            }
            return AckStatus.TryAgain;

        }

        @Override
        public void close() {

        }

    }

    @Listener(topics = {"PublishSimple-Test-deadmsgs"}, group = "SomeGroup")
    public static class TradEventDeadMessageListener extends AbstractListener<TradeEvent> implements MessageCallback<TradeEvent> {

        @Override
        public AckStatus receive(TradeEvent event) {
            testLogger.info(" TradEventDeadMessageListener receive {}", event.getId());
            if (isDeadLetterTest.get()) {
                rentrantLock.lock();
                try {
                    receiveMessage.signal();
                } finally {
                    rentrantLock.unlock();
                }
                return AckStatus.Consumed;
            }
            return AckStatus.TryAgain;
        }

        @Override
        public void close() {

        }

    }

    public static class TradeEvent extends BaseEvent {
        String group;

        @Override
        public String getGroup() {
            return group;
        }


        @Override
        public String toString() {
            return "TradeEvent{" +
                    "group='" + group + '\'' +
                    ", id='" + getId() + '\'' +
                    '}';
        }
    }

    public class TestModule implements Module {
        public void configure(Binder binder) {
            binder.bind(TradeListener.class).asEagerSingleton();
            binder.bind(TradeEventProducer.class).asEagerSingleton();
            binder.bind(TradEventDeadMessageListener.class).asEagerSingleton();
        }
    }
}