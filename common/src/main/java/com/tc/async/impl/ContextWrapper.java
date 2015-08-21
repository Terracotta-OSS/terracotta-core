/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.async.impl;

import com.tc.async.api.EventHandler;
import com.tc.async.api.EventHandlerException;

/**
   * This interface is used to wrap the contexts put into the queues since we use them different ways but still want them
   * handled in-order.  This replaces an instanceof hack, previously in use.
   */
public interface ContextWrapper<EC> {
  public void runWithHandler(EventHandler<EC> handler) throws EventHandlerException;
}
