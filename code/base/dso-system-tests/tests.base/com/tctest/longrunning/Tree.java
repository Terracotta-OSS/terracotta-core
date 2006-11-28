/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tctest.longrunning;


public class Tree {
  private Tree[] children;

  public void makeTree(int breadth, int depth) {
    children = new Tree[breadth];
    if (depth < 1) return;
    for (int b = 0; b < breadth; b++) {
      children[b] = new Tree();
      children[b].makeTree(breadth, depth -1);
    }
  }
}