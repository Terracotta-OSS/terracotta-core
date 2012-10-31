/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit.nonstop;

public interface NonStopManager {

  /**
   * begin a non stop operation with a timeout
   */
  void begin(long timeout);

  /**
   * Indicate that the non stop operation completed
   */
  void finish();

  /**
   * Shutdown this manager
   */
  void shutdown();
}
