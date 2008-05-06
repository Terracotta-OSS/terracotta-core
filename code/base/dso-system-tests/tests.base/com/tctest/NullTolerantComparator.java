/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest;

import java.util.Comparator;

public class NullTolerantComparator implements Comparator {

  public int compare(Object o1, Object o2) {
    if (o1 == null && o2 == null) { return 0; }
    if (o1 == null && o2 != null) { return -1; }
    if (o1 != null && o2 == null) { return 1; }
    return ((Comparable) o1).compareTo(o2);
  }
}