/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit.nonstop;

public interface NonStopDelegateProvider<T> {
  public long getTimeout(String name);

  public T getTimeoutBehavior();

  public T getDelegate();
}
