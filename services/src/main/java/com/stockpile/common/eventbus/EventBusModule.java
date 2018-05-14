/**
 * Copyright 2018 by StockPile Inc. All rights reserved
 */
package com.stockpile.common.eventbus;

import com.google.common.base.Preconditions;
import com.google.inject.Binder;
import com.google.inject.Module;
import com.google.inject.TypeLiteral;
import com.google.inject.matcher.Matchers;
import com.google.inject.spi.InjectionListener;
import com.google.inject.spi.TypeEncounter;
import com.google.inject.spi.TypeListener;
import com.sp.quantum.databus.DataBus;
import com.sp.quantum.databus.Listener;
import com.sp.quantum.databus.MessageCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;

public class EventBusModule implements Module {

    private static Logger logger = LoggerFactory.getLogger(EventBusModule.class);

    private static void setTopic(Object producerHolderInstance) throws IllegalAccessException {
        Field publisherField = null;
        Class<?> holderInstanceClass = producerHolderInstance.getClass();
        Publisher publisher = null;
        for (Field filed : holderInstanceClass.getDeclaredFields()) {
            if (IEventPublisher.class.isAssignableFrom(filed.getType()) && filed.isAnnotationPresent(Publisher.class)) {
                publisherField = filed;
                publisher = filed.getAnnotation(Publisher.class);
                break;
            }

        }
        if (publisherField == null || publisher == null) {
            return;
        }
        if (!publisherField.isAccessible()) {
            publisherField.setAccessible(true);
        }
        IEventPublisher eventPublisher = (IEventPublisher) publisherField.get(producerHolderInstance);
        KafkaEventPublisher kafkaEventPublisher = (KafkaEventPublisher) eventPublisher;
        kafkaEventPublisher.setTopic(publisher.topic());
        logger.info(" Topic {} assigned for instance {} ", publisher.topic(), holderInstanceClass.getCanonicalName());

    }

    @Override
    public void configure(Binder binder) {
        binder.bind(IEventPostProcessor.class).to(EventPostProcessor.class);
        binder.bind(IEventPublisher.class).to(KafkaEventPublisher.class); // not a singleton.
        binder.bindListener(Matchers.any(), new EventHandlerListener());
    }

    /**
     *
     */
    static class ProducerBindListener implements InjectionListener {

        @Override
        public void afterInjection(Object injectee) {
            try {
                setTopic(injectee);
            } catch (IllegalAccessException e) {
                logger.error(e.getLocalizedMessage(), e);
                Preconditions.checkArgument(false, e.getLocalizedMessage(), e.getCause());
            }
        }
    }


    static class ConsumerBindListener<Handler extends AbstractListener> implements InjectionListener<Handler> {

        public void afterInjection(Handler handler) {
            try {
                Listener listener = handler.getClass().getAnnotation(Listener.class);
                IEventPostProcessor postProcessor = handler.getEventPostProcessor();
                EventPostProcessor eventPostProcessor = (EventPostProcessor) postProcessor;
                eventPostProcessor.setDeadLetterListener(EventBusUtils.isDeadLetterQueue(listener.topics()[0]));
                eventPostProcessor.setTopic(EventBusUtils.getDeadLetterTopic(listener.topics()[0]));
                DataBus.subscribe((MessageCallback) handler);

            } catch (Exception e) {
                logger.error(e.getLocalizedMessage(), e);
                Preconditions.checkArgument(false, e.getLocalizedMessage(), e.getCause());
            }
        }
    }

    static class EventHandlerListener implements TypeListener {
        @Override
        public <I> void hear(TypeLiteral<I> type, TypeEncounter<I> encounter) {
            if (AbstractListener.class.isAssignableFrom(type.getRawType())) {
                encounter.register(new ConsumerBindListener());
            } else {
                for (Field filed : type.getRawType().getDeclaredFields()) {
                    if (filed.isAnnotationPresent(Publisher.class)) {
                        encounter.register(new ProducerBindListener());
                        break;
                    }
                }
            }

        }
    }
}
