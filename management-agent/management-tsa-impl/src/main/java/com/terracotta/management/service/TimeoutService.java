/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.management.service;

/**
 * @author Ludovic Orban
 */
public interface TimeoutService {

  /**
   * Get the call timeout previously set. If none was set, the default one is returned.
   *
   * @return the call timeout.
   */
  long getCallTimeout();

  /**
   * Set the call timeout for the current thread.
   *
   * @param timeout the call timeout.
   */
  void setCallTimeout(long timeout);

  /**
   * Clear the call timeout for the current thread.
   */
  void clearCallTimeout();
}
