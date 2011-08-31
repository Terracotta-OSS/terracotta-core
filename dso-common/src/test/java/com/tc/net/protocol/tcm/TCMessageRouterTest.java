/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.net.protocol.tcm;

import EDU.oswego.cs.dl.util.concurrent.SynchronizedRef;

import com.tc.net.protocol.tcm.msgs.PingMessage;
import com.tc.util.concurrent.SetOnceFlag;

import java.security.SecureRandom;
import java.util.Random;

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

    final SynchronizedRef msg = new SynchronizedRef(null);
    TCMessageRouter router = new TCMessageRouterImpl(new TCMessageSink() {
      public void putMessage(TCMessage message) {
        msg.set(message);
      }
    });
    TCMessage message = createMessage();
    router.putMessage(message);
    assertSame(message, msg.get());

    msg.set(null);
    router.routeMessageType(TCMessageType.PING_MESSAGE, new TCMessageSink() {
      public void putMessage(TCMessage m) {
        // ignore it
      }
    });
    router.putMessage(createMessage());
    assertNull(msg.get());
  }

  public void testRouteByType() {
    final SynchronizedRef defmsg = new SynchronizedRef(null);
    TCMessageRouter router = new TCMessageRouterImpl(new TCMessageSink() {
      public void putMessage(TCMessage m) {
        defmsg.set(m);
      }
    });

    final SynchronizedRef msg = new SynchronizedRef(null);
    router.routeMessageType(TCMessageType.PING_MESSAGE, new TCMessageSink() {
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
    final SynchronizedRef error = new SynchronizedRef(null);
    final SetOnceFlag stop = new SetOnceFlag();
    final TCMessageSink nullSink = new TCMessageSink() {
      public void putMessage(TCMessage message) {
        // nada
      }
    };
    final TCMessageRouter router = new TCMessageRouterImpl(nullSink);

    final Runnable putter = new Runnable() {
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
    
    for (int i = 0; i < threads.length; i++) {
      threads[i].setDaemon(true);
      threads[i].start();
    }
    
    Thread.sleep(5000);
    stop.set();
    
    for (int i = 0; i < threads.length; i++) {      
      threads[i].join(5000);
    }
    
    assertNull(error.get());    
  }

  private static void setError(Throwable t, SynchronizedRef error) {
    t.printStackTrace();
    error.set(t);
  }

  private PingMessage createMessage() {
    PingMessage rv = new PingMessage(new NullMessageMonitor());
    rv.dehydrate();
    return rv;
  }

}