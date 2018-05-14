# Event Bus 

Event bus provides infrastructure for sending and receiving events. Hides complexity behind how data is managed, send, receive and distributing events. It has few components EventPublisher, EventConsumer, Event, and Infra. 

### Event
An event contains following.
 - group: is used for distributing the messages. Example of such is a STOCK symbol. When multiple consumers are running, events are serialized and send to one consumer based on this. 
 - type: type of the event. 
 - created Time: Is used for watermarking etc. Any typical use of event time.
 - _id: A random UUID is generated, this can be used for all tracing and debugging between publisher and consumer. created Time and _id are co
 - _publish Time: Time when an event is published. There should be very less difference between create and publish time.
_id, createdTime, type are auto-filled, group is application specific.
 
### EventPublisher:
 - Event Publisher creates an Event and publishes to the bus.
 - Event infra would distribute to all the available clients.
 - Event publishing can be single or muti evet at a time.
 
### Event Consumer:
 - Even consumer would receive a message and hands the message to the consumer.
 - The consumer should consume and respond back with status.(Consumed, TryAgain, Failed)
 - If an event needs to try again, It would be tried again and again, Max number of times retry can happen would be 64, after which Event Bus considers event as "failed" would be stored for debugging/auditing.
 
### Other details
   -  Metrics and logs 
   -  Necessary changes for dependency injection (Guice/Spring) would be made.
    

###  Example Usage :
```java
  // Example Event object.
  public class TradingEvent extends BaseEvent {
      private Operation operation ; // Buy or Sell.
      private float dollarAmount; // 
      private float stock; // fractional stock user intrested in selling and buying.
  }
```
Define a producer class with EventContext annotation with topic name.
```java
// Define a producer class.
import com.stockpile.common.eventbus.Publisher;
import com.stockpile.common.eventbus.IEventPublisher;

import javax.inject.Inject;


public class TradeEventProducer {
    @Inject
    @Publisher(topic = "TradeEvents")
    private IEventPublisher eventPublisher;
    public void publish(TradeEvent event)  {
        try {
            eventPublisher.publish(event);
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        } finally {
        }
    }
}
```
Inside your guice module, we need to include this to create and inject all required dependencies.
```java
public class SomeModule implements Module {
    public void configure(Binder binder) {
        // Other bindings
        binder.bind(TradeEventProducer.class).asEagerSingleton();
        // ... Other bindings.
    }
}
```

Use Trade Event Producer in side service for  producing a message.

```java
// Some where in your code.
@Inject
private TradeEventProducer tradeEventProducer;
public void publishTradeEvent() {
      TradingEvent event = new TradingEvent();
      event.setOperation(Operatoon.Buy);
      event.setGroup("APPL"); // Buy Apple Stock.
      event.setDollarAmount(40.0D); // Sell 40 dollar worth of Apple stock.
      event.setStock(0.05); // fractional stock.
      tradeEventProducer.publish(event);
}
```

The following is the way we use Consumer 

```java
@Listener(topics = {"TradeEvents"}, group = "SomeGroup")
public class TradeListener extends AbstractListener<TradeEvent> implements MessageCallback<TradeEvent> {

    @Override
    public AckStatus receive(TradeEvent event) {
        System.out.println(" Trade Event "+event);
        // consume event 
        return AckStatus.Consumed;
    }
    @Override
    public void close() throws IOException {
        // close any other resources.
    }
}
```

This TradeEventConsumer needs a binding in guice. Consumer would be automatically called, when a new message sent to topic.

```java
public class SomeModule implements Module {
    public void configure(Binder binder) {
        // Other bindings
        binder.bind(TradeEventConsumer.class).asEagerSingleton();
        // ... Other bindings.
    }
}
```