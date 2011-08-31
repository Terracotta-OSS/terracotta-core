/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.util;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * A space efficient, grow'able list of ints -- not very fancy ;-)
 */
public class IntList {
  private static final int BLOCK        = 4096;
  private final List       arrays       = new ArrayList();
  private int[]            current;
  private int              currentIndex = 0;
  private int              size;

  public IntList() {
    next();
  }

  public void add(int i) {
    if (currentIndex == BLOCK) {
      next();
    }

    current[currentIndex++] = i;
    size++;
  }

  public int size() {
    return size;
  }

  public int[] toArray() {
    int[] rv = new int[size];
    int index = 0;
    int remaining = size;
    for (Iterator i = arrays.iterator(); i.hasNext();) {
      int len = Math.min(remaining, BLOCK);
      System.arraycopy(i.next(), 0, rv, index, len);
      remaining -= len;
      index += len;
    }

    return rv;
  }

  public String toString() {
    StringBuffer sb = new StringBuffer();
    sb.append('{');
    int[] vals = toArray();
    for (int i = 0, n = vals.length, last = n - 1; i < n; i++) {
      sb.append(vals[i]);
      if (i != last) {
        sb.append(", ");
      }
    }
    sb.append('}');
    return sb.toString();
  }

  public int get(int index) {
    int whichArray = index == 0 ? 0 : index / BLOCK;
    return ((int[]) arrays.get(whichArray))[index % BLOCK];
  }

  private void next() {
    current = new int[BLOCK];
    currentIndex = 0;
    arrays.add(current);
  }

}
