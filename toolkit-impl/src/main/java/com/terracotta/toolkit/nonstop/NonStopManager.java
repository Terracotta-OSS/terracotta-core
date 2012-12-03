/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit.nonstop;

public interface NonStopManager {

  /**
   * begin a non stop operation with a timeout. Throws IllegalStateException if a nonstop operation is already started
   * for this thread
   */
  void begin(long timeout);

  /**
   * try to begin a non stop operation with a timeout. retruns false if a nonstop operation was already started
   */
  boolean tryBegin(long timeout);

  /**
   * Indicate that the non stop operation completed
   */
  void finish();

}