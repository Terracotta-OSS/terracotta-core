/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.management;

/**
 *
 */
public class RemoteManagementException extends Exception {

  public RemoteManagementException(String message, Throwable cause) {
    super(message, cause);
  }

  public RemoteManagementException(String message) {
    super(message);
  }
}
