/*
 * All content copyright (c) 2003-2009 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object;

import com.tc.async.api.MultiThreadedEventContext;

public interface ObjectRequestServerContext extends ObjectRequestContext, MultiThreadedEventContext {

  public abstract String getRequestingThreadName();

  public abstract boolean isServerInitiated();

}