/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.async.api;

public interface OrderedEventContext extends EventContext {

  public long getSequenceID();
  
}
