/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.net.protocol.tcm;

import com.tc.net.protocol.tcm.msgs.PingMessage;
import com.tc.util.concurrent.SetOnceFlag;

import java.security.SecureRandom;
import java.util.Random;
import java.util.concurrent.atomic.AtomicReference;

import junit.framework.TestCase;

public class TCMessageRouterTest extends TestCase {

  public void testDefaultRoute() {

    try {
      TCMessageRouter router = new TCMessageRouterImpl();
      router.putMessage(createMessage());
      fail();
    } catch (UnsupportedMessageTypeException umte) {
      // expected
    }

    final AtomicReference<TCMessage> msg = new AtomicReference(null);
    TCMessageRouter router = new TCMessageRouterImpl(new TCMessageSink() {
      @Override
      public void putMessage(TCMessage message) {
        msg.set(message);
      }
    });
    TCMessage message = createMessage();
    router.putMessage(message);
    assertSame(message, msg.get());

    msg.set(null);
    router.routeMessageType(TCMessageType.PING_MESSAGE, new TCMessageSink() {
      @Override
      public void putMessage(TCMessage m) {
        // ignore it
      }
    });
    router.putMessage(createMessage());
    assertNull(msg.get());
  }

  public void testRouteByType() {
    final AtomicReference<TCMessage> defmsg = new AtomicReference(null);
    TCMessageRouter router = new TCMessageRouterImpl(new TCMessageSink() {
      @Override
      public void putMessage(TCMessage m) {
        defmsg.set(m);
      }
    });

    final AtomicReference<TCMessage> msg = new AtomicReference<TCMessage>(null);
    router.routeMessageType(TCMessageType.PING_MESSAGE, new TCMessageSink() {
      @Override
      public void putMessage(TCMessage m) {
        msg.set(m);
      }
    });
    TCMessage message = createMessage();
    router.putMessage(message);
    assertSame(message, msg.get());
    assertNull(defmsg.get());

    msg.set(null);
    defmsg.set(null);
    router.unrouteMessageType(TCMessageType.PING_MESSAGE);
    router.putMessage(message);
    assertNull(msg.get());
    assertSame(message, defmsg.get());
  }

  public void testConcurrency() throws Exception {
    final Random random = new SecureRandom();
    final AtomicReference<Throwable> error = new AtomicReference<Throwable>(null);
    final SetOnceFlag stop = new SetOnceFlag();
    final TCMessageSink nullSink = new TCMessageSink() {
      @Override
      public void putMessage(TCMessage message) {
        // nada
      }
    };
    final TCMessageRouter router = new TCMessageRouterImpl(nullSink);

    final Runnable putter = new Runnable() {
      @Override
      public void run() {
        TCMessage msg = createMessage();
        try {
          while (true) {
            for (int i = 0; i < 100; i++) {
              router.putMessage(msg);
            }
            if (stop.isSet()) { return; }
          }
        } catch (Throwable t) {
          setError(t, error);
        }
      }
    };

    final Runnable changer = new Runnable() {
      @Override
      public void run() {
        try {
          while (true) {
            for (int i = 0; i < 100; i++) {
                if (random.nextBoolean()) {
                  router.routeMessageType(TCMessageType.PING_MESSAGE, nullSink);
                } else {
                  router.unrouteMessageType(TCMessageType.PING_MESSAGE);
                }
            }
            if (stop.isSet()) { return; }
          }
        } catch (Throwable t) {
          setError(t, error);
        }
      }
    };

    Thread[] threads = new Thread[10];
    for (int i = 0; i < 5; i++) {
      threads[i] = new Thread(putter);
      threads[5+i] = new Thread(changer);
    }
    
    for (Thread thread : threads) {
      thread.setDaemon(true);
      thread.start();
    }
    
    Thread.sleep(5000);
    stop.set();
    
    for (Thread thread : threads) {
      thread.join(5000);
    }
    
    assertNull(error.get());
  }

  private static void setError(Throwable t, AtomicReference<Throwable> error) {
    t.printStackTrace();
    error.set(t);
  }

  private PingMessage createMessage() {
    PingMessage rv = new PingMessage(new NullMessageMonitor());
    rv.dehydrate();
    return rv;
  }

}
