package gov.usgs.earthquake.nats;

import io.nats.streaming.*;

import java.util.concurrent.CountDownLatch;

public class NatsStreamingConnectionTest {

  /**
   * Tests the connection of NATS streaming server
   * Code lifted from https://github.com/nats-io/stan.java
   *
   * @param args
   *          Input arguments
   *
   * @throws Exception When something goes wrong
   */
  public static void main(String[] args) throws Exception{
    // Connection factory
    // Produces streaming connections
    // Note: Should not make multiple factories; this should really only be done once
    StreamingConnectionFactory factory = new StreamingConnectionFactory("test-cluster","test-client");

    // Connection
    // It looks like we shouldn't be producing a bunch of NATS connections; instead, we pass them in to new streaming connections
    // Look into this in the future
    StreamingConnection connection = factory.createConnection(); //Exception just tossed out of main

    System.out.println("NATS Streaming Connection Test\n");

    // Publish blocks until we get a response from the server
    // In the future, we might want to use some more intelligent logic (with a time out)
    // Basically, if we can't hit the server, it's down and that's not good
    System.out.println("Trying to publish message...");
    try {
      connection.publish("test-subject", "test-data".getBytes());
      System.out.println("Message published");
    } catch (Exception e) {
      System.err.println("Unable to store message in NATS Streaming. Exception:");
      throw e;
    }

    // Creating countdown latch to wait for subscriber message receipt
    // This lets this thread wait until an operation in other threads finishes
    final CountDownLatch doneSignal = new CountDownLatch(1);

    // Asynchronous subscriber receiving messages
    // Evidently listens in another thread (that is what asynchronous means)
    Subscription subscription = connection.subscribe("test-subject", new MessageHandler() {
      public void onMessage(Message m) {
        System.out.printf("Received a message: %s\n", new String(m.getData()));
        doneSignal.countDown();
      }
    }, new SubscriptionOptions.Builder().deliverAllAvailable().build());

    // Wait for countdown latch
    doneSignal.await();

    // Unsubscribe for cleanup (is this necessary?)
    subscription.unsubscribe();

    // Close connection
    connection.close();

  }
}
