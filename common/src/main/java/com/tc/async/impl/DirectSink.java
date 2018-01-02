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
 *
 */
package com.tc.async.impl;

import com.tc.async.api.EventHandler;
import com.tc.async.api.EventHandlerException;
import com.tc.async.api.Sink;
import com.tc.stats.Stats;
import com.tc.util.Assert;
import java.util.function.Consumer;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This StageQueueImpl represents the sink and gives a handle to the source. We are internally just using a queue
 * since our queues are locally processed. This class can be replaced with a distributed queue to enable processing
 * across process boundaries.
 */
public class DirectSink<EC> implements Sink<EC> {
  private final EventHandler<EC> handler;
  private final Supplier<Boolean> isIdle;
  private final StageQueue<EC> ifNotDirect;
  private volatile boolean directInflight = false;
  private static final Logger LOGGER = LoggerFactory.getLogger(DirectSink.class);
  private static final ThreadLocal<Thread> ACTIVATED = new ThreadLocal<>();

  public DirectSink(EventHandler<EC> handler, Supplier<Boolean> isIdle, StageQueue<EC> queue) {
    this.handler = handler;
    this.isIdle = isIdle;
    this.ifNotDirect = queue;
    Assert.assertNotNull(this.isIdle);
    Assert.assertNotNull(this.ifNotDirect);
  }
  
  private void pipeline(EC context, Consumer<EC> underlying) {
    // access here MUST be fed by a single stage
    if (isSingleThreaded()) {
      try {
        directInflight = true;
        Assert.assertTrue(isIdle.get());
        handler.handleEvent(context);
        Assert.assertTrue(isIdle.get());
      } catch (EventHandlerException ee) {
        throw new RuntimeException(ee);
      } finally {
        directInflight = false;
      }
    } else {
      if (directInflight) {
        throw new AssertionError();
      } else {
        underlying.accept(context);
      }
    }
  }

  @Override
  public void addSingleThreaded(EC context) {
    pipeline(context, ifNotDirect::addSingleThreaded);
  }

  @Override
  public void addMultiThreaded(EC context) {
    pipeline(context, ifNotDirect::addMultiThreaded);
  }

  @Override
  public boolean isEmpty() {
    return !directInflight || this.ifNotDirect.isEmpty();
  }

  @Override
  public int size() {
    return this.ifNotDirect.size();
  }

  @Override
  public void clear() {
    this.ifNotDirect.clear();
  }

  @Override
  public void close() {
    this.ifNotDirect.close();
  }
  
  public static void activate(boolean activate) {
    if (activate) {
      ACTIVATED.set(Thread.currentThread());
    } else {
      ACTIVATED.remove();
    }
  }
  
  public static boolean isActivated() {
    return ACTIVATED.get() == Thread.currentThread();
  }
  
  private boolean isSingleThreaded() {
    if (LOGGER.isDebugEnabled()) {
      if (isActivated()) {
        if (!this.isIdle.get()) {
          LOGGER.debug("checked but not idle:" + ifNotDirect.toString());
          return false;
        } else {
          return true;
        }
      }
      return false;
    } else {
      return isActivated() && this.isIdle.get();
    }
  }
  
}
