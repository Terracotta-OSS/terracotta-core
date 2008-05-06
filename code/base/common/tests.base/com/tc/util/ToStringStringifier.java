/**
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
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
