/*
 *
 *  The contents of this file are subject to the Terracotta Public License Version
 *  2.0 (the "License"); You may not use this file except in compliance with the
 *  License. You may obtain a copy of the License at
 *
 *  http://terracotta.org/legal/terracotta-public-license.
 *
 *  Software distributed under the License is distributed on an "AS IS" basis,
 *  WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 *  the specific language governing rights and limitations under the License.
 *
 *  The Covered Software is Terracotta Core.
 *
 *  The Initial Developer of the Covered Software is
 *  Terracotta, Inc., a Software AG company
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

    final AtomicReference<TCMessage> msg = new AtomicReference<TCMessage>(null);
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
    final AtomicReference<TCMessage> defmsg = new AtomicReference<TCMessage>(null);
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
