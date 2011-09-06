/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.simulator.container;

public final class ContainerConfigException extends Exception {
  static final Reason  INVALID_CONTAINER_START_TIMEOUT       = new Reason("Invalid container start timeout.");
  static final Reason  INVALID_APPLICATION_START_TIMEOUT     = new Reason("Invalid application start timeout.");
  static final Reason  INVALID_APPLICATION_EXECUTION_TIMEOUT = new Reason("Invalid application execution timeout.");
  static final Reason  INVALID_APPLICATION_INSTANCE_COUNT    = new Reason("Invalid application instance count");

  private final Reason reason;

  ContainerConfigException(String message, Reason reason) {
    super(message);
    this.reason = reason;
  }

  Reason getReason() {
    return this.reason;
  }

  static class Reason {
    private final String name;

    private Reason(String name) {
      this.name = name;
    }

    public String toString() {
      return this.name;
    }
  }
}