/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.tx;

import com.tc.util.Assert;

/**
 * Type safe enumeration of transaction types
 */
public class TxnType {
  private static final byte   TYPE_NORMAL     = 2;
  private static final byte   TYPE_CONCURRENT = 3;
  private static final byte   TYPE_SYNC_WRITE = 4;

  public static final TxnType NORMAL          = new TxnType(TYPE_NORMAL);
  public static final TxnType SYNC_WRITE      = new TxnType(TYPE_SYNC_WRITE);
  public static final TxnType CONCURRENT      = new TxnType(TYPE_CONCURRENT);

  /**
   * Map type to enumerated value
   * 
   * @param type Type code
   * @return Type instance
   */
  public static TxnType typeFor(byte type) {
    switch (type) {
      case TYPE_NORMAL: {
        return NORMAL;
      }
      case TYPE_CONCURRENT: {
        return CONCURRENT;
      }
      case TYPE_SYNC_WRITE: {
        return SYNC_WRITE;
      }
      default: {
        throw Assert.failure("unknown transaction type " + type);
      }
    }
  }

  /**
   * @return True if concurrent
   */
  public boolean isConcurrent() {
    return this == CONCURRENT;
  }

  public boolean equals(Object other) {
    return this == other;
  }

  public int hashCode() {
    return this.type;
  }

  private final byte type;

  private TxnType(byte type) {
    this.type = type;
  }

  /**
   * @return Type code
   */
  public byte getType() {
    return type;
  }

  public String toString() {
    switch (type) {
      case TYPE_NORMAL: {
        return "NORMAL";
      }
      case TYPE_CONCURRENT: {
        return "CONCURRENT";
      }
      case TYPE_SYNC_WRITE: {
        return "SYNC_WRITE";
      }
      default: {
        return "UNKNOWN (" + type + ")";
      }
    }
  }

}
