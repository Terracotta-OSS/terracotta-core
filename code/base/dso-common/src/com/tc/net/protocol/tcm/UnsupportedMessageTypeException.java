/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.net.protocol.tcm;

/**
 * @author orion
 */
public class UnsupportedMessageTypeException extends RuntimeException {

  public UnsupportedMessageTypeException(String msg) {
    super(msg);
  }

  public UnsupportedMessageTypeException(TCMessageType type) {
    this("Unsupported Message type: " + type != null ? type.toString() : "null");
  }

}