/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.bytecode;

public class SessionConfiguration {

  private final int     lockType;
  private final boolean sessionLocking;
  private final boolean serialization;

  public SessionConfiguration(int lockType, boolean sessionLocking, boolean serialization) {
    this.lockType = lockType;
    this.sessionLocking = sessionLocking;
    this.serialization = serialization;
  }

  public int getLockType() {
    return lockType;
  }

  public boolean isSerialization() {
    return serialization;
  }

  public boolean isSessionLocking() {
    return sessionLocking;
  }

  @Override
  public String toString() {
    return "[lockType=" + lockType + ", sessionLocking=" + sessionLocking + ", serialization=" + serialization + ']';
  }

}
