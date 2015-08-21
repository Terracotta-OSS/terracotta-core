/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.async.api;

import com.tc.exception.TCException;

public class EventHandlerException extends TCException {

  public EventHandlerException() {
    this(null, null);
  }

  public EventHandlerException(String message) {
    this(message, null);
  }

  public EventHandlerException(Throwable cause) {
    this(null, cause);
  }

  public EventHandlerException(String message, Throwable cause) {
    super(message, cause);
  }

}
