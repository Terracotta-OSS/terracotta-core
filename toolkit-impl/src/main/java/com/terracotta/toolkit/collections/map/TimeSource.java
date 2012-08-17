/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit.collections.map;

/**
 * A source of time.
 */
public interface TimeSource {
  /**
   * Determine the current time, (like System.currentTimeMillis() but in seconds).
   * 
   * @return The current time in seconds since the epoch
   */
  int nowInSeconds();
}
