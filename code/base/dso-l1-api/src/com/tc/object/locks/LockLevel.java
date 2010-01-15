/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.locks;

public enum LockLevel {
  READ, WRITE,
  SYNCHRONOUS_WRITE,
  CONCURRENT;

  public static final int READ_LEVEL              = 1;
  public static final int WRITE_LEVEL             = 2;
  public static final int SYNCHRONOUS_WRITE_LEVEL = 3;
  public static final int CONCURRENT_LEVEL        = 4;

  public boolean isWrite() {
    switch (this) {
      case WRITE:
      case SYNCHRONOUS_WRITE:
        return true;
      default:
        return false;
    }
  }
  
  public boolean isRead() {
    switch (this) {
      case READ:
        return true;
      default:
        return false;
    }
  }
  
  public boolean isSyncWrite() {
    return SYNCHRONOUS_WRITE.equals(this);
  }
  
  public int toInt() {
    switch (this) {
      case READ:
        return READ_LEVEL;
      case WRITE:
        return WRITE_LEVEL;
      case SYNCHRONOUS_WRITE:
        return SYNCHRONOUS_WRITE_LEVEL;
      case CONCURRENT:
        return CONCURRENT_LEVEL;
    }
    throw new AssertionError("Enum semantics broken in LockLevel?");
  }

  public static LockLevel fromInt(int integer) {
    switch (integer) {
      case READ_LEVEL:
        return READ;
      case WRITE_LEVEL:
        return WRITE;
      case SYNCHRONOUS_WRITE_LEVEL:
        return SYNCHRONOUS_WRITE;
      case CONCURRENT_LEVEL:
        return CONCURRENT;
      default:
        throw new IllegalArgumentException("Invalid integer lock level " + integer);
    }
  }  
}
