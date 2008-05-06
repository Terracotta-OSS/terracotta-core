/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
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