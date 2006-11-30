/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
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