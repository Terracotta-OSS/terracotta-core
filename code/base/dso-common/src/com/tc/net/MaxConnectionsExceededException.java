/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tc.net;

public class MaxConnectionsExceededException extends Exception {

  public MaxConnectionsExceededException(String msg) {
    super(msg);
  }
}
