/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.exception;

/**
 * Wraps exception message in some other message
 */
public interface ExceptionWrapper {
  /**
   * Wrap message and return new message
   * @param message Original message
   * @return New message
   */
  public String wrap(String message);
}
