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

import com.tc.async.api.DirectExecutionMode;
import com.tc.async.api.EventHandlerException;
import com.tc.util.Assert;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This StageQueueImpl represents the sink and gives a handle to the source. We are internally just using a queue
 * since our queues are locally processed. This class can be replaced with a distributed queue to enable processing
 * across process boundaries.
 */
public class DirectEventCreator<EC> implements EventCreator<EC> {
  private final EventCreator<EC> base;
  private final Supplier<Boolean> isIdle;
  private volatile boolean directInflight = false;
  private static final Logger LOGGER = LoggerFactory.getLogger(DirectEventCreator.class);

  public DirectEventCreator(EventCreator<EC> base, Supplier<Boolean> isIdle) {
    this.base = base;
    this.isIdle = isIdle;
    Assert.assertNotNull(this.isIdle);
  }

  @Override
  public Event createEvent(EC event) {
    if (isSingleThreaded()) {
      try {
        directInflight = true;
        Assert.assertTrue(isIdle.get());
        base.createEvent(event).call();
        Assert.assertTrue(isIdle.get());
      } catch (EventHandlerException ee) {
        throw new RuntimeException(ee);
      } finally {
        directInflight = false;
      }
      return null;
    } else {
      if (directInflight) {
        throw new AssertionError();
      }
      return base.createEvent(event);
    }
  }
  
  private boolean isSingleThreaded() {
    if (LOGGER.isDebugEnabled()) {
      if (DirectExecutionMode.isActivated()) {
        if (!this.isIdle.get()) {
          return false;
        } else {
          return true;
        }
      }
      return false;
    } else {
      return DirectExecutionMode.isActivated() && this.isIdle.get();
    }
  }
  
}
