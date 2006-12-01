/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.objectserver.lockmanager.api;

/**
 * Thrown by server side locks when an illegal attempt to wait/notify is performed
 */
public class TCIllegalMonitorStateException extends Exception {

  public TCIllegalMonitorStateException() {
    super();
  }

  public TCIllegalMonitorStateException(String message) {
    super(message);
  }

  public TCIllegalMonitorStateException(Throwable cause) {
    super(cause);
  }

  public TCIllegalMonitorStateException(String message, Throwable cause) {
    super(message, cause);
  }

}