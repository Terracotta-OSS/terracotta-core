/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object;

public enum ServerMapRequestType {
  GET_VALUE_FOR_KEY, GET_SIZE;

  public static ServerMapRequestType fromOrdinal(final int ordinal) {
    if (ordinal == GET_VALUE_FOR_KEY.ordinal()) {
      return GET_VALUE_FOR_KEY;
    } else if (ordinal == GET_SIZE.ordinal()) {
      return GET_SIZE;
    } else {
      throw new AssertionError("Unknown ordinal : " + ordinal);
    }
  }
}