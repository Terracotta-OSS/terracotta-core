/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
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
