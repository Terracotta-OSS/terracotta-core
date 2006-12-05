/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest.performance.http.load;

import java.util.ArrayList;
import java.util.List;

/**
 * A space efficient, growable list of ints -- not very fancy ;-)
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
    if (currentIndex == current.length) {
      next();
    }

    current[currentIndex++] = i;
    size++;
  }

  public int size() {
    return size;
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

  public static void main(String args[]) {
    int num = 5000000;
    IntList il = new IntList();

    for (int i = 0; i < num; i++) {
      il.add(i);

    }

    if (il.size() != num) { throw new AssertionError("wrong size reported: " + il.size()); }

    for (int i = 0; i < num; i++) {
      int val = il.get(i);
      if (val != i) { throw new AssertionError("Expected " + i + " got " + val); }
    }
  }

}
