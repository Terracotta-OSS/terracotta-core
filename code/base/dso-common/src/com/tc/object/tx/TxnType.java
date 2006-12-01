/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.tx;

import com.tc.util.Assert;

/**
 * Type safe enumeration of transaction types
 */
public class TxnType {
  private static final byte   TYPE_RO         = 1;
  private static final byte   TYPE_NORMAL     = 2;
  private static final byte   TYPE_CONCURRENT = 3;

  public static final TxnType READ_ONLY       = new TxnType(TYPE_RO);
  public static final TxnType NORMAL          = new TxnType(TYPE_NORMAL);
  public static final TxnType CONCURRENT      = new TxnType(TYPE_CONCURRENT);

  public static TxnType typeFor(byte type) {
    switch (type) {
      case TYPE_RO: {
        return READ_ONLY;
      }
      case TYPE_NORMAL: {
        return NORMAL;
      }
      case TYPE_CONCURRENT: {
        return CONCURRENT;
      }
      default: {
        throw Assert.failure("unknown transaction type " + type);
      }
    }
  }

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

  public byte getType() {
    return type;
  }

  public String toString() {
    switch (type) {
      case TYPE_RO: {
        return "READ_ONLY";
      }
      case TYPE_NORMAL: {
        return "NORMAL";
      }
      case TYPE_CONCURRENT: {
        return "CONCURRENT";
      }
      default: {
        return "UNKNOWN (" + type + ")";
      }
    }
  }

}
