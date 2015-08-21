/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.net;

public class MaxConnectionsExceededException extends Exception {

  public MaxConnectionsExceededException(String msg) {
    super(msg);
  }
}
