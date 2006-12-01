/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest.transparency;

/**
 * TODO Nov 3, 2004: I, steve, am too lazy to write a single sentence describing what this class is for.
 */
public class NotInstrumented {
  private Object o = new Object();

  public NotInstrumented() {
    super();
    o.getClass();
  }

  public boolean equals(Object o2) {
    if (this == o2) return true;
    if (o2 instanceof NotInstrumented) { return true; }
    return false;
  }

}