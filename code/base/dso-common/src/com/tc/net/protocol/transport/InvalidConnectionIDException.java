/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tc.net.protocol.transport;

public class InvalidConnectionIDException extends Exception {

  public InvalidConnectionIDException(String connectionID, String reason) {
    super(message(connectionID, reason));
  }

  public InvalidConnectionIDException(String connectionID, String reason, Exception e) {
    super(message(connectionID, reason), e);
  }

  private static String message(String id, String reason) {
    return "ID: " + id + ", reason: " + reason;
  }

}
