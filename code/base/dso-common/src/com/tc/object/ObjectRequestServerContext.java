/*
 * All content copyright (c) 2003-2009 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object;


public interface ObjectRequestServerContext extends ObjectRequestContext {

  public abstract String getRequestingThreadName();

  public abstract boolean isServerInitiated();

}