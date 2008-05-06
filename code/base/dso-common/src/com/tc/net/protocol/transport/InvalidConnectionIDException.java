/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
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
