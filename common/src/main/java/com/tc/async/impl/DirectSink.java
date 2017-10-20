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
  private static final Logger LOGGER = LoggerFactory.getLogger(DirectSink.class);
  private static final ThreadLocal<Thread> ACTIVATED = new ThreadLocal<>();

  public DirectSink(EventHandler<EC> handler, Supplier<Boolean> isIdle, StageQueue<EC> queue) {
    this.handler = handler;
    this.isIdle = isIdle;
    this.ifNotDirect = queue;
    Assert.assertNotNull(this.isIdle);
    Assert.assertNotNull(this.ifNotDirect);
  }

  @Override
  public void addSingleThreaded(EC context) {
    // access here MUST be single threaded
    if (isSingleThreaded()) {
      try {
        Assert.assertTrue(isIdle.get());
        handler.handleEvent(context);
        Assert.assertTrue(isIdle.get());
      } catch (EventHandlerException ee) {
        throw new RuntimeException(ee);
      }
    } else {
      this.ifNotDirect.addSingleThreaded(context);
    }
  }

  @Override
  public void addMultiThreaded(EC context) {
    // access here MUST be single threaded
    if (isSingleThreaded()) {
      try {
        Assert.assertTrue(isIdle.get());
        handler.handleEvent(context);
        Assert.assertTrue(isIdle.get());
      } catch (EventHandlerException ee) {
        throw new RuntimeException(ee);
      }
    } else {
      this.ifNotDirect.addMultiThreaded(context);
    }
  }

  @Override
  public boolean isEmpty() {
    return this.ifNotDirect.isEmpty();
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

  @Override
  public void enableStatsCollection(boolean enable) {
    this.ifNotDirect.enableStatsCollection(enable);
  }

  @Override
  public boolean isStatsCollectionEnabled() {
    return this.ifNotDirect.isStatsCollectionEnabled();
  }

  @Override
  public Stats getStats(long frequency) {
    return this.ifNotDirect.getStats(frequency);
  }

  @Override
  public Stats getStatsAndReset(long frequency) {
    return this.ifNotDirect.getStatsAndReset(frequency);
  }

  @Override
  public void resetStats() {
    this.ifNotDirect.resetStats();
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
    return ACTIVATED.get() == Thread.currentThread()
      && this.isIdle.get();
  }
  
}
