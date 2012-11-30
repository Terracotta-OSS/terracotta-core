/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit.collections.map;

/**
 * A {@link TimeSource} that uses System.currentTimeMillis().
 */
public class SystemTimeSource implements TimeSource {

  @Override
  public int nowInSeconds() {
    return (int) (System.currentTimeMillis() / 1000);
  }
}