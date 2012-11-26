/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit.nonstop;

import org.terracotta.toolkit.nonstop.NonStopConfiguration;

public interface NonStopDelegateProvider<T> {
  public NonStopConfiguration getNonStopConfiguration(String name);

  public T getTimeoutBehavior();

  public T getDelegate();
}
