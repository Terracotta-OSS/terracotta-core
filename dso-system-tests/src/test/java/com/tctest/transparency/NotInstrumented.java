/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest.transparency;

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