/**
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tc.util;


/**
 * A {@link Stringifier} that uses {@link Object#toString()} to do its work.
 */
public class ToStringStringifier implements Stringifier {
  
  public static final ToStringStringifier INSTANCE = new ToStringStringifier();
  
  private ToStringStringifier() {
    // Use INSTANCE instead.
  }

  public String toString(Object o) {
    if (o == null) return "<null>";
    else return o.toString();
  }

}
