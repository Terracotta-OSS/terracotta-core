/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
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
